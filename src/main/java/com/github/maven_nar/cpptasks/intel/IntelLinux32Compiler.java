// FREEHEP
package com.github.maven_nar.cpptasks.intel;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.gcc.GccCompatibleCCompiler;

public final class IntelLinux32Compiler extends GccCompatibleCCompiler {
    private static final IntelLinux32Compiler instance = new IntelLinux32Compiler(
            false, new IntelLinux32Compiler(true, null, false, null), false,
            null);
    public static IntelLinux32Compiler getInstance() {
        return instance;
    }
    private IntelLinux32Compiler(boolean isLibtool,
            IntelLinux32Compiler libtoolCompiler, boolean newEnvironment,
            Environment env) {
        super("icpc", "-V", isLibtool, libtoolCompiler, newEnvironment, env);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new IntelLinux32Compiler(getLibtool(),
                    (IntelLinux32Compiler) getLibtoolCompiler(),
                    newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        return IntelLinux32Linker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return Integer.MAX_VALUE;
    }
}
