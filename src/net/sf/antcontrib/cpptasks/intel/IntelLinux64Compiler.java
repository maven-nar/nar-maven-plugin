// FREEHEP
package net.sf.antcontrib.cpptasks.intel;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.compiler.Processor;
import net.sf.antcontrib.cpptasks.gcc.GccCompatibleCCompiler;

import org.apache.tools.ant.types.Environment;

public final class IntelLinux64Compiler extends GccCompatibleCCompiler {
    private static final IntelLinux64Compiler instance = new IntelLinux64Compiler(
            false, new IntelLinux64Compiler(true, null, false, null), false,
            null);
    public static IntelLinux64Compiler getInstance() {
        return instance;
    }
    private IntelLinux64Compiler(boolean isLibtool,
            IntelLinux64Compiler libtoolCompiler, boolean newEnvironment,
            Environment env) {
        super("ecpc", "-V", isLibtool, libtoolCompiler, newEnvironment, env);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new IntelLinux64Compiler(getLibtool(),
                    (IntelLinux64Compiler) this.getLibtoolCompiler(),
                    newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        return IntelLinux64Linker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return Integer.MAX_VALUE;
    }
}
