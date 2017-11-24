package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PostorderNodeListGenerator;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.graph.transformer.NoopDependencyGraphTransformer;

/**
 * Generates a Makefile with dependencies information
 * 
 * @execute phase="process-sources"
 * @goal nar-makefile-gen
 * @requiresProject
 * @requiresDependencyResolution test
 * @author Jeremy Nguyen-Xuan (CERN)
 */
public class NarMakeMakefileGenerationMojo extends AbstractDownloadMojo {

	/**
	 * To look up Archiver/UnArchiver implementations
	 * 
	 * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
	 * @required
	 */
	protected ArchiverManager archiverManager;

	/**
	 * The entry point to Aether, i.e. the component doing all the work.
	 * 
	 * @component
	 */
	private RepositorySystem repoSystem;

	/**
	 * The current repository/network configuration of Maven.
	 * 
	 * @parameter default-value="${repositorySystemSession}"
	 * @readonly
	 */
	private RepositorySystemSession repoSession;

	/**
	 * The project's remote repositories to use for the resolution.
	 * 
	 * @parameter default-value="${project.remoteProjectRepositories}"
	 * @readonly
	 */
	private List<RemoteRepository> remoteRepos;

	/**
	 * @parameter default-value="${project}"
	 * @readonly
	 */
	private MavenProject project;

	private NarMakeMakefileGenerator makefileGenerator = new NarMakeMakefileGenerator();

	private AOL NARaol = null;

	public void narExecute() throws MojoFailureException, MojoExecutionException {

		NARaol = getAOL();
		
		processNarDependencies(collectSortedDependencies(Artifact.SCOPE_COMPILE), Artifact.SCOPE_COMPILE);
		processNarDependencies(collectSortedDependencies(Artifact.SCOPE_TEST), Artifact.SCOPE_TEST);

		try {
			makefileGenerator.generateMakefile(getMavenProject());
		} catch (IOException e) {
			e.printStackTrace();
			throw new MojoFailureException("Could not generate Makefile");
		}

	}

	private List<NarArtifact> collectSortedDependencies(String scope) throws MojoExecutionException,
			MojoFailureException {
		List<String> sortedArtifactIds = getTopologicallySortedDependencies(scope);

		// Retrieve the NAR dependencies and re-order them according to the
		// topologically sorted list
		@SuppressWarnings("unchecked")
		List<NarArtifact> narDeps = getNarManager().getAttachedNarDependencies(libraries, scope);
		
		List<NarArtifact> sortedNarDeps = new LinkedList<NarArtifact>();
		for (String sortedArtifactId : sortedArtifactIds) {
			for (NarArtifact narDep : narDeps) {
				if (sortedArtifactId.equals(narDep.getArtifactId())) {
					sortedNarDeps.add(narDep);
				}
			}
		}
		getLog().debug("narDeps: " + narDeps.toString());
		getLog().debug("sortedNarDeps: " + sortedNarDeps.toString());

		return sortedNarDeps;
	}

	private void processNarDependencies(List<NarArtifact> sortedNarDeps, String scope) throws MojoExecutionException,
			MojoFailureException {
		// Process the dependencies
		for (Iterator<NarArtifact> i = sortedNarDeps.iterator(); i.hasNext();) {
			NarArtifact narDependency = i.next();

			String binding = narDependency.getNarInfo().getBinding(NARaol, Library.NONE);
			getLog().debug("Looking for " + narDependency + " found binding " + binding);

			// Filter platform specific dependencies
			if (narDependency.getClassifier() != null && !narDependency.getClassifier().contains(NARaol.toString())) {
				getLog().info(
						"Skipping " + narDependency.getArtifactId() + ". Current AOL: " + NARaol
								+ ", targeted Classifier: " + narDependency.getClassifier());
			} else {
				addIncludesAndLibsPaths(narDependency, binding, scope);
			}
		}
	}

