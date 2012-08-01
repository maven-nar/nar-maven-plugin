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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Jars up the NAR files.
 * 
 * @goal nar-prepare-package
 * @phase prepare-package
 * @requiresProject
 * @author GDomjan
 */
public class NarPreparePackageMojo
    extends AbstractNarMojo
{    

    /**
     * @parameter expression="${project.build.directory}/classes"
     * @readonly
     */
    private File outputDirectory;
    
    // TODO: this is working of what is present rather than what was requested to be built, POM ~/= artifacts!
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // let the layout decide which (additional) nars to attach
        getLayout().prepareNarInfo( getTargetDirectory(), getMavenProject(), getNarInfo(), this );

        try
        {
        	// TODO: this structure seems overly deep it already gets unpacked to own folder - classes/
            File propertiesDir =
                new File( outputDirectory, "META-INF/nar/" + getMavenProject().getGroupId() + "/"
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
