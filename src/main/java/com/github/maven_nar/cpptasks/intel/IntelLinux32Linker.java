package com.github.maven_nar.cpptasks.intel;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractLdLinker;
import com.github.maven_nar.cpptasks.gcc.GccLibrarian;
/**
 * Adapter for the Intel (r) Linker for Linux (r) for IA-32
 * 
 * @author Curt Arnold
 */
public final class IntelLinux32Linker extends AbstractLdLinker {
    private static final String[] discardFiles = new String[0];
    private static final String[] libtoolObjFiles = new String[]{".fo", ".a",
            ".lib", ".dll", ".so", ".sl"};
    private static final String[] objFiles = new String[]{".o", ".a", ".lib",
            ".dll", ".so", ".sl"};
    private static final IntelLinux32Linker dllLinker = new IntelLinux32Linker(
            "lib", ".so", false, new IntelLinux32Linker("lib", ".so", true,
                    null));
    private static final IntelLinux32Linker instance = new IntelLinux32Linker(
            "", "", false, null);
    public static IntelLinux32Linker getInstance() {
        return instance;
    }
    private IntelLinux32Linker(String outputPrefix, String outputSuffix,
            boolean isLibtool, IntelLinux32Linker libtoolLinker) {
// FREEHEP
        super("icpc", "-V", objFiles, discardFiles, outputPrefix, outputSuffix,
                isLibtool, libtoolLinker);
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
