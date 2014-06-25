package com.github.maven_nar.cpptasks.compaq;
import java.util.Vector;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.devstudio.DevStudioCompatibleLinker;

/**
 * Adapter for the Compaq(r) Visual Fortran linker.
 * 
 * @author Curt Arnold
 */
public final class CompaqVisualFortranLinker extends DevStudioCompatibleLinker {
    private static final CompaqVisualFortranLinker dllLinker = new CompaqVisualFortranLinker(
            ".dll");
    private static final CompaqVisualFortranLinker instance = new CompaqVisualFortranLinker(
            ".exe");
    public static CompaqVisualFortranLinker getInstance() {
        return instance;
    }
    private CompaqVisualFortranLinker(String outputSuffix) {
        super("DF", "__bogus__.xxx", outputSuffix);
    }
    protected void addImpliedArgs(boolean debug, LinkType linkType, Vector args) {
        args.addElement("/NOLOGO");
        boolean staticRuntime = linkType.isStaticRuntime();
        if (staticRuntime) {
            args.addElement("/libs:static");
        } else {
            args.addElement("/libs:dll");
        }
        if (debug) {
            args.addElement("/debug");
        } else {
        }
        if (linkType.isSharedLibrary()) {
            args.addElement("/dll");
        } else {
            args.addElement("/exe");
        }
    }
    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return CompaqVisualFortranLibrarian.getInstance();
        }
        if (type.isSharedLibrary()) {
            return dllLinker;
        }
        return instance;
    }
    public String[] getOutputFileSwitch(String outputFile) {
        StringBuffer buf = new StringBuffer("/OUT:");
        if (outputFile.indexOf(' ') >= 0) {
            buf.append('"');
            buf.append(outputFile);
            buf.append('"');
        } else {
            buf.append(outputFile);
        }
        return new String[]{buf.toString()};
    }
}
