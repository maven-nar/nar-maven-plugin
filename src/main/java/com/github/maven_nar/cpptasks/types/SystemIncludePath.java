package com.github.maven_nar.cpptasks.types;
import org.apache.tools.ant.Project;
/**
 * A system include path.
 * 
 * Files located using a system include path will not participate in dependency
 * analysis.
 * 
 * Standard include paths for a compiler should not be specified since these
 * should be determined from environment variables or configuration files by
 * the compiler adapter.
 * 
 * Works like other paths in Ant with with the addition of "if" and "unless"
 * conditions.
 * 
 * @author Curt Arnold
 */
public class SystemIncludePath extends ConditionalPath {
    public SystemIncludePath(Project project) {
        super(project);
    }
    public SystemIncludePath(Project p, String path) {
        super(p, path);
    }
    public void execute() throws org.apache.tools.ant.BuildException {
        throw new org.apache.tools.ant.BuildException(
                "Not an actual task, but looks like one for documentation purposes");
    }
}
