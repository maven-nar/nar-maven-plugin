package com.github.maven_nar.cpptasks.types;
import org.apache.tools.ant.types.EnumeratedAttribute;
/**
 * Enumeration of library types for LibrarySet
 * 
 * @author Curt Arnold
 *  
 */
public class LibraryTypeEnum extends EnumeratedAttribute {
    /**
     * Constructor
     * 
     * Set by default to "shared"
     * 
     * @see java.lang.Object#Object()
     */
    public LibraryTypeEnum() {
        setValue("shared");
    }
    /**
     * Gets list of acceptable values
     * 
     * @see org.apache.tools.ant.types.EnumeratedAttribute#getValues()
     */
    public String[] getValues() {
        return new String[]{"shared", // prefer shared libraries
                "static", // prefer static libraries
                "framework" // framework libraries (Mac OS/X)
				            //  equiv to shared on other platforms
        };
    }
}
