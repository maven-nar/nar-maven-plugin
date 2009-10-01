// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Sets up a library to create
 * 
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/Library.java 19804ec9b6b9 2007/09/04 23:36:51 duns $
 */
public class Library implements Executable {

	public static final String STATIC = "static";
	public static final String SHARED = "shared";
	public static final String EXECUTABLE = "executable";
	public static final String JNI = "jni";
	public static final String PLUGIN = "plugin";
	public static final String NONE = "none";			// no library produced

	/**
	 * Type of the library to generate. Possible choices are: "plugin",
	 * "shared", "static", "jni" or "executable". Defaults to "shared".
	 * 
	 * @parameter expression=""
	 */
	protected String type = "shared";

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
	 * If specified will create the NarSystem class with methods
	 * to load a JNI library.
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
	protected File narSystemDirectory = new File("target/nar/nar-generated");
	
	/**
     * When true and if type is "executable" run this executable.
     * Defaults to false;
   	 * 
	 * @parameter expression=""
	 */
	protected boolean run=false;
	
	/**
	 * Arguments to be used for running this executable.
	 * Defaults to empty list. This option is 
	 * only used if run=true and type=executable.
	 * 
	 * @parameter expression=""
	 */
    protected List/*<String>*/ args = new ArrayList();

	public String getType() {
		return type;
	}

	public boolean linkCPP() {
		return linkCPP;
	}
	
	public boolean linkFortran() {
		return linkFortran;
	}
	
	public String getNarSystemPackage() {
		return narSystemPackage;
	}
	
	public boolean shouldRun() {
		return run;
	}
	
    public List/*<String>*/ getArgs() {
    	return args;
    }

	public String getNarSystemName() {
		return narSystemName;
	}

	public File getNarSystemDirectory() {
		return narSystemDirectory;
	}	
}
