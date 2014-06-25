package com.github.maven_nar.cpptasks;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Enumerated attribute with the values "none", "severe", "default",
 * "production", "diagnostic", and "aserror".
 */
public final class WarningLevelEnum extends EnumeratedAttribute {
   /**
    * Constructor.
    *
    */
    public WarningLevelEnum() {
        setValue("default");
    }
    /**
     * Get allowable values.
     * @return allowable values
     */
    public String[] getValues() {
        return new String[]{"none", "severe", "default", "production",
              "diagnostic", "aserror"};
    }
}
