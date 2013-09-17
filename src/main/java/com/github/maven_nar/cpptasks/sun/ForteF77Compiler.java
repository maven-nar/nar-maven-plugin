/* FREEHEP
 */
package com.github.maven_nar.cpptasks.sun;
import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.GccCompatibleCCompiler;

/**
 * Adapter for the Sun (r) Forte (tm) F77 compiler
 * 
 * @author Mark Donszelmann
 */
public final class ForteF77Compiler extends GccCompatibleCCompiler {
    private static final ForteF77Compiler instance = new ForteF77Compiler("f77");
    /**
     * Gets singleton instance of this class
     */
    public static ForteF77Compiler getInstance() {
        return instance;
    }
    private String identifier;
    private File[] includePath;
    /**
     * Private constructor. Use ForteF77Compiler.getInstance() to get singleton
     * instance of this class.
     */
    private ForteF77Compiler(String command) {
        super(command, "-V", false, null, false, null);
    }
    public void addImpliedArgs(final Vector args, 
    		final boolean debug,
            final boolean multithreaded, 
			final boolean exceptions, 
			final LinkType linkType,
			final Boolean rtti,
			final OptimizationEnum optimization) {
        args.addElement("-c");
        if (debug) {
            args.addElement("-g");
        }
    	if (optimization != null) {
    		if (optimization.isSpeed()) {
    			args.addElement("-xO2");
    		}
    	}
        if (multithreaded) {
            args.addElement("-mt");
        }
        if (linkType.isSharedLibrary()) {
            args.addElement("-KPIC");
        }
        
    }
    public void addWarningSwitch(Vector args, int level) {
        switch (level) {
            case 0 :
                args.addElement("-w");
                break;
            case 1 :
            case 2 :
                args.addElement("+w");
                break;
            case 3 :
            case 4 :
            case 5 :
                args.addElement("+w2");
                break;
        }
    }
    public File[] getEnvironmentIncludePath() {
        if (includePath == null) {
            File f77Loc = CUtil.getExecutableLocation("f77");
            if (f77Loc != null) {
                File compilerIncludeDir = new File(
                        new File(f77Loc, "../include").getAbsolutePath());
                if (compilerIncludeDir.exists()) {
                    includePath = new File[2];
                    includePath[0] = compilerIncludeDir;
                }
            }
            if (includePath == null) {
                includePath = new File[1];
            }
            includePath[includePath.length - 1] = new File("/usr/include");
        }
        return includePath;
    }
    public Linker getLinker(LinkType linkType) {
        return ForteCCLinker.getInstance().getLinker(linkType);
    }
    public int getMaximumCommandLength() {
        return Integer.MAX_VALUE;
    }
}