	/**
	 * Collect the dependencies and sort them in a topological way. It returns
	 * only a list of artifactIds, because the versions are not correct
	 * according to Maven's resolver.
	 * 
	 * @return a list of artifactId sorted topologically
	 */
	private List<String> getTopologicallySortedDependencies(String scope) {
		// Create the root node
		Dependency dependency = new Dependency(new DefaultArtifact(project.getArtifact().getId()), Artifact.SCOPE_COMPILE);

		// Collect the whole expanded dependency tree, where nodes can be
		// duplicated and no conflicts have been resolved
		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(dependency);
		collectRequest.setRepositories(remoteRepos);
		CollectResult collectResult = null;
		try {
			DefaultRepositorySystemSession myRepoSession = new DefaultRepositorySystemSession(repoSession);
			NoopDependencyGraphTransformer myVisitor = new NoopDependencyGraphTransformer();
			myRepoSession = myRepoSession.setDependencyGraphTransformer(myVisitor);
			if (scope.equals(Artifact.SCOPE_TEST)) {
				getLog().debug("Scope test");
				Collection<String> include = new ArrayList<String>();
				Collection<String> exclude = new ArrayList<String>();
				include.add(Artifact.SCOPE_TEST);
				myRepoSession = myRepoSession.setDependencySelector(new ScopeDependencySelector(include, exclude));
			}
			collectResult = repoSystem.collectDependencies(myRepoSession, collectRequest);
		} catch (DependencyCollectionException e) {
			e.printStackTrace();
		}

		// Sorting the whole dependency tree using Postorder from Aether
		PostorderNodeListGenerator nlg = new PostorderNodeListGenerator();
		DependencyNode node = collectResult.getRoot();
		node.accept(nlg);

		// Excluding the duplicated nodes, we start from the top of the list and
		// exclude any node which has already been seen
		List<DependencyNode> nodesList = nlg.getNodes();
		List<DependencyNode> seenList = new ArrayList<DependencyNode>();
		for (Iterator<DependencyNode> it = nodesList.iterator(); it.hasNext();) {
			DependencyNode dep = it.next();
			if (contains(seenList, dep)) {
				it.remove();
			} else {
				seenList.add(dep);
			}
		}

		// Topological sort is a revert postorder sort
		Collections.reverse(nodesList);

		// Finally, only the artifactIds are extracted. The versions don't match
		// Maven's resolver.
		// We can resolve versions by using Aether's
		// NearestVersionConflictResolver graph transformer, but in our case the
		// artifactIds were sufficient.
		List<String> sortedDependencies = new LinkedList<String>();
		for (DependencyNode dependencyNode : nodesList) {
			sortedDependencies.add(dependencyNode.getDependency().getArtifact().getArtifactId());
		}

		getLog().debug("Topologically sorted deps: " + sortedDependencies);
		
		return sortedDependencies;
	}

	/**
	 * 
	 * @param seenList
	 * @param dep
	 * @return true if the dependencyNode appears in the list
	 */
	private boolean contains(List<DependencyNode> seenList, DependencyNode dep) {
		for (DependencyNode node : seenList) {
			if (dep.getDependency().getArtifact().getArtifactId()
					.equals(node.getDependency().getArtifact().getArtifactId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds the compiler options and the linker options to the global variables
	 * 
	 * @param narArtifact
	 * @param binding
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
	public void addIncludesAndLibsPaths(NarArtifact narArtifact, String binding, String scope)
			throws MojoExecutionException, MojoFailureException {

		boolean test = false;
		if (scope.equals("test")) {
			test = true;
		}

		File unpackDirectory = getUnpackDirectory();
		File include = getLayout().getIncludeDirectory(unpackDirectory, narArtifact.getArtifactId(),
				narArtifact.getVersion());
		if (test) {
			makefileGenerator.addTestInclude(include.getPath());
			getLog().debug("adding test include: " + include.getPath());
		} else {
			if (include.exists()) {
				makefileGenerator.addInclude(include.getPath());
				getLog().debug("adding include: " + include.getPath());
			} else {
				getLog().info("No includes for " + narArtifact.getArtifactId());
			}
		}


		if (!binding.equals(Library.NONE)) {
			// add dependency lib paths
			File dir = getLayout().getLibDirectory(getUnpackDirectory(), narArtifact.getArtifactId(),
					narArtifact.getVersion(), NARaol.toString(), binding);
			String dirPath = dir.toString();
			if (test) {
				makefileGenerator.addTestLibPath(dirPath);
			} else {
				makefileGenerator.addLibPath(dirPath);
			}
			getLog().debug("adding Library Directory: " + dir);

			// add dependency lib
			String depLib = narArtifact.getArtifactId();
			if (test) {
				makefileGenerator.addTestLib(depLib);
			} else {
				makefileGenerator.addLib(depLib);
			}
			getLog().debug("adding Library: " + depLib);
		} else {
			getLog().info("No library for " + narArtifact.getArtifactId());
		}
	}

	/**
	 * Returns true if the List<String> contains the String element
	 * 
	 * @param list
	 * @param element
	 * @return boolean
	 */
	public boolean contains(List<String> list, String element) {
		for (String fromList : list) {
			if (fromList.equals(element)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * List the dependencies needed for tests compilations, those dependencies are
	 * used to get the include paths needed
	 * for compilation and to get the libraries paths and names needed for
	 * linking.
	 */
	@Override
	protected ScopeFilter getArtifactScopeFilter() {
	  // Was Artifact.SCOPE_TEST  - runtime??
	  return new ScopeFilter( Artifact.SCOPE_TEST, null );
	}

}
