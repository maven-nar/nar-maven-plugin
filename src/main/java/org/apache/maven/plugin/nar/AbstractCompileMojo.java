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
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractCompileMojo
    extends AbstractDependencyMojo
{

    /**
     * C++ Compiler
     * 
     * @parameter expression=""
     */
    private Cpp cpp;

    /**
     * C Compiler
     * 
     * @parameter expression=""
     */
    private C c;

    /**
     * Fortran Compiler
     * 
     * @parameter expression=""
     */
    private Fortran fortran;

    /**
     * Maximum number of Cores/CPU's to use. 0 means unlimited.
     * 
     * @parameter expression=""
     */
    private int maxCores = 0;

    /**
     * Name of the output
     * 
     * @parameter expression="${project.artifactId}-${project.version}"
     */
    private String output;

    /**
     * Fail on compilation/linking error.
     * 
     * @parameter expression="" default-value="true"
     * @required
     */
    private boolean failOnError;

    /**
     * Sets the type of runtime library, possible values "dynamic", "static".
     * 
     * @parameter expression="" default-value="dynamic"
     * @required
     */
    private String runtime;

    /**
     * Set use of libtool. If set to true, the "libtool " will be prepended to the command line for compatible
     * processors.
     * 
     * @parameter expression="" default-value="false"
     * @required
     */
    private boolean libtool;

    /**
     * The home of the Java system. Defaults to a derived value from ${java.home} which is OS specific.
     * 
     * @parameter expression=""
     * @readonly
     */
    private File javaHome;

    /**
     * List of libraries to create
     * 
     * @parameter expression=""
     */
    private List libraries;

    /**
     * List of tests to create
     * 
     * @parameter expression=""
     */
    private List tests;

    /**
     * Javah info
     * 
     * @parameter expression=""
     */
    private Javah javah;

    /**
     * Java info for includes and linking
     * 
     * @parameter expression=""
     */
    private Java java;

    private NarInfo narInfo;

    private List/* <String> */dependencyLibOrder;

    private Project antProject;

    protected final Project getAntProject()
    {
        if ( antProject == null )
        {
            // configure ant project
            antProject = new Project();
            antProject.setName( "NARProject" );
            antProject.addBuildListener( new NarLogger( getLog() ) );
        }
        return antProject;
    }

    protected final C getC()
    {
        if ( c == null )
        {
            c = new C();
        }
        c.setAbstractCompileMojo( this );
        return c;
    }

    protected final Cpp getCpp()
    {
        if ( cpp == null )
        {
            cpp = new Cpp();
        }
        cpp.setAbstractCompileMojo( this );
        return cpp;
    }

    protected final Fortran getFortran()
    {
        if ( fortran == null )
        {
            fortran = new Fortran();
        }
        fortran.setAbstractCompileMojo( this );
        return fortran;
    }

    protected final int getMaxCores( AOL aol )
        throws MojoExecutionException
    {
        return getNarInfo().getProperty( aol, "maxCores", maxCores );
    }

    protected final boolean useLibtool( AOL aol )
        throws MojoExecutionException
    {
        return getNarInfo().getProperty( aol, "libtool", libtool );
    }

    protected final boolean failOnError( AOL aol )
        throws MojoExecutionException
    {
        return getNarInfo().getProperty( aol, "failOnError", failOnError );
    }

    protected final String getRuntime( AOL aol )
        throws MojoExecutionException
    {
        return getNarInfo().getProperty( aol, "runtime", runtime );
    }

    protected final String getOutput( AOL aol )
        throws MojoExecutionException
    {
        return getNarInfo().getProperty( aol, "output", output );
    }

    protected final File getJavaHome( AOL aol )
        throws MojoExecutionException
    {
        // FIXME should be easier by specifying default...
        return getNarInfo().getProperty( aol, "javaHome", NarUtil.getJavaHome( javaHome, getOS() ) );
    }

    protected final List getLibraries()
    {
        if ( libraries == null )
        {
            libraries = Collections.EMPTY_LIST;
        }
        return libraries;
    }

    protected final List getTests()
    {
        if ( tests == null )
        {
            tests = Collections.EMPTY_LIST;
        }
        return tests;
    }

    protected final Javah getJavah()
    {
        if ( javah == null )
        {
            javah = new Javah();
        }
        javah.setAbstractCompileMojo( this );
        return javah;
    }

    protected final Java getJava()
    {
        if ( java == null )
        {
            java = new Java();
        }
        java.setAbstractCompileMojo( this );
        return java;
    }

    public final void setDependencyLibOrder( List/* <String> */order )
    {
        dependencyLibOrder = order;
    }

    protected final List/* <String> */getDependencyLibOrder()
    {
        return dependencyLibOrder;
    }

    protected final NarInfo getNarInfo()
        throws MojoExecutionException
    {
        if ( narInfo == null )
        {
        	String groupId = getMavenProject().getGroupId();
        	String artifactId = getMavenProject().getArtifactId();
        	
            File propertiesDir = new File( getMavenProject().getBasedir(), "src/main/resources/META-INF/nar/" + groupId + "/" + artifactId );
            File propertiesFile = new File( propertiesDir, NarInfo.NAR_PROPERTIES );

            narInfo = new NarInfo( 
                groupId, artifactId,
                getMavenProject().getVersion(), 
                getLog(),
                propertiesFile );
        }
        return narInfo;
    }
}
