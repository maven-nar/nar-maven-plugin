package com.github.maven_nar.cpptasks.devstudio;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
/**
 * Adapter for the Microsoft(r) C/C++ Optimizing Compiler
 * 
 * @author Adam Murdoch
 */
public final class DevStudioCCompiler extends DevStudioCompatibleCCompiler {
    private static final DevStudioCCompiler instance = new DevStudioCCompiler(
            "cl", false, null);
    public static DevStudioCCompiler getInstance() {
        return instance;
    }
    private DevStudioCCompiler(String command, boolean newEnvironment,
            Environment env) {
        super(command, "/bogus", newEnvironment, env);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new DevStudioCCompiler(getCommand(), newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        return DevStudioLinker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
// FREEHEP stay on safe side
        return 32000; // 32767;
    }
}
