package com.github.maven_nar.cpptasks;
import org.apache.tools.ant.types.EnumeratedAttribute;
/**
 * Enumeration of supported subsystems
 * 
 * @author Curt Arnold
 *  
 */
public final class SubsystemEnum extends EnumeratedAttribute {
    private final static String[] values = new String[]{"gui", "console",
            "other"};
    public SubsystemEnum() {
        setValue("gui");
    }
    public String[] getValues() {
        return (String[]) values.clone();
    }
}
