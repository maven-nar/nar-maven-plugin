package com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2;
import java.io.File;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractLdLinker;

/**
 * Adapter for the 'ld' linker
 * 
 * @author Curt Arnold
 */
public final class LdLinker extends AbstractLdLinker {
    private static final String[] discardFiles = new String[0];
    private static final String[] libtoolObjFiles = new String[]{".fo", ".a",
            ".lib", ".dll", ".so", ".sl"};
    private static final String[] objFiles = new String[]{".o", ".a", ".lib",
            ".dll", ".so", ".sl"};
    private static final LdLinker dllLinker = new LdLinker(
            GccCCompiler.CMD_PREFIX + "ld", objFiles, discardFiles, "lib",
            ".so", false, new LdLinker(GccCCompiler.CMD_PREFIX + "ld",
                    objFiles, discardFiles, "lib", ".so", true, null));
    private static final LdLinker instance = new LdLinker(
            GccCCompiler.CMD_PREFIX + "ld", objFiles, discardFiles, "", "",
            false, null);
    public static LdLinker getInstance() {
        return instance;
    }
    private File[] libDirs;
    private LdLinker(String command, String[] extensions,
            String[] ignoredExtensions, String outputPrefix,
            String outputSuffix, boolean isLibtool, LdLinker libtoolLinker) {
        super(command, "-version", extensions, ignoredExtensions, outputPrefix,
                outputSuffix, isLibtool, libtoolLinker);
    }
    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return GccLibrarian.getInstance();
        }
        if (type.isSharedLibrary()) {
            return dllLinker;
        }
        return instance;
    }
}
