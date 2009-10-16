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
    
    private static String autogen = "autogen.sh";
    private static String configure = "configure";
    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( shouldSkip() )
            return;

        if ( gnuSourceDirectory.exists() )
        {
            getLog().info( "Copying GNU sources" );

            File targetDir = getGnuAOLSourceDirectory();
            try
            {
                FileUtils.mkdir( targetDir.getPath() );
                NarUtil.copyDirectoryStructure( gnuSourceDirectory, targetDir, null, null );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy GNU sources", e );
            }

            if ((!gnuAutogenSkip) && new File(targetDir, "autogen.sh").exists()) {
                getLog().info( "Running GNU "+autogen );
                NarUtil.runCommand( "sh ./"+autogen, null, targetDir, null, getLog() );
            }

            getLog().info( "Running GNU "+configure );
            NarUtil.runCommand( "sh ./"+configure, new String[] { "--disable-ccache", "--prefix="
                + getGnuAOLTargetDirectory().getAbsolutePath() }, targetDir, null, getLog() );
        }
    }
}
