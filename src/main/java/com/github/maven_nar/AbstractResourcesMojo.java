/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Keeps track of resources
 * 
 * @author Mark Donszelmann
 */
public abstract class AbstractResourcesMojo
    extends AbstractNarMojo
{
    /**
     * Binary directory
     * 
     * @parameter default-value="bin"
     * @required
     */
    private String resourceBinDir;

    /**
     * Include directory
     * 
     * @parameter default-value="include"
     * @required
     */
    private String resourceIncludeDir;

    /**
     * Library directory
     * 
     * @parameter default-value="lib"
     * @required
     */
    private String resourceLibDir;

    /**
     * To look up Archiver/UnArchiver implementations
     * 
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;

    protected final int copyIncludes( File srcDir )
        throws IOException, MojoExecutionException, MojoFailureException
    {
        int copied = 0;

        // copy includes
        File includeDir = new File( srcDir, resourceIncludeDir );
        if ( includeDir.exists() )
        {
            File includeDstDir =
                getLayout().getIncludeDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                                 getMavenProject().getVersion() );
            getLog().debug( "Copying includes from " + includeDir + " to " + includeDstDir );
            copied += NarUtil.copyDirectoryStructure( includeDir, includeDstDir, null, NarUtil.DEFAULT_EXCLUDES );
        }

        return copied;
    }

    protected final int copyBinaries( File srcDir, String aol )
        throws IOException, MojoExecutionException, MojoFailureException
    {
        int copied = 0;

        // copy binaries
        File binDir = new File( srcDir, resourceBinDir );
        if ( binDir.exists() )
        {
            File binDstDir =
                getLayout().getBinDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                             getMavenProject().getVersion(), aol );
            getLog().debug( "Copying binaries from " + binDir + " to " + binDstDir );
            copied += NarUtil.copyDirectoryStructure( binDir, binDstDir, null, NarUtil.DEFAULT_EXCLUDES );
        }

        return copied;
    }

    protected final int copyLibraries( File srcDir, String aol )
        throws MojoFailureException, IOException, MojoExecutionException
    {
        int copied = 0;

        // copy libraries
        File libDir = new File( srcDir, resourceLibDir );
        if ( libDir.exists() )
        {
        	// TODO:  copyLibraries is used on more than just this artifact - this check needs to be placed elsewhere
			if( getLibraries().isEmpty() )
				getLog().warn("Appear to have library resources, but not Libraries are defined");
            // create all types of libs
            for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
            {
                Library library = (Library) i.next();
                String type = library.getType();
                
                File typedLibDir = new File( libDir, type );
                if ( typedLibDir.exists() ) libDir = typedLibDir;
                
                File libDstDir =
                    getLayout().getLibDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                                 getMavenProject().getVersion(), aol, type );
                getLog().debug( "Copying libraries from " + libDir + " to " + libDstDir );

                // filter files for lib
                String includes =
                    "**/*." + NarProperties.getInstance(getMavenProject()).getProperty( NarUtil.getAOLKey( aol ) + "." + type + ".extension" );

                // add import lib for Windows shared libraries
                if ( new AOL( aol ).getOS().equals( OS.WINDOWS ) && type.equals( Library.SHARED ) )
                {
                    includes += ",**/*.lib";
                }
                copied += NarUtil.copyDirectoryStructure( libDir, libDstDir, includes, NarUtil.DEFAULT_EXCLUDES );
            }
        }

        return copied;
    }

    protected final void copyResources( File srcDir, String aol )
        throws MojoExecutionException, MojoFailureException
    {
        int copied = 0;
        try
        {
            copied += copyIncludes( srcDir );

            copied += copyBinaries( srcDir, aol );

            copied += copyLibraries( srcDir, aol );

            // unpack jar files
            File classesDirectory = new File( getOutputDirectory(), "classes" );
            classesDirectory.mkdirs();
            List jars = FileUtils.getFiles( srcDir, "**/*.jar", null );
            for ( Iterator i = jars.iterator(); i.hasNext(); )
            {
                File jar = (File) i.next();
                getLog().debug( "Unpacking jar " + jar );
                UnArchiver unArchiver;
                unArchiver = archiverManager.getUnArchiver( NarConstants.NAR_ROLE_HINT );
                unArchiver.setSourceFile( jar );
                unArchiver.setDestDirectory( classesDirectory );
                unArchiver.extract();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NAR: Could not copy resources for " + aol, e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "NAR: Could not find archiver for " + aol, e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "NAR: Could not unarchive jar file for " + aol, e );
        }
        getLog().info( "Copied " + copied + " resources for " + aol );
    }

}
