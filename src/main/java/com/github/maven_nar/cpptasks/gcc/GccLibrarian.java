package com.github.maven_nar.cpptasks.gcc;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
/**
 * Adapter for the 'ar' archiver
 * 
 * @author Adam Murdoch
 */
public final class GccLibrarian extends AbstractArLibrarian {
    private static String[] objFileExtensions = new String[]{".o"};
    private static GccLibrarian instance = new GccLibrarian("ar",
            objFileExtensions, false, new GccLibrarian("ar", objFileExtensions,
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
