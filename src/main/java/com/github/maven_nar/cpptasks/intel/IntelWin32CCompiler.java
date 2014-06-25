package com.github.maven_nar.cpptasks.intel;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.devstudio.DevStudioCompatibleCCompiler;
/**
 * Adapter for the Intel (r) C++ compiler for 32-bit applications
 * 
 * The Intel (r) C++ compiler for IA32 Windows mimics the command options for
 * the Microsoft (r) C++ compiler.
 * 
 * @author Curt Arnold
 */
public final class IntelWin32CCompiler extends DevStudioCompatibleCCompiler {
    private static final IntelWin32CCompiler instance = new IntelWin32CCompiler(
            false, null);
    public static IntelWin32CCompiler getInstance() {
        return instance;
    }
    private IntelWin32CCompiler(boolean newEnvironment, Environment env) {
        super("icl", "-help", newEnvironment, env);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new IntelWin32CCompiler(newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        return IntelWin32Linker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return 32767;
    }
}
