package com.github.maven_nar;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Move the GNU style include/lib to some output directory
 * 
 * @goal nar-gnu-resources
 * @phase process-resources
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarGnuResources
    extends AbstractGnuMojo
{
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( getGnuSourceDirectory().exists() )
        {
            int copied = 0;
            
            try
            {
                copied += copyIncludes( getGnuSourceDirectory() );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "NAR: Gnu could not copy resources", e );
            }
            
            if (copied > 0) {
                getLog().info( "Copied "+copied+" GNU resources" );
            }

        }
    }
}
