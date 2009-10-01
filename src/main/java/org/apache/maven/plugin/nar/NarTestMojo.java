// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Tests NAR files. Runs Native Tests and executables if produced.
 * 
 * @goal nar-test
 * @phase test
 * @requiresProject
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarTestMojo.java 51709c87671c 2007/08/08 22:49:17 duns $
 */
public class NarTestMojo extends AbstractCompileMojo {

	/**
	 * The classpath elements of the project being tested.
	 * 
	 * @parameter expression="${project.testClasspathElements}"
	 * @required
	 * @readonly
	 */
	private List classpathElements;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (shouldSkip())
			return;

		// run all tests
		for (Iterator i = getTests().iterator(); i.hasNext();) {
			runTest((Test) i.next());
		}

		for (Iterator i = getLibraries().iterator(); i.hasNext();) {
			runExecutable((Library) i.next());
		}
	}

	private void runTest(Test test) throws MojoExecutionException,
			MojoFailureException {
		// run if requested
		if (test.shouldRun()) {
			String name = "target/test-nar/bin/" + getAOL() + "/" + test.getName();
			getLog().info("Running " + name);
			List args = test.getArgs();
			int result = NarUtil.runCommand(getMavenProject()
					.getBasedir()
					+ "/" + name, (String[]) args.toArray(new String[args.size()]), generateEnvironment(test,
					getLog()), getLog());
			if (result != 0)
				throw new MojoFailureException("Test " + name
						+ " failed with exit code: " + result+" 0x"+Integer.toHexString(result));
		}
	}

	private void runExecutable(Library library) throws MojoExecutionException,
			MojoFailureException {
		if (library.getType().equals(Library.EXECUTABLE) && library.shouldRun()) {
			MavenProject project = getMavenProject();
			String name = "target/nar/bin/" + getAOL() + "/"
					+ project.getArtifactId();
			getLog().info("Running " + name);
			List args = library.getArgs();
			int result = NarUtil.runCommand(project.getBasedir()
					+ "/" + name, (String[]) args.toArray(new String[args.size()]), generateEnvironment(
					library, getLog()), getLog());
			if (result != 0)
				throw new MojoFailureException("Test " + name
						+ " failed with exit code: " + result+" 0x"+Integer.toHexString(result));
		}
	}

	protected File getTargetDirectory() {
		return new File(getMavenProject().getBuild().getDirectory(), "test-nar");
	}

	private String[] generateEnvironment(Executable exec, Log log)
			throws MojoExecutionException, MojoFailureException {
		List env = new ArrayList();

		Set/*<File>*/ sharedPaths = new HashSet();
		
		// add all shared libraries of this package
		for (Iterator i=getLibraries().iterator(); i.hasNext(); ) {
			Library lib = (Library)i.next();
			if (lib.getType().equals(Library.SHARED)) {
				sharedPaths.add(new File(getMavenProject().getBasedir(), "target/nar/lib/"+getAOL()+"/"+lib.getType()));
			}
		}

		// add dependent shared libraries
		String classifier = getAOL()+"-shared";
		List narArtifacts = getNarManager().getNarDependencies("compile");
		List dependencies = getNarManager().getAttachedNarDependencies(
				narArtifacts, classifier);
		for (Iterator d = dependencies.iterator(); d.hasNext();) {
			Artifact dependency = (Artifact) d.next();
			getLog().debug("Looking for dependency " + dependency);

			// FIXME reported to maven developer list, isSnapshot
			// changes behaviour
			// of getBaseVersion, called in pathOf.
			if (dependency.isSnapshot())
				;

			File libDir = new File(getLocalRepository().pathOf(dependency));
			libDir = new File(getLocalRepository().getBasedir(), libDir
					.getParent());
			libDir = new File(libDir, "nar/lib/"+getAOL()+"/shared");
			sharedPaths.add(libDir);
		}
		
		// set environment
		if (sharedPaths.size() > 0) {
			String sharedPath = "";
			for (Iterator i=sharedPaths.iterator(); i.hasNext(); ) {
				sharedPath += ((File)i.next()).getPath();
				if (i.hasNext()) sharedPath += File.pathSeparator;
			}
		
			String sharedEnv = NarUtil.addLibraryPathToEnv(sharedPath, null, getOS());
			env.add(sharedEnv);
		}
		
		// necessary to find WinSxS
		if (getOS().equals(OS.WINDOWS)) {
			env.add("SystemRoot="+NarUtil.getEnv("SystemRoot", "SystemRoot", "C:\\Windows"));
		}
		
		// add CLASSPATH
		env.add("CLASSPATH="+StringUtils.join(classpathElements.iterator(), File.pathSeparator));
		
		return env.size() > 0 ? (String[]) env.toArray(new String[env.size()]) : null;
	}
}
