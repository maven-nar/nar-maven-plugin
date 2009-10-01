// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

public class NarManager {

	private Log log;

	private MavenProject project;

	private ArtifactRepository repository;

	private AOL defaultAOL;
	private String linkerName;
	
	private String[] narTypes = { "noarch", Library.STATIC, Library.SHARED, Library.JNI, Library.PLUGIN };

	public NarManager(Log log, ArtifactRepository repository,
			MavenProject project, String architecture, String os, Linker linker)
			throws MojoFailureException {
		this.log = log;
		this.repository = repository;
		this.project = project;
		this.defaultAOL = NarUtil.getAOL(architecture, os, linker, null);
		this.linkerName = NarUtil.getLinkerName(architecture, os, linker);
	}

	/**
	 * Returns dependencies which are dependent on NAR files (i.e. contain
	 * NarInfo)
	 */
	public List/* <NarArtifact> */getNarDependencies(String scope)
			throws MojoExecutionException {
		List narDependencies = new LinkedList();
		for (Iterator i = getDependencies(scope).iterator(); i.hasNext();) {
			Artifact dependency = (Artifact) i.next();
			log.debug("Examining artifact for NarInfo: "+dependency);
			
			NarInfo narInfo = getNarInfo(dependency);
			if (narInfo != null) {
				log.debug("    - added as NarDependency");
				narDependencies.add(new NarArtifact(dependency, narInfo));
			}
		}
		return narDependencies;
	}

	/**
	 * Returns all NAR dependencies by type: noarch, static, dynamic, jni,
	 * plugin.
	 * 
	 * @throws MojoFailureException
	 */
	public Map/* <String, List<AttachedNarArtifact>> */getAttachedNarDependencyMap(
			String scope) throws MojoExecutionException, MojoFailureException {
		Map attachedNarDependencies = new HashMap();
		for (Iterator i = getNarDependencies(scope).iterator(); i.hasNext();) {
			Artifact dependency = (Artifact) i.next();
			for (int j = 0; j < narTypes.length; j++) {
				List artifactList = getAttachedNarDependencies(dependency,
						defaultAOL, narTypes[j]);
				if (artifactList != null)
					attachedNarDependencies.put(narTypes[j], artifactList);
			}
		}
		return attachedNarDependencies;
	}

	public List/* <AttachedNarArtifact> */getAttachedNarDependencies(
			List/* <NarArtifacts> */narArtifacts)
			throws MojoExecutionException, MojoFailureException {
		return getAttachedNarDependencies(narArtifacts, null);
	}

	public List/* <AttachedNarArtifact> */getAttachedNarDependencies(
			List/* <NarArtifacts> */narArtifacts, String classifier)
			throws MojoExecutionException, MojoFailureException {
		AOL aol = null;
		String type = null;
		if (classifier != null) {
			int dash = classifier.lastIndexOf('-');
			if (dash < 0) {
				aol = new AOL(classifier);
				type = null;
			} else {
				aol = new AOL(classifier.substring(0, dash));
				type = classifier.substring(dash + 1);
			}
		}
		return getAttachedNarDependencies(narArtifacts, aol, type);
	}

	/**
	 * Returns a list of all attached nar dependencies for a specific binding
	 * and "noarch", but not where "local" is specified
	 * 
	 * @param scope
	 *            compile, test, runtime, ....
	 * @param aol
	 *            either a valid aol, noarch or null. In case of null both the
	 *            default getAOL() and noarch dependencies are returned.
	 * @param type
	 *            noarch, static, shared, jni, or null. In case of null the
	 *            default binding found in narInfo is used.
	 * @return
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
	public List/* <AttachedNarArtifact> */getAttachedNarDependencies(
			List/* <NarArtifacts> */narArtifacts, AOL aol, String type)
			throws MojoExecutionException, MojoFailureException {
		boolean noarch = false;
		if (aol == null) {
			noarch = true;
			aol = defaultAOL;
		}

		List artifactList = new ArrayList();
		for (Iterator i = narArtifacts.iterator(); i.hasNext();) {
			Artifact dependency = (Artifact) i.next();
			NarInfo narInfo = getNarInfo(dependency);
			if (noarch) {
				artifactList.addAll(getAttachedNarDependencies(dependency,
						null, "noarch"));
			}

			// use preferred binding, unless non existing.
			String binding = narInfo.getBinding(aol, type != null ? type
					: "static");

			// FIXME kludge, but does not work anymore since AOL is now a class
			if (aol.equals("noarch")) {
				// FIXME no handling of local
				artifactList.addAll(getAttachedNarDependencies(dependency,
						null, "noarch"));
			} else {
				artifactList.addAll(getAttachedNarDependencies(dependency, aol,
						binding));
			}
		}
		return artifactList;
	}

