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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Assemble libraries of NAR files.
 * 
 * @goal nar-assembly
 * @phase process-resources
 * @requiresProject
 * @requiresDependencyResolution
 * @author Mark Donszelmann
 */
public class NarAssemblyMojo
    extends AbstractDependencyMojo
{

    /**
     * List of classifiers which you want to assemble. Example ppc-MacOSX-g++-static, x86-Windows-msvc-shared,
     * i386-Linux-g++-executable, .... not setting means all.
     * 
     * @parameter
     */
    private String[] classifiers = null;

    /**
     * Copies the unpacked nar libraries and files into the projects target area
     */
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        List narArtifacts = getNarManager().getNarDependencies( "compile" );

        List dependencies = getNarManager().getAttachedNarDependencies( narArtifacts, classifiers );

        // this may make some extra copies...
        for ( Iterator d = dependencies.iterator(); d.hasNext(); )
        {
            Artifact dependency = (Artifact) d.next();
            getLog().debug( "Assemble from " + dependency );

            // FIXME reported to maven developer list, isSnapshot
            // changes behaviour
            // of getBaseVersion, called in pathOf.
            dependency.isSnapshot();

            File srcDir =
                getLayout().getNarUnpackDirectory( getUnpackDirectory(), getNarManager().getNarFile( dependency ) );
            // File srcDir = new File( getLocalRepository().pathOf( dependency ) );
            // srcDir = new File( getLocalRepository().getBasedir(), srcDir.getParent() );
            // srcDir = new File( srcDir, "nar/" );

            File dstDir = getTargetDirectory();
            try
            {
                FileUtils.mkdir( dstDir.getPath() );
                getLog().debug( "SrcDir: " + srcDir );
                if ( srcDir.exists() )
                {
                    FileUtils.copyDirectoryStructure( srcDir, dstDir );
                }
            }
            catch ( IOException ioe )
            {
                throw new MojoExecutionException( "Failed to copy directory for dependency " + dependency + " from "
                    + srcDir + " to " + dstDir, ioe );
            }
        }
    }
}
