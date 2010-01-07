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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Validates the configuration of the NAR project (aol and pom)
 * 
 * @goal nar-validate
 * @phase validate
 * @author Mark Donszelmann
 */
public class NarValidateMojo
    extends AbstractCompileMojo
{
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // check aol
        AOL aol = getAOL();
        getLog().info( "Using AOL: " + aol );

        // check linker exists in retrieving the version number
        Linker linker = getLinker();
        getLog().debug( "Using linker version: " + linker.getVersion() );

        // check compilers
        int noOfCompilers = 0;
        Compiler cpp = getCpp();
        if ( cpp.getName() != null )
        {
            noOfCompilers++;
            // need includes
            if ( cpp.getIncludes( Compiler.MAIN ).isEmpty() )
            {
                throw new MojoExecutionException( "No includes defined for compiler " + cpp.getName() );
            }
        }
        Compiler c = getC();
        if ( c.getName() != null )
        {
            noOfCompilers++;
            // need includes
            if ( c.getIncludes( Compiler.MAIN ).isEmpty() )
            {
                throw new MojoExecutionException( "No includes defined for compiler " + c.getName() );
            }
        }
        Compiler fortran = getFortran();
        if ( fortran.getName() != null )
        {
            noOfCompilers++;
            // need includes
            if ( fortran.getIncludes( Compiler.MAIN ).isEmpty() )
            {
                throw new MojoExecutionException( "No includes defined for compiler " + fortran.getName() );
            }
        }

        // at least one compiler has to be defined
        if ( noOfCompilers == 0 )
        {
            throw new MojoExecutionException( "No compilers defined for linker " + linker.getName() );
        }
    }
}
