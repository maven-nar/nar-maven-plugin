package com.github.maven_nar;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Jar up the NAR files and attach them to the projects main artifact (for installation and deployment).
 * 
 * @goal nar-package
 * @phase package
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarPackageMojo
    extends AbstractNarMojo
{    
    /**
     * To look up Archiver/UnArchiver implementations
     * 
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;
    
    /**
     * Used for attaching the artifact in the project
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;


    // TODO: this is working of what is present rather than what was requested to be built, POM ~/= artifacts!
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // let the layout decide which nars to attach
        getLayout().attachNars( getTargetDirectory(), archiverManager, projectHelper, getMavenProject() );
     
    }
}
