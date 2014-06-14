package com.github.maven_nar;

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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractCompileMojo
    extends AbstractDependencyMojo
{

    /**
     * C++ Compiler
     */
    @Parameter
    private Cpp cpp;

    /**
     * C Compiler
     */
    @Parameter
    private C c;

    /**
     * Fortran Compiler
     */
    @Parameter
    private Fortran fortran;

    /**
     * Resource Compiler
     */
    @Parameter
    private Resource resource;

    /**
     * IDL Compiler
     */
    @Parameter
    private IDL idl;

    /**
     * Message Compiler
     */
    @Parameter
    private Message message;

    /**
     * By default NAR compile will attempt to compile using all known compilers against files in the directories specified by convention.
     * This allows configuration to a reduced set, you will have to specify each compiler to use in the configuration.
     */
    @Parameter(defaultValue = "false")
    protected boolean onlySpecifiedCompilers;

    /**
     * Do we log commands that is executed to produce the end-result?
     * Conception was to allow eclipse to sniff out include-paths from compile.
     */
    @Parameter
    protected int commandLogLevel=Project.MSG_VERBOSE;

    /**
     * Maximum number of Cores/CPU's to use. 0 means unlimited.
     */
    @Parameter
    private int maxCores = 0;


    /**
     * Fail on compilation/linking error.
     */
    @Parameter(defaultValue = "true", required = true)
    private boolean failOnError;

    /**
     * Sets the type of runtime library, possible values "dynamic", "static".
     */
    @Parameter(defaultValue = "dynamic", required = true)
    private String runtime;

    /**
     * Set use of libtool. If set to true, the "libtool " will be prepended to the command line for compatible
     * processors.
     */
    @Parameter(defaultValue = "false", required = true)
    private boolean libtool;

    /**
     * List of tests to create
     */
    @Parameter
    private List tests;

    /**
     * Java info for includes and linking
     */
    @Parameter
    private Java java;

    /**
     * Flag to cpptasks to indicate whether linker options should be decorated or not
     */
    @Parameter
    protected boolean decorateLinkerOptions;

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

    public void setCpp(Cpp cpp) {
        this.cpp = cpp;
        cpp.setAbstractCompileMojo( this );
    }

    public void setC(C c) {
        this.c = c;
        c.setAbstractCompileMojo( this );
    }

    public void setFortran(Fortran fortran) {
        this.fortran = fortran;
        fortran.setAbstractCompileMojo( this );
    }

    public void setResource(Resource resource) {
        this.resource = resource;
        resource.setAbstractCompileMojo( this );
    }

    public void setIdl(IDL idl) {
        this.idl = idl;
        idl.setAbstractCompileMojo( this );
    }
    
    public void setMessage(Message message) {
        this.message = message;
        message.setAbstractCompileMojo( this );
    }
    
    protected final C getC()
    {
        if ( c == null && !onlySpecifiedCompilers )
        {
            setC( new C() );
        }
        return c;
    }

    protected final Cpp getCpp()
    {
        if ( cpp == null && !onlySpecifiedCompilers )
        {
            setCpp( new Cpp() );
        }
        return cpp;
    }

    protected final Fortran getFortran()
    {
        if ( fortran == null && !onlySpecifiedCompilers )
        {
            setFortran( new Fortran() );
        }
        return fortran;
    }

    protected final Resource getResource( )
    {
		if ( resource == null && !onlySpecifiedCompilers )
        {
			setResource( new Resource() );
        }
        return resource;
    }
    
    protected final IDL getIdl( )
    {
		if ( idl == null && !onlySpecifiedCompilers )
        {
            setIdl( new IDL() );
        }
        return idl;
    }
    
    protected final Message getMessage( )
    {
		if ( message == null && !onlySpecifiedCompilers )
        {
			setMessage( new Message() );
        }
        return message;
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

    protected final String getOutput( AOL aol, String type )
        throws MojoExecutionException
    {
        return getNarInfo().getOutput( aol, getOutput( !Library.EXECUTABLE.equals( type ) ) );
    }

    protected final List getTests()
    {
        if ( tests == null )
        {
            tests = Collections.EMPTY_LIST;
        }
        return tests;
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

}
