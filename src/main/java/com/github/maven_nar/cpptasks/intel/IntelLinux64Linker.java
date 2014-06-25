package com.github.maven_nar.cpptasks.intel;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractLdLinker;
import com.github.maven_nar.cpptasks.gcc.GccLibrarian;
/**
 * Adapter for the Intel (r) linker for Linux for IA-64
 * 
 * @author Curt Arnold
 */
public final class IntelLinux64Linker extends AbstractLdLinker {
    private static final String[] discardFiles = new String[0];
    private static final String[] libtoolObjFiles = new String[]{".fo", ".a",
            ".lib", ".dll", ".so", ".sl"};
    private static final String[] objFiles = new String[]{".o", ".a", ".lib",
            ".dll", ".so", ".sl"};
    private static final IntelLinux64Linker dllLinker = new IntelLinux64Linker(
            "lib", ".so", false, new IntelLinux64Linker("lib", ".so", true,
                    null));
    private static final IntelLinux64Linker instance = new IntelLinux64Linker(
            "", "", false, null);
    public static IntelLinux64Linker getInstance() {
        return instance;
    }
    private IntelLinux64Linker(String outputPrefix, String outputSuffix,
            boolean isLibtool, IntelLinux64Linker libtoolLinker) {
// FREEHEP
        super("ecpc", "-V", objFiles, discardFiles, outputPrefix, outputSuffix,
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
