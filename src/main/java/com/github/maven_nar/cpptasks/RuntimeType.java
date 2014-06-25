package com.github.maven_nar.cpptasks;
import org.apache.tools.ant.types.EnumeratedAttribute;
/**
 * Enumerated attribute with the values "dynamic" and "static",
 */
public class RuntimeType extends EnumeratedAttribute {
    public String[] getValues() {
        return new String[]{"dynamic", "static"};
    }
}
