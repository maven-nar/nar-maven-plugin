package com.github.maven_nar.cpptasks.intel;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.devstudio.DevStudioCompatibleLinker;
/**
 * Adapter for the Intel (r) linker for 32-bit applications
 * 
 * @author Curt Arnold
 */
public final class IntelWin32Linker extends DevStudioCompatibleLinker {
    private static final IntelWin32Linker dllLinker = new IntelWin32Linker(
            ".dll");
    private static final IntelWin32Linker instance = new IntelWin32Linker(
            ".exe");
    public static IntelWin32Linker getInstance() {
        return instance;
    }
    private IntelWin32Linker(String outputSuffix) {
        super("xilink", "-qv", outputSuffix);
    }
    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return IntelWin32Librarian.getInstance();
        }
        if (type.isSharedLibrary()) {
            return dllLinker;
        }
        return instance;
    }
}
