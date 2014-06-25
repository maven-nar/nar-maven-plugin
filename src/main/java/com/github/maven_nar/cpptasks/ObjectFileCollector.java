package com.github.maven_nar.cpptasks;
import java.io.File;
import java.util.Vector;


import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.compiler.Linker;
/**
 * Collects object files for the link step.
 * 
 *  
 */
public final class ObjectFileCollector implements FileVisitor {
    private final Vector files;
    private final Linker linker;
    public ObjectFileCollector(Linker linker, Vector files) {
        this.linker = linker;
        this.files = files;
    }
    public void visit(File parentDir, String filename) throws BuildException {
        int bid = linker.bid(filename);
        if (bid >= 1) {
            files.addElement(new File(parentDir, filename));
        }
    }
}
