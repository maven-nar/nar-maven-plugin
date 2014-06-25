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
import java.util.Iterator;
import java.util.List;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.types.CommandLineArgument;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LinkerArgument;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Java specifications for NAR
 * 
 * @author Mark Donszelmann
 */
public class Java
{

    /**
     * Add Java includes to includepath
     * 
     * @parameter default-value="false"
     * @required
     */
    private boolean include = false;

    /**
     * Java Include Paths, relative to a derived ${java.home}. Defaults to: "${java.home}/include" and
     * "${java.home}/include/<i>os-specific</i>".
     * 
     * @parameter default-value=""
     */
    private List includePaths;

    /**
     * Add Java Runtime to linker
     * 
     * @parameter default-value="false"
     * @required
     */
    private boolean link = false;

    /**
     * Relative path from derived ${java.home} to the java runtime to link with Defaults to Architecture-OS-Linker
     * specific value. FIXME table missing
     * 
     * @parameter default-value=""
     */
    private String runtimeDirectory;

    /**
     * Name of the runtime
     * 
     * @parameter default-value="jvm"
     */
    private String runtime = "jvm";

    private AbstractCompileMojo mojo;

    public Java()
    {
    }

    public final void setAbstractCompileMojo( AbstractCompileMojo mojo )
    {
        this.mojo = mojo;
    }

    public final void addIncludePaths( CCTask task, String outType )
        throws MojoFailureException, MojoExecutionException
    {
        if ( include || mojo.getJavah().getJniDirectory().exists() )
        {
            if ( includePaths != null )
            {
                for ( Iterator i = includePaths.iterator(); i.hasNext(); )
                {
                    String path = (String) i.next();
                    task.createIncludePath().setPath( new File( mojo.getJavaHome( mojo.getAOL() ), path ).getPath() );
                }
            }
            else
            {
                String prefix = mojo.getAOL().getKey() + ".java.";
                String includes = NarProperties.getInstance(mojo.getMavenProject()).getProperty( prefix + "include" );
                if ( includes != null )
                {
                    String[] path = includes.split( ";" );
                    for ( int i = 0; i < path.length; i++ )
                    {
                        task.createIncludePath().setPath(
                                                          new File( mojo.getJavaHome( mojo.getAOL() ), path[i] ).getPath() );
                    }
                }
            }
        }
    }

    public final void addRuntime( CCTask task, File javaHome, String os, String prefix )
        throws MojoFailureException
    {
        if ( link )
        {
            if ( os.equals( OS.MACOSX ) )
            {
                CommandLineArgument.LocationEnum end = new CommandLineArgument.LocationEnum();
                end.setValue( "end" );

                // add as argument rather than library to avoid argument quoting
                LinkerArgument framework = new LinkerArgument();
                framework.setValue( "-framework" );
                framework.setLocation( end );
                task.addConfiguredLinkerArg( framework );

                LinkerArgument javavm = new LinkerArgument();
                javavm.setValue( "JavaVM" );
                javavm.setLocation( end );
                task.addConfiguredLinkerArg( javavm );
            }
            else
            {
                if ( runtimeDirectory == null )
                {
                    runtimeDirectory = NarProperties.getInstance(mojo.getMavenProject()).getProperty( prefix + "runtimeDirectory" );
                    if ( runtimeDirectory == null )
                    {
                        throw new MojoFailureException( "NAR: Please specify a <RuntimeDirectory> as part of <Java>" );
                    }
                }
                mojo.getLog().debug( "Using Java Runtime Directory: " + runtimeDirectory );

                LibrarySet libset = new LibrarySet();
                libset.setProject( mojo.getAntProject() );
                libset.setLibs( new CUtil.StringArrayBuilder( runtime ) );
                libset.setDir( new File( javaHome, runtimeDirectory ) );
                task.addLibset( libset );
            }
        }
    }
}
