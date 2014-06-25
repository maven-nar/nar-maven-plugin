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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * Jar up the NAR files and attach them to the projects main artifact (for installation and deployment).
 * 
 * @goal nar-package
 * @phase package
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarPackageMojo
    extends AbstractNarMojo
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


    // TODO: this is working of what is present rather than what was requested to be built, POM ~/= artifacts!
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // let the layout decide which nars to attach
        getLayout().attachNars( getTargetDirectory(), archiverManager, projectHelper, getMavenProject() );
     
    }
}
