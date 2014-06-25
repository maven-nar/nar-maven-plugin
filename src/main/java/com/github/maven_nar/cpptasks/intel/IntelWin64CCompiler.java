package com.github.maven_nar.cpptasks.intel;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.devstudio.DevStudioCompatibleCCompiler;
/**
 * Adapter for the Intel C++ compiler for Itanium(TM) Applications
 * 
 * @author Curt Arnold
 */
public final class IntelWin64CCompiler extends DevStudioCompatibleCCompiler {
    private static final IntelWin64CCompiler instance = new IntelWin64CCompiler(
            false, null);
    public static IntelWin64CCompiler getInstance() {
        return instance;
    }
    private IntelWin64CCompiler(boolean newEnvironment, Environment env) {
        super("ecl", "-help", newEnvironment, env);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new IntelWin64CCompiler(newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        //
        //   currently the Intel Win32 and Win64 linkers
        //      are command line equivalent
        return IntelWin32Linker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return 32767;
    }
}
