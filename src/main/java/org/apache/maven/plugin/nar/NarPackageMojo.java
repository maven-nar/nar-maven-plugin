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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * Jars up the NAR files.
 * 
 * @goal nar-package
 * @phase package
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarPackageMojo
    extends AbstractCompileMojo
{

    /**
     * Used for attaching the artifact in the project
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( shouldSkip() )
            return;

        // FIX for NARPLUGIN-??? where -DupdateReleaseInfo copies to a .nar file
        getMavenProject().getArtifact().setArtifactHandler( new NarArtifactHandler() );

        // noarch
        File noarchDir = getLayout().getNoarchDirectory( getTargetDirectory() );
        if ( noarchDir.exists() )
        {
            String type = noarchDir.getName();
            attachNar( noarchDir, type );
            getNarInfo().setNar(
                                 null,
                                 type,
                                 getMavenProject().getGroupId() + ":" + getMavenProject().getArtifactId() + ":"
                                     + NAR_TYPE + ":" + type );
        }

        // create nar with binaries
        // FIXME NAR-90
        String bin = "bin";
        String[] binAOLs = new File( getTargetDirectory(), bin ).list();
        for ( int i = 0; i < ( binAOLs != null ? binAOLs.length : 0 ); i++ )
        {
            attachNarOld( bin + "/" + binAOLs[i], binAOLs[i], Library.EXECUTABLE );
        }

        // create nars for each type of library (static, shared).
        String bindingType = null;
        for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
        {
            Library library = (Library) i.next();
            String type = library.getType();
            if ( bindingType == null )
                bindingType = type;

            // create nar with libraries
            // FIXME NAR-90, the resources nar may copy extra libs in.
            File aolDirectory = getLayout().getAolDirectory( getTargetDirectory() );
            String[] subDirs = aolDirectory.list();
            for ( int j = 0; j < ( subDirs != null ? subDirs.length : 0 ); j++ )
            {
                attachNar( new File( aolDirectory, subDirs[j] ), subDirs[j] );
                getNarInfo().setNar(
                                     null,
                                     type,
                                     getMavenProject().getGroupId() + ":" + getMavenProject().getArtifactId() + ":"
                                         + NAR_TYPE + ":" + "${aol}-" + type );
            }
        }

        // override binding if not set
        if ( getNarInfo().getBinding( null, null ) == null )
        {
            getNarInfo().setBinding( null, bindingType != null ? bindingType : Library.NONE );
        }

        try
        {
            File propertiesDir =
                new File( getOutputDirectory(), "classes/META-INF/nar/" + getMavenProject().getGroupId() + "/"
                    + getMavenProject().getArtifactId() );
            if ( !propertiesDir.exists() )
            {
                propertiesDir.mkdirs();
            }
            File propertiesFile = new File( propertiesDir, NarInfo.NAR_PROPERTIES );
            getNarInfo().writeToFile( propertiesFile );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Cannot write nar properties file", ioe );
        }
    }

    /**
     * @param file
     * @param string
     * @param type
     * @throws MojoExecutionException
     */
    private void attachNar( File dir, String string )
        throws MojoExecutionException
    {
        String aolType = dir.getName();
        File narFile = new File( getOutputDirectory(), getFinalName() + "-" + dir.getName() + "." + NAR_EXTENSION );
        nar( narFile, dir );
        projectHelper.attachArtifact( getMavenProject(), NAR_TYPE, aolType, narFile );
    }

    private void attachNarOld( String dir, String aol, String type )
        throws MojoExecutionException
    {
        File libFile =
            new File( getOutputDirectory(), getFinalName() + "-" + ( aol != null ? aol + "-" : "" ) + type + "."
                + NAR_EXTENSION );
        narOld( libFile, getTargetDirectory(), new String[] { dir } );
        projectHelper.attachArtifact( getMavenProject(), NAR_TYPE, ( aol != null ? aol + "-" : "" ) + type, libFile );
        getNarInfo().setNar(
                             null,
                             type,
                             getMavenProject().getGroupId() + ":" + getMavenProject().getArtifactId() + ":" + NAR_TYPE
                                 + ":" + ( aol != null ? "${aol}-" : "" ) + type );

    }

    private void nar( File nar, File dir )
        throws MojoExecutionException
    {
        try
        {
            if ( nar.exists() )
            {
                nar.delete();
            }

            Archiver archiver = new ZipArchiver();
            // seems to return same archiver all the time
            // archiverManager.getArchiver(NAR_ROLE_HINT);
            String[] includes = new String[] { "*/**" };
            archiver.addDirectory( dir, includes, null );
            archiver.setDestFile( nar );
            archiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error while creating NAR archive.", e );
            // } catch (NoSuchArchiverException e) {
            // throw new MojoExecutionException("Error while creating NAR
            // archive.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error while creating NAR archive.", e );
        }
    }

    private void narOld( File nar, File dir, String[] dirs )
        throws MojoExecutionException
    {
        try
        {
            if ( nar.exists() )
            {
                nar.delete();
            }

            Archiver archiver = new ZipArchiver();
            // seems to return same archiver all the time
            // archiverManager.getArchiver(NAR_ROLE_HINT);
            for ( int i = 0; i < dirs.length; i++ )
            {
                String[] includes = new String[] { dirs[i] + "/**" };
                archiver.addDirectory( dir, includes, null );
            }
            archiver.setDestFile( nar );
            archiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error while creating NAR archive.", e );
            // } catch (NoSuchArchiverException e) {
            // throw new MojoExecutionException("Error while creating NAR
            // archive.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error while creating NAR archive.", e );
        }
    }

}
