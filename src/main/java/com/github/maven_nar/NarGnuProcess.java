package com.github.maven_nar;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Move the GNU style output in the correct directories for nar-package
 * 
 * @goal nar-gnu-process
 * @phase process-classes
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarGnuProcess
    extends AbstractGnuMojo
{
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        File srcDir = getGnuAOLTargetDirectory();
        if ( srcDir.exists() )
        {
            getLog().info( "Running GNU process" );

            copyResources( srcDir, getAOL().toString() );
        }
    }
}
