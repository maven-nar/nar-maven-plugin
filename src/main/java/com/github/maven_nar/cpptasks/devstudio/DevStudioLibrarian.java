package com.github.maven_nar.cpptasks.devstudio;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
/**
 * Adapter for the Microsoft (r) Library Manager
 * 
 * @author Curt Arnold
 */
public final class DevStudioLibrarian extends DevStudioCompatibleLibrarian {
    private static final DevStudioLibrarian instance = new DevStudioLibrarian();
    public static DevStudioLibrarian getInstance() {
        return instance;
    }
    private DevStudioLibrarian() {
        super("lib", "/bogus");
    }
    public Linker getLinker(LinkType type) {
        return DevStudioLinker.getInstance().getLinker(type);
    }
}
