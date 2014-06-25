package com.github.maven_nar.cpptasks;
import org.apache.tools.ant.types.EnumeratedAttribute;
/**
 * Enumeration of supported subsystems
 * 
 * @author Curt Arnold
 *  
 */
public class OutputTypeEnum extends EnumeratedAttribute {
    /**
     * Constructor
     * 
     * Set by default to "executable"
     * 
     * @see java.lang.Object#Object()
     */
    public OutputTypeEnum() {
        setValue("executable");
    }
    /**
     * Gets list of acceptable values
     * 
     * @see org.apache.tools.ant.types.EnumeratedAttribute#getValues()
     */
    public String[] getValues() {
        return new String[]{"executable", // executable program
                "plugin", // plugin module
                "shared", // dynamically linkable module
                "static",
// FREEHEP
                "jni"	  // jni module
        };
    }
}