	private List/* <AttachedNarArtifact> */getAttachedNarDependencies(
			Artifact dependency, AOL aol, String type)
			throws MojoExecutionException, MojoFailureException {
		log.debug("GetNarDependencies for " + dependency + ", aol: " + aol + ", type: " + type);
		List artifactList = new ArrayList();
		NarInfo narInfo = getNarInfo(dependency);
		String[] nars = narInfo.getAttachedNars(aol, type);
		// FIXME Move this to NarInfo....
		if (nars != null) {
			for (int j = 0; j < nars.length; j++) {
				log.debug("    Checking: " + nars[j]);
				if (nars[j].equals(""))
					continue;
				String[] nar = nars[j].split(":", 5);
				if (nar.length >= 4) {
					try {
						String groupId = nar[0].trim();
						String artifactId = nar[1].trim();
						String ext = nar[2].trim();
						String classifier = nar[3].trim();
						// translate for instance g++ to gcc...
						aol = narInfo.getAOL(aol);
						if (aol != null) {
							classifier = NarUtil.replace("${aol}", aol.toString(),
									classifier);
						}
						String version = nar.length >= 5 ? nar[4].trim()
								: dependency.getVersion();
						artifactList.add(new AttachedNarArtifact(groupId,
								artifactId, version, dependency.getScope(),
								ext, classifier, dependency.isOptional()));
					} catch (InvalidVersionSpecificationException e) {
						throw new MojoExecutionException(
								"Error while reading nar file for dependency "
										+ dependency, e);
					}
				} else {
					log.warn("nars property in " + dependency.getArtifactId()
							+ " contains invalid field: '" + nars[j]
							+ "' for type: " + type);
				}
			}
		}
		return artifactList;
	}

