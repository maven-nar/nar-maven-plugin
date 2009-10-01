// Copyright FreeHEP, 2006-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Assemble libraries of NAR files.
 * 
 * @goal nar-assembly
 * @phase process-resources
 * @requiresProject
 * @requiresDependencyResolution
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarAssemblyMojo.java c867ab546be1 2007/07/05 21:26:30 duns $
 */
public class NarAssemblyMojo extends AbstractDependencyMojo {

	/**
	 * List of classifiers which you want to assemble. Example ppc-MacOSX-g++-static,
	 * x86-Windows-msvc-shared, i386-Linux-g++-executable, ....
	 * 
	 * @parameter expression=""
	 * @required
	 */
	private List classifiers;

	/**
	 * Copies the unpacked nar libraries and files into the projects target area
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (shouldSkip()) {
			getLog()
					.info(
							"***********************************************************************");
			getLog()
					.info(
							"NAR Assembly SKIPPED since no NAR libraries were built/downloaded.");
			getLog()
					.info(
							"***********************************************************************");
			// NOTE: continue since the standard assemble mojo fails if we do
			// not create the directories...
		}

		for (Iterator j = classifiers.iterator(); j.hasNext();) {
			String classifier = (String) j.next();

			List narArtifacts = getNarManager().getNarDependencies("compile");
			List dependencies = getNarManager().getAttachedNarDependencies(
					narArtifacts, classifier);
			// this may make some extra copies...
			for (Iterator d = dependencies.iterator(); d.hasNext();) {
				Artifact dependency = (Artifact) d.next();
				getLog().debug("Assemble from " + dependency);

				// FIXME reported to maven developer list, isSnapshot
				// changes behaviour
				// of getBaseVersion, called in pathOf.
				if (dependency.isSnapshot())
					;

				File srcDir = new File(getLocalRepository().pathOf(dependency));
				srcDir = new File(getLocalRepository().getBasedir(), srcDir
						.getParent());
				srcDir = new File(srcDir, "nar/");
				File dstDir = new File("target/nar/");
				try {
					FileUtils.mkdir(dstDir.getPath());
					if (shouldSkip()) {
						File note = new File(dstDir, "NAR_ASSEMBLY_SKIPPED");
						FileUtils
								.fileWrite(
										note.getPath(),
										"The NAR Libraries of this distribution are missing because \n"
												+ "the NAR dependencies were not built/downloaded, presumably because\n"
												+ "the the distribution was built with the '-Dnar.skip=true' flag.");
					} else {
						getLog().debug("SrcDir: " + srcDir);
						if (srcDir.exists()) {
							FileUtils.copyDirectoryStructure(srcDir, dstDir);
						}
					}
				} catch (IOException ioe) {
					throw new MojoExecutionException(
							"Failed to copy directory for dependency "
									+ dependency + " from "+srcDir+" to " + dstDir, ioe);
				}
			}
		}
	}
}
