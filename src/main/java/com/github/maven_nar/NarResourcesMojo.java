package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.SelectorUtils;

/**
 * Copies any resources, including AOL specific distributions, to the target area for packaging
 * 
 * @goal nar-resources
 * @phase process-resources
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarResourcesMojo
    extends AbstractResourcesMojo
{
    /**
     * Use given AOL only. If false, copy for all available AOLs.
     * 
     * @parameter property="nar.resources.copy.aol" default-value="true"
     * @required
     */
    private boolean resourcesCopyAOL;

    /**
     * Directory for nar resources. Defaults to src/nar/resources
     * 
     * @parameter default-value="${basedir}/src/nar/resources"
     * @required
     */
    private File resourceDirectory;

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // noarch resources
        try
        {
            int copied = 0;
            File noarchDir = new File( resourceDirectory, NarConstants.NAR_NO_ARCH );
            if ( noarchDir.exists() )
            {
                File noarchDstDir = getLayout().getNoArchDirectory( getTargetDirectory(), getMavenProject().getArtifactId(), getMavenProject().getVersion() );
                getLog().debug( "Copying noarch from " + noarchDir + " to " + noarchDstDir );
                copied += NarUtil.copyDirectoryStructure( noarchDir, noarchDstDir, null, NarUtil.DEFAULT_EXCLUDES );
            }
            getLog().info( "Copied " + copied + " resources" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NAR: Could not copy resources", e );
        }
        
        // scan resourceDirectory for AOLs
        File aolDir = new File( resourceDirectory, NarConstants.NAR_AOL );
        if ( aolDir.exists() )
        {
            String[] aol = aolDir.list();
            for ( int i = 0; i < aol.length; i++ )
            {
                // copy only resources of current AOL
                if ( resourcesCopyAOL && ( !aol[i].equals( getAOL().toString() ) ) )
                {
                    continue;
                }

                boolean ignore = false;
                for ( Iterator j = FileUtils.getDefaultExcludesAsList().iterator(); j.hasNext(); )
                {
                    String exclude = (String) j.next();
                    if ( SelectorUtils.matchPath( exclude.replace( '/', File.separatorChar ), aol[i] ) )
                    {
                        ignore = true;
                        break;
                    }
                }
                if ( !ignore )
                {
                    File aolFile = new File( aolDir, aol[i] );
                    copyResources( aolFile, aolFile.getName() );
                }
            }
        }
    }
}
