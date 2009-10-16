package org.apache.maven.plugin.nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
    extends AbstractCompileMojo
{
    /**
     * Binary directory
     * 
     * @parameter expression="bin"
     * @required
     */
    private String resourceBinDir;

    /**
     * Include directory
     * 
     * @parameter expression="include"
     * @required
     */
    private String resourceIncludeDir;

    /**
     * Library directory
     * 
     * @parameter expression="lib"
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

    protected void copyResources( File srcDir, String aol)
        throws MojoExecutionException, MojoFailureException
    {
        int copied = 0;
        try
        {
            // copy headers
            File includeDir = new File( srcDir, resourceIncludeDir );
            if ( includeDir.exists() )
            {
                File includeDstDir = new File( getTargetDirectory(), "include" );
                copied += NarUtil.copyDirectoryStructure( includeDir, includeDstDir, null, NarUtil.DEFAULT_EXCLUDES );
            }

            // copy binaries
            File binDir = new File( srcDir, resourceBinDir );
            if ( binDir.exists() )
            {
                File binDstDir = new File( getTargetDirectory(), "bin" );
                binDstDir = new File( binDstDir, aol );

                copied += NarUtil.copyDirectoryStructure( binDir, binDstDir, null, NarUtil.DEFAULT_EXCLUDES );
            }

            // copy libraries
            File libDir = new File( srcDir, resourceLibDir );
            if ( libDir.exists() )
            {
                // create all types of libs
                for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
                {
                    Library library = (Library) i.next();
                    String type = library.getType();
                    File libDstDir = new File( getTargetDirectory(), "lib" );
                    libDstDir = new File( libDstDir, aol );
                    libDstDir = new File( libDstDir, type );

                    // filter files for lib
                    String includes =
                        "**/*."
                            + NarUtil.getDefaults().getProperty( NarUtil.getAOLKey( aol ) + "." + type + ".extension" );
                    copied += NarUtil.copyDirectoryStructure( libDir, libDstDir, includes, NarUtil.DEFAULT_EXCLUDES );
                }
            }

            // unpack jar files
            File classesDirectory = new File( getOutputDirectory(), "classes" );
            classesDirectory.mkdirs();
            List jars = FileUtils.getFiles( srcDir, "**/*.jar", null );
            for ( Iterator i = jars.iterator(); i.hasNext(); )
            {
                File jar = (File) i.next();
                getLog().debug( "Unpacking jar " + jar );
                UnArchiver unArchiver;
                unArchiver = archiverManager.getUnArchiver( AbstractNarMojo.NAR_ROLE_HINT );
                unArchiver.setSourceFile( jar );
                unArchiver.setDestDirectory( classesDirectory );
                unArchiver.extract();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NAR: Could not copy resources", e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "NAR: Could not find archiver", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "NAR: Could not unarchive jar file", e );
        }
        getLog().info( "Copied " + copied + " resources for " + aol );
    }

}
