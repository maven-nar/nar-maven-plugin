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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

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
     * To look up Archiver/UnArchiver implementations
     * 
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;
    
    /**
     * Used for attaching the artifact in the project
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // let the layout decide which nars to attach
        getLayout().attachNars( getTargetDirectory(), archiverManager, projectHelper, getMavenProject(), getNarInfo() );
        
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
}
