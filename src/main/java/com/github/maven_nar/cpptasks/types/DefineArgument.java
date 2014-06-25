package com.github.maven_nar.cpptasks.types;
/**
 * Preprocessor macro definition.
 * 
 * @author Mark A Russell <a
 *         href="mailto:mark_russell@csgsystems.com">mark_russell@csg_systems.com
 *         </a>
 */
public class DefineArgument extends UndefineArgument {
    private String value;
    public DefineArgument() {
        super(true);
    }
    /** Returns the value of the define */
    public final String getValue() {
        return value;
    }
    /** Set the value attribute */
    public final void setValue(String value) {
        this.value = value;
    }
}
