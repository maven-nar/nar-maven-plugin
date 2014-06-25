package com.github.maven_nar.cpptasks.arm;

import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

import java.io.File;

/**
 * Adapter for ARM Librarian
 *
 * @author Curt Arnold
 */
public class ADSLibrarian extends CommandLineLinker {

    private static final ADSLibrarian instance = new ADSLibrarian();

    public static ADSLibrarian getInstance() {
      return instance;
    }

    private ADSLibrarian()
    {
        super("armar",null,
          new String[] { ".o" }, new String[0], ".lib", false, null);
    }

    protected String getCommandFileSwitch(String commandFile) {
        // TODO Auto-generated method stub
        return null;
    }

    public File[] getLibraryPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        return new String[0];
    }

    public Linker getLinker(LinkType linkType) {
        // TODO Auto-generated method stub
        return null;
    }

    protected int getMaximumCommandLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    protected String[] getOutputFileSwitch(String outputFile) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCaseSensitive() {
        // TODO Auto-generated method stub
        return false;
    }

}
