package com.github.maven_nar;

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
     * @parameter default-value=""
     */
    private String type = SHARED;
    
    /**
     * Type of subsystem to generate: "gui", "console", "other". Defaults to "console".
     *
     * @parameter default-value=""
     */
    private String subSystem = "console";
    
    /**
     * Link with stdcpp if necessary Defaults to true.
     * 
     * @parameter default-value=""
     */
    private boolean linkCPP = true;

    /**
     * Link with fortran runtime if necessary Defaults to false.
     * 
     * @parameter default-value=""
     */
    private boolean linkFortran = false;

    /**
     * Link with fortran startup, so that the gcc linker can find the "main" of fortran. Defaults to false.
     * 
     * @parameter default-value=""
     */
    private boolean linkFortranMain = false;

    /**
     * If specified will create the NarSystem class with methods to load a JNI library.
     * 
     * @parameter default-value=""
     */
    private String narSystemPackage = null;

    /**
     * Name of the NarSystem class
     * 
     * @parameter default-value="NarSystem"
     * @required
     */
    private String narSystemName = "NarSystem";

    /**
     * The target directory into which to generate the output.
     * 
     * @parameter default-value="${project.build.dir}/nar/nar-generated"
     * @required
     */
    private String narSystemDirectory = "nar-generated";

    /**
     * When true and if type is "executable" run this executable. Defaults to false;
     * 
     * @parameter default-value=""
     */
    private boolean run = false;

    /**
     * Arguments to be used for running this executable. Defaults to empty list. This option is only used if run=true
     * and type=executable.
     * 
     * @parameter default-value=""
     */
    private List/* <String> */args = new ArrayList();

    public final String getType()
    {
        return type;
    }

    public final boolean linkCPP()
    {
        return linkCPP;
    }

    public final boolean linkFortran()
    {
        return linkFortran;
    }

    public final boolean linkFortranMain()
    {
        return linkFortranMain;
    }

    public final String getNarSystemPackage()
    {
        return narSystemPackage;
    }

    public final boolean shouldRun()
    {
        return run;
    }

    public final List/* <String> */getArgs()
    {
        return args;
    }

    public final String getNarSystemName()
    {
        return narSystemName;
    }

    public final String getNarSystemDirectory()
    {
        return narSystemDirectory;
    }

    // FIXME incomplete
    public final String toString()
    {
        StringBuffer sb = new StringBuffer( "Library: " );
        sb.append( "type: " );
        sb.append( getType() );
        return sb.toString();
    }

    public String getSubSystem()
    {
        return subSystem;
    }
}
