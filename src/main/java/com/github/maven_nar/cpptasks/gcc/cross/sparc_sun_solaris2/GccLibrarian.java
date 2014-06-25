package com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractArLibrarian;
/**
 * Adapter for the 'ar' archiver
 * 
 * @author Adam Murdoch
 */
public final class GccLibrarian extends AbstractArLibrarian {
    private static String[] objFileExtensions = new String[]{".o"};
    private static GccLibrarian instance = new GccLibrarian(
            GccCCompiler.CMD_PREFIX + "ar", objFileExtensions, false,
            new GccLibrarian(GccCCompiler.CMD_PREFIX + "ar", objFileExtensions,
                    true, null));
    public static GccLibrarian getInstance() {
        return instance;
    }
    private GccLibrarian(String command, String[] inputExtensions,
            boolean isLibtool, GccLibrarian libtoolLibrarian) {
        super(command, "V", inputExtensions, new String[0], "lib", ".a",
                isLibtool, libtoolLibrarian);
    }
    public Linker getLinker(LinkType type) {
        return GccLinker.getInstance().getLinker(type);
    }
}
