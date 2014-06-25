package com.github.maven_nar;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Create the nar.properties file.
 * 
 * @goal nar-prepare-package
 * @phase prepare-package
 * @requiresProject
 * @author GDomjan
 */
public class NarPreparePackageMojo
    extends AbstractNarMojo
{    
    
    // TODO: this is working of what is present rather than what was requested to be built, POM ~/= artifacts!
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // let the layout decide which (additional) nars to attach
        getLayout().prepareNarInfo( getTargetDirectory(), getMavenProject(), getNarInfo(), this );

        getNarInfo().writeToDirectory( classesDirectory );
    }

}
