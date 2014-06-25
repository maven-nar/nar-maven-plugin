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
 * @requiresDependencyResolution compile
 * @author Mark Donszelmann
 */
public class NarAssemblyMojo
    extends AbstractDependencyMojo
{
	@Override
	protected List/*<Artifact>*/ getArtifacts() {
		return getMavenProject().getCompileArtifacts();  // Artifact.SCOPE_COMPILE 
	}

    /**
     * Copies the unpacked nar libraries and files into the projects target area
     */
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
//TODO: old allowed for classifiers
    	List<NarArtifact> narArtifacts = getNarArtifacts( );
		List<AttachedNarArtifact> dependencies = getAllAttachedNarArtifacts( narArtifacts/*, library*/ );
		downloadAttachedNars( dependencies );
		unpackAttachedNars( dependencies );

//        List dependencies = getNarManager().getAttachedNarDependencies( narArtifacts, classifiers.toArray(new String[0] ) );

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
                    FileUtils.copyDirectoryStructureIfModified( srcDir, dstDir );
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
