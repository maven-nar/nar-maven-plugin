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
 * Runs make on the GNU style generated Makefile
 * 
 * @goal nar-gnu-make
 * @phase compile
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarGnuMakeMojo
    extends AbstractGnuMojo
{
    /**
     * Space delimited list of arguments to pass to make
     *
     * @parameter default-value=""
     */
    private String gnuMakeArgs;

    /**
     * Comma delimited list of environment variables to setup before running make
     *
     * @parameter default-value=""
     */
    private String gnuMakeEnv;

    /**
     * Boolean to control if we should skip 'make install' after the make
     *
     * @parameter default-value="false"
     */
    private boolean gnuMakeInstallSkip;

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !useGnu() )
        {
            return;
        }

        File srcDir = getGnuAOLSourceDirectory();
        if ( srcDir.exists() )
        {
            String[] args= null;
            String[] env= null;

            if ( gnuMakeArgs != null )
            {
               args= gnuMakeArgs.split( " " );
            }
            if ( gnuMakeEnv != null )
            {
               env= gnuMakeEnv.split( "," );
            }

            getLog().info( "Running GNU make" );
            int result = NarUtil.runCommand( "make", args, srcDir, env, getLog() );
            if ( result != 0 )
            {
                throw new MojoExecutionException( "'make' errorcode: " + result );
            }

            if ( !gnuMakeInstallSkip )
            {
               getLog().info( "Running make install" );
               if ( args != null )
               {
                  gnuMakeArgs= gnuMakeArgs + " install";
                  args= gnuMakeArgs.split( " " );
               }
               else
               {
                  args= new String[] { "install" };
               }
               result = NarUtil.runCommand( "make", args, srcDir, null, getLog() );
               if ( result != 0 )
               {
                   throw new MojoExecutionException( "'make install' errorcode: " + result );
               }
            }
        }
    }
}
