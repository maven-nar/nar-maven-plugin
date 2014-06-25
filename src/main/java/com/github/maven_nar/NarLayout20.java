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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils;

/**
 * Initial layout which expands a nar file into:
 * 
 * <pre>
 * nar/includue
 * nar/bin
 * nar/lib
 * </pre>
 * 
 * this layout was abandoned because there is no one-to-one relation between the nar file and its directory structure.
 * Therefore SNAPSHOTS could not be fully deleted when replaced.
 * 
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarLayout20
    extends AbstractNarLayout
{
    private NarFileLayout fileLayout;

    public NarLayout20( Log log )
    {
        super( log );
        this.fileLayout = new NarFileLayout10();
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#getNoArchDirectory(java.io.File)
     */
    public File getNoArchDirectory( File baseDir, String artifactId, String version )
        throws MojoExecutionException, MojoFailureException
    {
        return baseDir;
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#getIncludeDirectory(java.io.File)
     */
    public final File getIncludeDirectory( File baseDir, String artifactId, String version )
    {
        return new File( baseDir, fileLayout.getIncludeDirectory() );
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#getLibDir(java.io.File, com.github.maven_nar.AOL, String type)
     */
    public final File getLibDirectory( File baseDir, String artifactId, String version, String aol, String type )
        throws MojoFailureException
    {
        if ( type.equals( Library.EXECUTABLE ) )
        {
            throw new MojoFailureException( "INTERNAL ERROR, Replace call to getLibDirectory with getBinDirectory" );
        }

        File dir = new File( baseDir, fileLayout.getLibDirectory( aol, type ) );
        return dir;
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#getBinDirectory(java.io.File, java.lang.String)
     */
    public final File getBinDirectory( File baseDir, String artifactId, String version, String aol )
    {
        File dir = new File( baseDir, fileLayout.getBinDirectory( aol ) );
        return dir;
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#attachNars(java.io.File, org.apache.maven.project.MavenProjectHelper,
     * org.apache.maven.project.MavenProject, com.github.maven_nar.NarInfo)
     */
    public final void prepareNarInfo( File baseDir, MavenProject project, NarInfo narInfo, AbstractNarMojo mojo )
        throws MojoExecutionException
    {
        if ( getIncludeDirectory( baseDir, project.getArtifactId(), project.getVersion() ).exists() )
        {
            narInfo.setNar( null, "noarch", project.getGroupId() + ":" + project.getArtifactId() + ":"
                + NarConstants.NAR_TYPE + ":" + "noarch" );
        }

        String[] binAOL = new File( baseDir, "bin" ).list();
        for ( int i = 0; ( binAOL != null ) && ( i < binAOL.length ); i++ )
        {// TODO: chose not to apply new file naming for outfile in case of backwards compatability,  may need to reconsider
            narInfo.setNar( null, Library.EXECUTABLE, project.getGroupId() + ":" + project.getArtifactId() + ":"
                + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + Library.EXECUTABLE );
            narInfo.setBinding( new AOL( binAOL[i] ), Library.EXECUTABLE );
            narInfo.setBinding( null, Library.EXECUTABLE );
        }

        File libDir = new File( baseDir, "lib" );
        String[] libAOL = libDir.list();
        for ( int i = 0; ( libAOL != null ) && ( i < libAOL.length ); i++ )
        {
            String bindingType = null;
            String[] libType = new File( libDir, libAOL[i] ).list();
            for ( int j = 0; ( libType != null ) && ( j < libType.length ); j++ )
            {
                narInfo.setNar( null, libType[j], project.getGroupId() + ":" + project.getArtifactId() + ":"
                    + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + libType[j] );

                // set if not set or override if SHARED
                if ( ( bindingType == null ) || libType[j].equals( Library.SHARED ) )
                {
                    bindingType = libType[j];
                }
            }

            AOL aol = new AOL( libAOL[i] );
            if ( narInfo.getBinding( aol, null ) == null )
            {
                narInfo.setBinding( aol, bindingType != null ? bindingType : Library.NONE );
            }
            if ( narInfo.getBinding( null, null ) == null )
            {
                narInfo.setBinding( null, bindingType != null ? bindingType : Library.NONE );
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#attachNars(java.io.File, org.apache.maven.project.MavenProjectHelper,
     * org.apache.maven.project.MavenProject, com.github.maven_nar.NarInfo)
     */
    public final void attachNars( File baseDir, ArchiverManager archiverManager, MavenProjectHelper projectHelper,
                                  MavenProject project )
        throws MojoExecutionException
    {
        if ( getIncludeDirectory( baseDir, project.getArtifactId(), project.getVersion() ).exists() )
        {
            attachNar( archiverManager, projectHelper, project, "noarch", baseDir, "include/**" );
        }

        String[] binAOL = new File( baseDir, "bin" ).list();
        for ( int i = 0; ( binAOL != null ) && ( i < binAOL.length ); i++ )
        {
            attachNar( archiverManager, projectHelper, project, binAOL[i] + "-" + Library.EXECUTABLE, baseDir, "bin/"
                + binAOL[i] + "/**" );
        }

        File libDir = new File( baseDir, "lib" );
        String[] libAOL = libDir.list();
        for ( int i = 0; ( libAOL != null ) && ( i < libAOL.length ); i++ )
        {
            String bindingType = null;
            String[] libType = new File( libDir, libAOL[i] ).list();
            for ( int j = 0; ( libType != null ) && ( j < libType.length ); j++ )
            {
                attachNar( archiverManager, projectHelper, project, libAOL[i] + "-" + libType[j], baseDir, "lib/"
                    + libAOL[i] + "/" + libType[j] + "/**" );
            }

        }
    }

    public void unpackNar( File unpackDir, ArchiverManager archiverManager, File file, String os, String linkerName,
                           AOL defaultAOL )
        throws MojoExecutionException, MojoFailureException
    {
        File flagFile =
            new File( unpackDir, FileUtils.basename( file.getPath(), "." + NarConstants.NAR_EXTENSION ) + ".flag" );

        boolean process = false;
        if ( !unpackDir.exists() )
        {
            unpackDir.mkdirs();
            process = true;
        }
        else if ( !flagFile.exists() )
        {
            process = true;
        }
        else if ( file.lastModified() > flagFile.lastModified() )
        {
            process = true;
        }

        if ( process )
        {
            try
            {
                unpackNarAndProcess( archiverManager, file, unpackDir, os, linkerName, defaultAOL );
                FileUtils.fileDelete( flagFile.getPath() );
                FileUtils.fileWrite( flagFile.getPath(), "" );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Cannot create flag file: " + flagFile.getPath(), e );
            }
        }
    }

    public File getNarUnpackDirectory(File baseUnpackDirectory, File narFile)
    {
        File dir = new File(
            baseUnpackDirectory,
            FileUtils.basename( narFile.getPath(), "." + NarConstants.NAR_EXTENSION ));
        return dir;
    }
}