	public NarInfo getNarInfo(Artifact dependency)
			throws MojoExecutionException {
		// FIXME reported to maven developer list, isSnapshot changes behaviour
		// of getBaseVersion, called in pathOf.
		if (dependency.isSnapshot())
			;

		File file = new File(repository.getBasedir(), repository
				.pathOf(dependency));
		JarFile jar = null;
		try {
			jar = new JarFile(file);
			NarInfo info = new NarInfo(dependency.getGroupId(), dependency
					.getArtifactId(), dependency.getVersion(), log);
			if (!info.exists(jar))
				return null;
			info.read(jar);
			return info;
		} catch (IOException e) {
			throw new MojoExecutionException("Error while reading " + file, e);
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public File getNarFile(Artifact dependency) throws MojoFailureException {
		// FIXME reported to maven developer list, isSnapshot changes behaviour
		// of getBaseVersion, called in pathOf.
		if (dependency.isSnapshot())
			;
		return new File(repository.getBasedir(), NarUtil.replace("${aol}",
				defaultAOL.toString(), repository.pathOf(dependency)));
	}

	private List getDependencies(String scope) {
		if (scope.equals("test")) {
			return project.getTestArtifacts();
		} else if (scope.equals("runtime")) {
			return project.getRuntimeArtifacts();
		}
		return project.getCompileArtifacts();
	}

	public void downloadAttachedNars(List/* <NarArtifacts> */narArtifacts,
			List remoteRepositories, ArtifactResolver resolver,
			String classifier) throws MojoExecutionException,
			MojoFailureException {
		// FIXME this may not be the right way to do this.... -U ignored and
		// also SNAPSHOT not used
		List dependencies = getAttachedNarDependencies(narArtifacts, classifier);
		for (Iterator i = dependencies.iterator(); i.hasNext();) {
			Artifact dependency = (Artifact) i.next();
			try {
				log.debug("Resolving " + dependency);
				resolver.resolve(dependency, remoteRepositories, repository);
			} catch (ArtifactNotFoundException e) {
				String message = "nar not found " + dependency.getId();
				throw new MojoExecutionException(message, e);
			} catch (ArtifactResolutionException e) {
				String message = "nar cannot resolve " + dependency.getId();
				throw new MojoExecutionException(message, e);
			}
		}
	}

	public void unpackAttachedNars(List/* <NarArtifacts> */narArtifacts,
			ArchiverManager manager, String classifier, String os)
			throws MojoExecutionException, MojoFailureException {
		log.debug("Unpack called for OS: "+os+", classifier: "+classifier+" for NarArtifacts {");
		for (Iterator i = narArtifacts.iterator(); i.hasNext(); ) {
			log.debug("  - "+((NarArtifact)i.next()));
		}
		log.debug("}");
		// FIXME, kludge to get to download the -noarch, based on classifier
		List dependencies = getAttachedNarDependencies(narArtifacts, classifier);
		for (Iterator i = dependencies.iterator(); i.hasNext();) {
			Artifact dependency = (Artifact) i.next();
			log.debug("Unpack " + dependency);
			File file = getNarFile(dependency);
			File narLocation = new File(file.getParentFile(), "nar");
			File flagFile = new File(narLocation, FileUtils.basename(file
					.getPath(), "." + AbstractNarMojo.NAR_EXTENSION)
					+ ".flag");

			boolean process = false;
			if (!narLocation.exists()) {
				narLocation.mkdirs();
				process = true;
			} else if (!flagFile.exists()) {
				process = true;
			} else if (file.lastModified() > flagFile.lastModified()) {
				process = true;
			}

			if (process) {
				try {
					unpackNar(manager, file, narLocation);
					if (!NarUtil.getOS(os).equals("Windows")) {
						NarUtil.makeExecutable(new File(narLocation, "bin/"+defaultAOL),
								log);
						// FIXME clumsy
						if (defaultAOL.hasLinker("g++")) {
							NarUtil.makeExecutable(new File(narLocation, "bin/"+NarUtil.replace("g++", "gcc", defaultAOL.toString())),
									log);							
						}
					}
					if (linkerName.equals("gcc") || linkerName.equals("g++")) {
						NarUtil.runRanlib(new File(narLocation, "lib/"+defaultAOL), log);
						// FIXME clumsy
						if (defaultAOL.hasLinker("g++")) {
							NarUtil.runRanlib(new File(narLocation, "lib/"+NarUtil.replace("g++", "gcc", defaultAOL.toString())),
									log);							
						}
					}
					FileUtils.fileDelete(flagFile.getPath());
					FileUtils.fileWrite(flagFile.getPath(), "");
				} catch (IOException e) {
					log.warn("Cannot create flag file: " + flagFile.getPath());
				}
			}
		}
	}

	private void unpackNar(ArchiverManager manager, File file, File location)
			throws MojoExecutionException {
		try {
			UnArchiver unArchiver;
			unArchiver = manager.getUnArchiver(AbstractNarMojo.NAR_ROLE_HINT);
			unArchiver.setSourceFile(file);
			unArchiver.setDestDirectory(location);
			unArchiver.extract();
		} catch (IOException e) {
			throw new MojoExecutionException("Error unpacking file: " + file
					+ " to: " + location, e);
		} catch (NoSuchArchiverException e) {
			throw new MojoExecutionException("Error unpacking file: " + file
					+ " to: " + location, e);
		} catch (ArchiverException e) {
			throw new MojoExecutionException("Error unpacking file: " + file
					+ " to: " + location, e);
		}
	}
}
