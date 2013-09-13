// FREEHEP
package com.github.maven_nar.cpptasks.intel;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractLdLinker;
import com.github.maven_nar.cpptasks.gcc.GccLibrarian;
public final class IntelLinux32CLinker extends AbstractLdLinker {
    private static final String[] discardFiles = new String[0];
    private static final String[] libtoolObjFiles = new String[]{".fo", ".a",
            ".lib", ".dll", ".so", ".sl"};
    private static final String[] objFiles = new String[]{".o", ".a", ".lib",
            ".dll", ".so", ".sl"};
    private static final IntelLinux32CLinker dllLinker = new IntelLinux32CLinker(
            "lib", ".so", false, new IntelLinux32CLinker("lib", ".so", true,
                    null));
    private static final IntelLinux32CLinker instance = new IntelLinux32CLinker(
            "", "", false, null);
    public static IntelLinux32CLinker getInstance() {
        return instance;
    }
    private IntelLinux32CLinker(String outputPrefix, String outputSuffix,
            boolean isLibtool, IntelLinux32CLinker libtoolLinker) {
        super("icc", "-V", objFiles, discardFiles, outputPrefix, outputSuffix,
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
