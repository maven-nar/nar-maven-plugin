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
            getLog().info( "Running GNU make" );
            int result = NarUtil.runCommand( "make", null, srcDir, null, getLog() );
            if ( result != 0 )
            {
                throw new MojoExecutionException( "'make' errorcode: " + result );
            }

            result = NarUtil.runCommand( "make", new String[] { "install" }, srcDir, null, getLog() );
            if ( result != 0 )
            {
                throw new MojoExecutionException( "'make install' errorcode: " + result );
            }
        }
    }
}
