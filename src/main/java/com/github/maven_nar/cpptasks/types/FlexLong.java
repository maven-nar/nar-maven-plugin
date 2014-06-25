package com.github.maven_nar.cpptasks.types;
import java.lang.reflect.Method;

/**
 * Helper class which can be used for Ant task attribute setter methods to
 * allow the build file to specify a long in either decimal, octal, or
 * hexadecimal format.
 *  // FlexInteger author
 * @author Erik Hatcher
 * @see org.apache.tools.ant.types.FlexInteger
 */
public class FlexLong {
    private Long value;
    /**
     * Constructor used by Ant's introspection mechanism for attribute
     * population
     */
    public FlexLong(String value) {
        // Java 1.1 did not support Long.decode().. so we call it by
        // reflection.
        try {
            Method m = Long.class
                    .getMethod("decode", new Class[]{String.class});
            Object rc = m.invoke(null, new Object[]{value});
            this.value = (Long) rc;
        } catch (Exception e) {
            // Try it the old fashioned way, we must be on a 1.1 jre
            this.value = new Long(value);
        }
    }
    /**
     * Returns the decimal integer value
     */
    public long longValue() {
        return value.longValue();
    }
    /**
     * Overridden method to return the decimal value for display
     */
    public String toString() {
        return value.toString();
    }
}
