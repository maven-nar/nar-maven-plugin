package com.github.maven_nar.cpptasks.types;
/**
 * A set of system library names. Timestamp or location of system libraries are
 * not considered in dependency analysis.
 * 
 * Libraries can also be added to a link by specifying them in a fileset.
 * 
 * For most Unix-like compilers, syslibset will result in a series of -l and -L
 * linker arguments. For Windows compilers, the library names will be used to
 * locate the appropriate library files which will be added to the linkers
 * input file list as if they had been specified in a fileset.
 */
public class SystemLibrarySet extends LibrarySet {
    public SystemLibrarySet() {
        super();
    }
    public void execute() throws org.apache.tools.ant.BuildException {
        throw new org.apache.tools.ant.BuildException(
                "Not an actual task, but looks like one for documentation purposes");
    }
}
