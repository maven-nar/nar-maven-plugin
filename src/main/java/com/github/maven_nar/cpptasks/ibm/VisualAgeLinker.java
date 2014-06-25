package com.github.maven_nar.cpptasks.ibm;
import java.util.Vector;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractLdLinker;
import com.github.maven_nar.cpptasks.gcc.GccLibrarian;

/**
 * Adapter for IBM(r) Visual Age(tm) Linker for AIX(tm)
 * 
 * @author Curt Arnold
 */
public final class VisualAgeLinker extends AbstractLdLinker {
    private static final String[] discardFiles = new String[]{};
    private static final String[] objFiles = new String[]{".o", ".a", ".lib",
            ".dll", ".so", ".sl"};
    private static final VisualAgeLinker dllLinker = new VisualAgeLinker(
            "xlC", objFiles, discardFiles, "lib", ".a");
    private static final VisualAgeLinker instance = new VisualAgeLinker("xlC",
            objFiles, discardFiles, "", "");
    public static VisualAgeLinker getInstance() {
        return instance;
    }
    private VisualAgeLinker(String command, String[] extensions,
            String[] ignoredExtensions, String outputPrefix, String outputSuffix) {
        //
        //  just guessing that -? might display something useful
        //
        super(command, "-?", extensions, ignoredExtensions, outputPrefix,
                outputSuffix, false, null);
    }
    public void addImpliedArgs(boolean debug, LinkType linkType, Vector args) {
        if (debug) {
            //args.addElement("-g");
        }
        if (linkType.isSharedLibrary()) {
            args.addElement("-qmkshrobj");
        }
    }
    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return GccLibrarian.getInstance();
        }
        if (type.isSharedLibrary()) {
            return dllLinker;
        }
        return instance;
    }
    /**
     * Gets identifier for the compiler.
     * 
     * Initial attempt at extracting version information
     * would lock up.  Using a stock response.
     */
    public String getIdentifier() {
        return "VisualAge linker - unidentified version";
    }

    protected String getDynamicLibFlag() {
        return "-bdynamic";  
    }

    protected String getStaticLibFlag() {
        return "-bstatic";
    }
    
}
