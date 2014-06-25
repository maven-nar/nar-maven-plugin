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
import java.util.ArrayList;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils;

/**
 * Layout which expands a nar file into:
 * 
 * <pre>
 * nar/noarch/include
 * nar/aol/<aol>-<type>/bin
 * nar/aol/<aol>-<type>/lib
 * </pre>
 * 
 * This loayout has a one-to-one relation with the aol-type version of the nar.
 * 
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarLayout21
    extends AbstractNarLayout
{
    private NarFileLayout fileLayout;

    public NarLayout21( Log log )
    {
        super( log );
        this.fileLayout = new NarFileLayout10();
    }

    public File getNoArchDirectory( File baseDir, String artifactId, String version )
    {
        return new File( baseDir, artifactId + "-" + version + "-" + NarConstants.NAR_NO_ARCH );
    }

    private File getAolDirectory( File baseDir, String artifactId, String version, String aol, String type )
    {
        return new File( baseDir, artifactId + "-" + version + "-" + aol + "-" + type );
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#getIncludeDirectory(java.io.File)
     */
    public final File getIncludeDirectory( File baseDir, String artifactId, String version )
    {
        return new File( getNoArchDirectory( baseDir, artifactId, version ), fileLayout.getIncludeDirectory() );
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#getLibDir(java.io.File, com.github.maven_nar.AOL,
     * java.lang.String)
     */
    public final File getLibDirectory( File baseDir, String artifactId, String version, String aol, String type )
        throws MojoExecutionException
    {
        if ( type.equals( Library.EXECUTABLE ) )
        {
            throw new MojoExecutionException(
                                              "NAR: for type EXECUTABLE call getBinDirectory instead of getLibDirectory" );
        }

        File dir = getAolDirectory( baseDir, artifactId, version, aol, type );
        dir = new File( dir, fileLayout.getLibDirectory( aol, type ) );
        return dir;
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarLayout#getLibDir(java.io.File, com.github.maven_nar.AOL,
     * java.lang.String)
     */
    public final File getBinDirectory( File baseDir, String artifactId, String version, String aol )
    {
        File dir = getAolDirectory( baseDir, artifactId, version, aol, Library.EXECUTABLE );
        dir = new File( dir, fileLayout.getBinDirectory( aol ) );
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
        if ( getNoArchDirectory( baseDir, project.getArtifactId(), project.getVersion() ).exists() )
        {
            narInfo.setNar( null, NarConstants.NAR_NO_ARCH, project.getGroupId() + ":" + project.getArtifactId() + ":"
                + NarConstants.NAR_TYPE + ":" + NarConstants.NAR_NO_ARCH );
        }

        String artifactIdVersion = project.getArtifactId() + "-" + project.getVersion();
        // list all directories in basedir, scan them for classifiers
        String[] subDirs = baseDir.list();
        ArrayList<String> classifiers = new ArrayList<String>();
        for ( int i = 0; ( subDirs != null ) && ( i < subDirs.length ); i++ )
        {
            // skip entries not belonging to this project
            if ( !subDirs[i].startsWith( artifactIdVersion ) )
                continue;

            String classifier = subDirs[i].substring( artifactIdVersion.length() + 1 );

            // skip noarch here
            if ( classifier.equals( NarConstants.NAR_NO_ARCH ) )
                continue;
            
            classifiers.add(classifier);
        }
        
        if( !classifiers.isEmpty() ){
	
        	for(String classifier : classifiers ){
	            int lastDash = classifier.lastIndexOf( '-' );
	            String type = classifier.substring( lastDash + 1 );
	            AOL aol = new AOL( classifier.substring( 0, lastDash ) );

	            if ( ( narInfo.getOutput( aol, null ) == null ) )
	            {
	                narInfo.setOutput( aol, mojo.getOutput(! Library.EXECUTABLE.equals( type ) ) );
	            }

            	// We prefer shared to jni/executable/static/none, 
	            if ( type.equals( Library.SHARED ) )  // overwrite whatever we had
	            {
	                narInfo.setBinding( aol, type );
	                narInfo.setBinding( null, type );
	            }
	            else
	            {
	            	// if the binding is already set, then don't write it for jni/executable/static/none.
	                if ( ( narInfo.getBinding( aol, null ) == null ) )
	                {
	                    narInfo.setBinding( aol, type );
	                }
	                if ( ( narInfo.getBinding( null, null ) == null ) )
	                {
	                    narInfo.setBinding( null, type );
	                }
	            }
	
	            narInfo.setNar( null, type, project.getGroupId() + ":" + project.getArtifactId() + ":"
	                + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + type );
	            
	        }

        	// setting this first stops the per type config because getOutput check for aol defaults to this generic one...
            if ( mojo!= null && ( narInfo.getOutput( null, null ) == null ) )
            {
                narInfo.setOutput( null, mojo.getOutput(true) );
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
        if ( getNoArchDirectory( baseDir, project.getArtifactId(), project.getVersion() ).exists() )
        {
            attachNar( archiverManager, projectHelper, project, NarConstants.NAR_NO_ARCH,
                       getNoArchDirectory( baseDir, project.getArtifactId(), project.getVersion() ), "*/**" );
        }

        // list all directories in basedir, scan them for classifiers
        String[] subDirs = baseDir.list();
        for ( int i = 0; ( subDirs != null ) && ( i < subDirs.length ); i++ )
        {
            String artifactIdVersion = project.getArtifactId() + "-" + project.getVersion();

            // skip entries not belonging to this project
            if ( !subDirs[i].startsWith( artifactIdVersion ) )
                continue;

            String classifier = subDirs[i].substring( artifactIdVersion.length() + 1 );

            // skip noarch here
            if ( classifier.equals( NarConstants.NAR_NO_ARCH ) )
                continue;

            File dir = new File( baseDir, subDirs[i] );
            attachNar( archiverManager, projectHelper, project, classifier, dir, "*/**" );
        }
    }

    public void unpackNar( File unpackDirectory, ArchiverManager archiverManager, File file, String os, String linkerName,
                           AOL defaultAOL )
        throws MojoExecutionException, MojoFailureException
    {
        File dir = getNarUnpackDirectory(unpackDirectory, file);

        boolean process = false;

        if ( !unpackDirectory.exists() )
        {
            unpackDirectory.mkdirs();
            process = true;
        }
        else if ( !dir.exists() )
        {
            process = true;
        }
        else if ( file.lastModified() > dir.lastModified() )
        {
            try
            {
                FileUtils.deleteDirectory( dir );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not delete directory: " + dir, e );
            }

            process = true;
        }

        if ( process )
        {
            unpackNarAndProcess( archiverManager, file, dir, os, linkerName, defaultAOL );
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
