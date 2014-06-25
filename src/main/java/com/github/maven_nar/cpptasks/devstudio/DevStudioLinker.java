package com.github.maven_nar.cpptasks.devstudio;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
/**
 * Adapter for the Microsoft (r) Incremental Linker
 * 
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public final class DevStudioLinker extends DevStudioCompatibleLinker {
    private static final DevStudioLinker dllLinker = new DevStudioLinker(".dll");
    private static final DevStudioLinker instance = new DevStudioLinker(".exe");
    public static DevStudioLinker getInstance() {
        return instance;
    }
    private DevStudioLinker(String outputSuffix) {
        super("link", "/DLL", outputSuffix);
    }
    public Linker getLinker(LinkType type) {
        if (type.isSharedLibrary()) {
            return dllLinker;
        }
        if (type.isStaticLibrary()) {
            return DevStudioLibrarian.getInstance();
        }
        return instance;
    }
}
