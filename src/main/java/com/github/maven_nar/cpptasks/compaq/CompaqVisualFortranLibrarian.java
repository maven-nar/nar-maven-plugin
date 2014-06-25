package com.github.maven_nar.cpptasks.compaq;
import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.devstudio.DevStudioLibrarian;
import com.github.maven_nar.cpptasks.devstudio.DevStudioProcessor;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

import java.io.File;
import java.util.Vector;

/**
 * Adapter for the Compaq(r) Visual Fortran Librarian
 * 
 * @author Curt Arnold
 */
public class CompaqVisualFortranLibrarian extends CommandLineLinker {
    private static final CompaqVisualFortranLibrarian instance = new CompaqVisualFortranLibrarian();
    public static CompaqVisualFortranLibrarian getInstance() {
        return instance;
    }
    private CompaqVisualFortranLibrarian() {
        super("lib", "/bogus", new String[]{".obj"}, new String[0], ".lib",
                false, null);
    }

    protected void addImpliedArgs(CCTask task, boolean debug, LinkType linkType, Vector args) {
        args.addElement("/nologo");
    }
    protected String getCommandFileSwitch(String commandFile) {
        return DevStudioProcessor.getCommandFileSwitch(commandFile);
    }
    public File[] getLibraryPath() {
        return new File[0];
    }
    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        return new String[0];
    }
    public Linker getLinker(LinkType type) {
        return CompaqVisualFortranLinker.getInstance().getLinker(type);
    }
    protected int getMaximumCommandLength() {
        return DevStudioLibrarian.getInstance().getMaximumCommandLength();
    }
    protected String[] getOutputFileSwitch(String outputFile) {
        return DevStudioLibrarian.getInstance().getOutputFileSwitch(outputFile);
    }
    public boolean isCaseSensitive() {
        return false;
    }
}
