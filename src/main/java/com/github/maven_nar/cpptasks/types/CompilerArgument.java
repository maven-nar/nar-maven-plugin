package com.github.maven_nar.cpptasks.types;
/**
 * A compiler command line argument.
 */
public class CompilerArgument extends CommandLineArgument {
    public CompilerArgument() {
    }
    public void execute() throws org.apache.tools.ant.BuildException {
        throw new org.apache.tools.ant.BuildException(
                "Not an actual task, but looks like one for documentation purposes");
    }
}
