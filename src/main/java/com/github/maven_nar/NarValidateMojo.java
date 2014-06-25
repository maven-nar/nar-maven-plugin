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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.List;

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
    /**
     * Source directory for GNU style project
     * 
     * @parameter default-value="${basedir}/src/gnu"
     * @required
     */
    private File gnuSourceDirectory;
    
	@Override
	protected List/*<Artifact>*/ getArtifacts() {
		return null;//getMavenProject().getCompileArtifacts();  // Artifact.SCOPE_COMPILE 
	}

	public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
//    	super.narExecute();

    	// check aol
        AOL aol = getAOL();
        getLog().info( "Using AOL: " + aol );

        // check linker exists in retrieving the version number
        Linker linker = getLinker();
        getLog().debug( "Using linker version: " + linker.getVersion() );

        // check compilers
        int noOfCompilers = 0;
        if( onlySpecifiedCompilers ) {
	        if ( getCpp() != null && getCpp().getName() != null )
	        {
	            noOfCompilers++;
	            // need includes
	            if ( getCpp().getIncludes( Compiler.MAIN ).isEmpty() )
	            {
	                throw new MojoExecutionException( "No includes defined for compiler " + getCpp().getName() );
	            }
	        }
	        
	        if ( getC() != null && getC().getName() != null )
	        {
	            noOfCompilers++;
	            // need includes
	            if ( getC().getIncludes( Compiler.MAIN ).isEmpty() )
	            {
	                throw new MojoExecutionException( "No includes defined for compiler " + getC().getName() );
	            }
	        }        
	        
	        if ( getFortran() != null && getFortran().getName() != null )
	        {
	            noOfCompilers++;
	            // need includes
	            if ( getFortran().getIncludes( Compiler.MAIN ).isEmpty() )
	            {
	                throw new MojoExecutionException( "No includes defined for compiler " + getFortran().getName() );
	            }
	        }
	        
	        // at least one compiler has to be defined
	        // OR
	        // a <gnuSourceDirectory> is configured.
	        if ( noOfCompilers == 0 && ( gnuSourceDirectory == null || !gnuSourceDirectory.exists() ) )
	        {
	            throw new MojoExecutionException( "No compilers defined for linker " + linker.getName() + ", and no" +
	                    " <gnuSourceDirectory> is defined.  Either define a compiler or a linker." );     
	        }
        } 
    }
}
