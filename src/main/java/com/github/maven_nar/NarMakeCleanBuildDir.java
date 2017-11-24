package com.github.maven_nar;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;

/**
 * Removes the .jar and .nar that could have been left from previous builds.
 * 
 * @goal nar-make-clean-build-dir
 * @requiresProject
 * @author Jeremy Nguyen-Xuan (CERN)
 */
public class NarMakeCleanBuildDir extends AbstractCompileMojo {

	@Override
	public void narExecute() throws MojoFailureException, MojoExecutionException {
		cleanTarget();
		cleanTargetNar();
	}

	private void cleanTargetNar() {
		File narDirectory = getTargetDirectory();
		File[] listOfFiles = narDirectory.listFiles();
		String artifactId = getMavenProject().getArtifactId();
		String version = getMavenProject().getVersion();
		String regex = artifactId + "-" + version + "(-noarch|(-[A-Za-z0-9_]+){4})";
		
		getLog().debug(regex);
		for (File current : listOfFiles) {
			// WARNING: This will fail if the current project depends on another project with extends its name with a number.
			// For example if cmw-lib depends on cmw-lib-util-2. So it is not very likely to happen.
			if (current.isDirectory() && current.getName().startsWith(artifactId + "-[0-9]")) { 
				getLog().debug(current.getName());
				if (!current.getName().matches(regex)) {
					getLog().warn(current.getName() + " will be deleted. It may result from a previous build.");
					if(!deleteDirectory(current)){
						getLog().error("Could not delete directory " + current.getName() + ". It can be unsafe to call mvn deploy!");
					}
				}
			}
		}
	}

	private void cleanTarget() {
		File targetDirectory = getOutputDirectory();
		File[] listOfFiles = targetDirectory.listFiles();

		String artifactId = getMavenProject().getArtifactId();
		String version = getMavenProject().getVersion();
		//String regex = artifactId + "-" + version + "(-noarch\\.nar|\\.jar|-[A-Za-z0-9]+-[A-Za-z0-9]+-[A-Za-z0-9]+-[A-Za-z0-9]+\\.nar)";
		String regex = artifactId + "-" + version + "(-noarch\\.nar|\\.jar|(-[A-Za-z0-9_]+){4}\\.nar)";
		
		getLog().debug(regex);
		for (File currentFile : listOfFiles) {
			if (currentFile.isFile()) {
				getLog().debug(currentFile.getName());
				if (!currentFile.getName().matches(regex)) {
					getLog().warn(currentFile.getName() + " will be deleted. It may result from a previous build.");
					if(!currentFile.delete()){
						getLog().error("Could not delete file " + currentFile.getName() + ". It can be unsafe to call mvn deploy!");
					}
				}
			}
		}
	}

	
	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
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
