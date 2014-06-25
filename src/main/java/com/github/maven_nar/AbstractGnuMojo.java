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

/**
 * Abstract GNU Mojo keeps configuration
 * 
 * @author Mark Donszelmann
 */
public abstract class AbstractGnuMojo
    extends AbstractResourcesMojo
{
    /**
     * Use GNU goals on Windows
     * 
     * @parameter expresssion="nar.gnu.useonwindows" default-value="false"
     * @required
     */
    private boolean gnuUseOnWindows;
    
    /**
     * Source directory for GNU style project
     * 
     * @parameter default-value="${basedir}/src/gnu"
     * @required
     */
    private File gnuSourceDirectory;

    /**
     * Directory in which gnu sources are copied and "configured"
     * 
     * @parameter default-value="${project.build.directory}/nar/gnu"
     * @required
     */
    private File gnuTargetDirectory;

    /**
     * @return
     * @throws MojoFailureException
     * @throws MojoExecutionException 
     */
    protected final File getGnuAOLSourceDirectory()
        throws MojoFailureException, MojoExecutionException
    {
        return new File( getGnuAOLDirectory(), "src" );
    }

    /**
     * @return
     * @throws MojoFailureException
     * @throws MojoExecutionException 
     */
    protected final File getGnuAOLTargetDirectory()
        throws MojoFailureException, MojoExecutionException
    {
        return new File( getGnuAOLDirectory(), "target" );
    }
    
    protected final File getGnuSourceDirectory() {
        return gnuSourceDirectory;
    }

    /**
     * @return
     * @throws MojoFailureException
     * @throws MojoExecutionException 
     */
    private File getGnuAOLDirectory()
        throws MojoFailureException, MojoExecutionException
    {
        return new File( gnuTargetDirectory, getAOL().toString() );
    }
    
    /**
     * Returns true if we do not want to use GNU on Windows
     * 
     * @return
     */
    protected final boolean useGnu() {
        return gnuUseOnWindows || !OS.WINDOWS.equals(NarUtil.getOS( null ));
    }
}
