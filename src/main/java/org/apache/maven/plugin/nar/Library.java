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

import java.util.ArrayList;
import java.util.List;

/**
 * Sets up a library to create
 * 
 * @author Mark Donszelmann
 */
public class Library
    implements Executable
{

    public static final String STATIC = "static";

    public static final String SHARED = "shared";

    public static final String EXECUTABLE = "executable";

    public static final String JNI = "jni";

    public static final String PLUGIN = "plugin";

    public static final String NONE = "none"; // no library produced

    /**
     * Type of the library to generate. Possible choices are: "plugin", "shared", "static", "jni" or "executable".
     * Defaults to "shared".
     * 
     * @parameter expression=""
     */
    protected String type = SHARED;

    /**
     * Link with stdcpp if necessary Defaults to true.
     * 
     * @parameter expression=""
     */
    protected boolean linkCPP = true;

    /**
     * Link with fortran runtime if necessary Defaults to false.
     * 
     * @parameter expression=""
     */
    protected boolean linkFortran = false;

    /**
     * If specified will create the NarSystem class with methods to load a JNI library.
     * 
     * @parameter expression=""
     */
    protected String narSystemPackage = null;

    /**
     * Name of the NarSystem class
     * 
     * @parameter expression="NarSystem"
     * @required
     */
    protected String narSystemName = "NarSystem";

    /**
     * The target directory into which to generate the output.
     * 
     * @parameter expression="${project.build.dir}/nar/nar-generated"
     * @required
     */
    protected String narSystemDirectory = "target/nar/nar-generated";

    /**
     * When true and if type is "executable" run this executable. Defaults to false;
     * 
     * @parameter expression=""
     */
    protected boolean run = false;

    /**
     * Arguments to be used for running this executable. Defaults to empty list. This option is only used if run=true
     * and type=executable.
     * 
     * @parameter expression=""
     */
    protected List/* <String> */args = new ArrayList();

    public String getType()
    {
        return type;
    }

    public boolean linkCPP()
    {
        return linkCPP;
    }

    public boolean linkFortran()
    {
        return linkFortran;
    }

    public String getNarSystemPackage()
    {
        return narSystemPackage;
    }

    public boolean shouldRun()
    {
        return run;
    }

    public List/* <String> */getArgs()
    {
        return args;
    }

    public String getNarSystemName()
    {
        return narSystemName;
    }

    public String getNarSystemDirectory()
    {
        return narSystemDirectory;
    }

    // FIXME incomplete
    public String toString()
    {
        StringBuffer sb = new StringBuffer( "Library: " );
        sb.append( "type: " );
        sb.append( getType() );
        return sb.toString();
    }
}
