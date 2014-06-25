package com.github.maven_nar;

import java.util.List;

/**
 * Downloads any dependent NAR files. This includes the noarch and aol type NAR files.
 * 
 * @goal nar-download-dependencies
 * @phase process-sources
 * @requiresProject
 * @requiresDependencyResolution test
 * @author Mark Donszelmann
 */
public class NarDownloadDependenciesMojo
    extends AbstractDependencyMojo
{
    
	// excludeTransitive 
	@Override
	protected List/*<Artifact>*/ getArtifacts() {
		return getMavenProject().getTestArtifacts();  // Artifact.SCOPE_TEST 
	}	

}
