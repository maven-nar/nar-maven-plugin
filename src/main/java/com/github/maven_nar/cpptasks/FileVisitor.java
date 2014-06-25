package com.github.maven_nar.cpptasks;
import java.io.File;

import org.apache.tools.ant.BuildException;
/**
 * An abstract class implemented to walk over the fileset members of a
 * ProcessorDef
 */
public interface FileVisitor {
    abstract void visit(File parentDir, String filename) throws BuildException;
}
