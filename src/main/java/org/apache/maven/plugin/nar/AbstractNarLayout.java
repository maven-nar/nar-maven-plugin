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

package org.apache.maven.plugin.nar;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 * @version $Id$
 */
public abstract class AbstractNarLayout
    implements NarLayout, NarConstants
{

    protected final void attachNar( ArchiverManager archiverManager, MavenProjectHelper projectHelper, MavenProject project,
                                    String classifier, File dir, String include )
        throws MojoExecutionException
    {
        File narFile =
            new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + "-" + classifier + "."
                + NarConstants.NAR_EXTENSION );
        if ( narFile.exists() )
        {
            narFile.delete();
        }
        try
        {
            Archiver archiver = archiverManager.getArchiver( NarConstants.NAR_ROLE_HINT );
            archiver.addDirectory( dir, new String[] { include }, null );
            archiver.setDestFile( narFile );
            archiver.createArchive();
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "NAR: cannot find archiver", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "NAR: cannot create NAR archive '" + narFile + "'", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NAR: cannot create NAR archive '" + narFile + "'", e );
        }
        projectHelper.attachArtifact( project, NarConstants.NAR_TYPE, classifier, narFile );
    }
}
