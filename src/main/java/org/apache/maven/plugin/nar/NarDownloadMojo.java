// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Downloads any dependent NAR files. This includes the noarch and aol type NAR files.
 *
 * @goal nar-download
 * @phase generate-sources
 * @requiresProject
 * @requiresDependencyResolution
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarDownloadMojo.java c867ab546be1 2007/07/05 21:26:30 duns $
 */
public class NarDownloadMojo extends AbstractDependencyMojo {

	/**
	 * Artifact resolver, needed to download source jars for inclusion in
	 * classpath.
	 * 
	 * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
	 * @required
	 * @readonly
	 */
	private ArtifactResolver artifactResolver;

	/**
	 * Remote repositories which will be searched for source attachments.
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @required
	 * @readonly
	 */
	private List remoteArtifactRepositories;

	/**
	 * List of classifiers which you want download. Example ppc-MacOSX-g++,
	 * x86-Windows-msvc, i386-Linux-g++.
	 * 
	 * @parameter expression=""
	 */
	private List classifiers;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Using AOL: "+getAOL());
		
		if (shouldSkip()) {
    		getLog().info("***********************************************************************");
    		getLog().info("NAR Plugin SKIPPED, no NAR Libraries will be produced.");
    		getLog().info("***********************************************************************");
    		
    		return;
		}
		
		List narArtifacts = getNarManager().getNarDependencies("compile");
		if (classifiers == null) {
			getNarManager().downloadAttachedNars(narArtifacts, remoteArtifactRepositories,
					artifactResolver, null);
		} else {
			for (Iterator j = classifiers.iterator(); j.hasNext();) {
				getNarManager().downloadAttachedNars(narArtifacts, remoteArtifactRepositories,
						artifactResolver, (String) j.next());
			}
		}
	}
}
