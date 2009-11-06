/*
 * 
 * Copyright 2001-2007 The Ant-Contrib project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.antcontrib.cpptasks.sun;
import java.io.File;
import java.util.Vector;

import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.gcc.GccCompatibleCCompiler;
import net.sf.antcontrib.cpptasks.OptimizationEnum;
/**
 * Adapter for the Sun (r) Forte (tm) C++ compiler
 * 
 * @author Curt Arnold
 */
public final class ForteCCCompiler extends GccCompatibleCCompiler {
    private final static String[] headerExtensions = new String[]{".h", ".hpp",
            ".inl"};
    private final static String[] sourceExtensions = new String[]{".c", ".cc",
            ".cxx", ".cpp", ".c++", ".i", ".s"};
    
    private static final ForteCCCompiler instance = new ForteCCCompiler("CC", 
            sourceExtensions, headerExtensions);
    /**
     * Gets singleton instance of this class
     */
    public static ForteCCCompiler getInstance() {
        return instance;
    }
    private String identifier;
    private File[] includePath;
    /**
     * Private constructor. Use ForteCCCompiler.getInstance() to get singleton
     * instance of this class.
     */
    private ForteCCCompiler(String command, String[] sourceExtensions, 
            String[] headerExtensions) {
        super(command, "-V", sourceExtensions, headerExtensions, false, null, 
                false, null);
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
    	if (rtti != null) {
    		if (rtti.booleanValue()) {
    			args.addElement("-features=rtti");
    		} else {
    			args.addElement("-features=no%rtti");
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
                break;
            case 3 :
                args.addElement("+w");
                break;
            case 4 :
                args.addElement("+w2");
                break;
            case 5 :
                args.addElement("+w2");
                args.addElement("-xwe");
        }
    }
    public File[] getEnvironmentIncludePath() {
        if (includePath == null) {
            File ccLoc = CUtil.getExecutableLocation("CC");
            if (ccLoc != null) {
                File compilerIncludeDir = new File(
                        new File(ccLoc, "../include").getAbsolutePath());
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
