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
import org.codehaus.plexus.util.FileUtils;

/**
 * Copies the GNU style source files to a target area, autogens and configures them.
 * 
 * @goal nar-gnu-configure
 * @phase process-sources
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarGnuConfigureMojo
    extends AbstractGnuMojo
{
    /**
     * Skip running of autogen.sh.
     * 
     * @parameter expression="${nar.gnu.autogen.skip}" default-value="false"
     */
    private boolean gnuAutogenSkip;

    /**
     * Skip running of configure and therefore also autogen.sh
     * 
     * @parameter expression="${nar.gnu.configure.skip}" default-value="false"
     */
    private boolean gnuConfigureSkip;

    private static final String AUTOGEN = "autogen.sh";

    private static final String CONFIGURE = "configure";

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( shouldSkip() )
        {
            return;
        }

        if ( !useGnu() )
        {
            return;
        }

        File targetDir = getGnuAOLSourceDirectory();
        if ( getGnuSourceDirectory().exists() )
        {
            getLog().info( "Copying GNU sources" );

            try
            {
                FileUtils.mkdir( targetDir.getPath() );
                NarUtil.copyDirectoryStructure( getGnuSourceDirectory(), targetDir, null, null );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy GNU sources", e );
            }

            File autogen = new File( targetDir, AUTOGEN );
            if ( !gnuConfigureSkip && !gnuAutogenSkip && autogen.exists() )
            {
                getLog().info( "Running GNU " + AUTOGEN );
                
                // fix missing config directory
                File configDir = new File(targetDir, "config");
                if (!configDir.exists()) {
                    configDir.mkdirs();
                }
                
                NarUtil.makeExecutable( autogen, getLog() );
                int result = NarUtil.runCommand( "./" + autogen.getName(), null, targetDir, null, getLog() );
                if ( result != 0 )
                {
                    throw new MojoExecutionException( "'" + AUTOGEN + "' errorcode: " + result );
                }
            }

            File configure = new File( targetDir, CONFIGURE );
            if ( !gnuConfigureSkip && configure.exists() )
            {
                getLog().info( "Running GNU " + CONFIGURE );
                NarUtil.makeExecutable( configure, getLog() );
                int result =
                    NarUtil.runCommand( "./" + configure.getName(), new String[] { "--disable-ccache",
                        "--prefix=" + getGnuAOLTargetDirectory().getAbsolutePath() }, targetDir, null, getLog() );
                if ( result != 0 )
                {
                    throw new MojoExecutionException( "'" + CONFIGURE + "' errorcode: " + result );
                }
            }
        }
    }
}
