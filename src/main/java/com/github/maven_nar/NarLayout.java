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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Interface to define the layout of nar files (executables, libs, include dirs) in both the repository (local,
 * unpacked) as well as in target.
 * 
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public interface NarLayout
{
    /**
     * Specifies where all the "no architecture" specific files are stored
     */
    File getNoArchDirectory( File baseDir, String artifactId, String version )
        throws MojoExecutionException, MojoFailureException;

    /**
     * Specifies where libraries are stored
     * 
     * @return
     * @throws MojoExecutionException, MojoFailureException
     */
    File getLibDirectory( File baseDir, String artifactId, String version, String aol, String type )
        throws MojoExecutionException, MojoFailureException;

    /**
     * Specifies where includes are stored
     * 
     * @return
     */
    File getIncludeDirectory( File baseDir, String artifactId, String version )
        throws MojoExecutionException, MojoFailureException;

    /**
     * Specifies where binaries are stored
     * 
     * @return
     */
    File getBinDirectory( File baseDir, String artifactId, String version, String aol )
        throws MojoExecutionException, MojoFailureException;

    /**
     * Called to attach nars to main nar/jar file. This method needs to set NarInfo accordingly so it can be included in the nar archive.
     */
    void prepareNarInfo( File baseDir, MavenProject project, NarInfo narInfo, AbstractNarMojo libraryName )
        throws MojoExecutionException;

    /**
     * Called to attach nars to main nar/jar file. This method needs to produce all the attached nar archive files.
     */
    void attachNars( File baseDir, ArchiverManager archiverManager, MavenProjectHelper projectHelper,
                     MavenProject project)
        throws MojoExecutionException, MojoFailureException;

    /**
     * Called to unpack a nar file
     * @param defaultAOL 
     * @param linkerName 
     */
    void unpackNar( File baseDir, ArchiverManager archiverManager, File file, String os, String linkerName, AOL defaultAOL )
        throws MojoExecutionException, MojoFailureException;

    /**
     * Returns the unpack directory of a specific nar file.
     */
    File getNarUnpackDirectory(File baseUnpackDirectory, File narFile);

}
