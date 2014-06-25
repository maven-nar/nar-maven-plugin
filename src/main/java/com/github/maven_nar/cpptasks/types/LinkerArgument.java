package com.github.maven_nar.cpptasks.types;
/**
 * A linker command line argument.
 */
public class LinkerArgument extends CommandLineArgument {
    public LinkerArgument() {
    }
    public void execute() throws org.apache.tools.ant.BuildException {
        throw new org.apache.tools.ant.BuildException(
                "Not an actual task, but looks like one for documentation purposes");
    }
}
