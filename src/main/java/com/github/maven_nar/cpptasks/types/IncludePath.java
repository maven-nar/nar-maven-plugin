package com.github.maven_nar.cpptasks.types;
import org.apache.tools.ant.Project;
/**
 * An include path.
 * 
 * Works like other paths in Ant with with the addition of "if" and "unless"
 * conditions.
 * 
 * @author Curt Arnold
 */
public class IncludePath extends ConditionalPath {
    public IncludePath(Project project) {
        super(project);
    }
    public IncludePath(Project p, String path) {
        super(p, path);
    }
    public void execute() throws org.apache.tools.ant.BuildException {
        throw new org.apache.tools.ant.BuildException(
                "Not an actual task, but looks like one for documentation purposes");
    }
}
