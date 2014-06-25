/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.gcc;
import java.io.File;
import java.util.Vector;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.FortranParser;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the GCC C/C++ compiler
 * 
 * @author Adam Murdoch
 */
public final class GccCCompiler extends GccCompatibleCCompiler {
    private final static String[] sourceExtensions = new String[]{".c", /* C */
            ".cc", /* C++ */
            ".cpp", /* C++ */
            ".cxx", /* C++ */
            ".c++", /* C++ */
            ".i", /* preprocessed C */
            ".ii", /* preprocessed C++ */
            ".f", /* FORTRAN */
            ".for", /* FORTRAN */
            ".f90", /* FORTRAN 90 */
            ".m", /* Objective-C */
            ".mm", /* Objected-C++ */
            ".s" /* Assembly */
            };
    private final static String[] headerExtensions = new String[]{".h", ".hpp",
    ".inl"};
    private static final GccCCompiler cppInstance = new GccCCompiler("c++",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("c++", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    private static final GccCCompiler g77Instance = new GccCCompiler("g77",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("g77", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
// FREEHEP
    private static final GccCCompiler gfortranInstance = new GccCCompiler("gfortran",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("gfortran", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    private static final GccCCompiler gppInstance = new GccCCompiler("g++",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("g++", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    private static final GccCCompiler instance = new GccCCompiler("gcc",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("gcc", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    private static final GccCCompiler clangInstance = new GccCCompiler("clang",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("clang", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    private static final GccCCompiler cpplangInstance = new GccCCompiler("clang++",
            sourceExtensions, headerExtensions, false,
            new GccCCompiler("clang++", sourceExtensions, headerExtensions, true,
                    null, false, null), false, null);
    /**
     * Gets c++ adapter
     */
    public static GccCCompiler getCppInstance() {
        return cppInstance;
    }
    /**
     * Gets g77 adapter
     */
    public static GccCCompiler getG77Instance() {
        return g77Instance;
    }
    /**
     * Gets g77 adapter
     */
// FREEHEPBEGIN
    public static GccCCompiler getGFortranInstance() {
        return gfortranInstance;
    }
// FREEHEPEND
    /**
     * Gets gpp adapter
     */
    public static GccCCompiler getGppInstance() {
        return gppInstance;
    }
    /**
     * Gets gcc adapter
     */
    public static GccCCompiler getInstance() {
        return instance;
    }
    /**
     * Gets clang adapter
     */
    public static GccCCompiler getCLangInstance() {
        return clangInstance;
    }
    /**
     * Gets clang++ adapter
     */
    public static GccCCompiler getCLangppInstance() {
        return cpplangInstance;
    }
    private String identifier;
    private File[] includePath;
    private boolean isPICMeaningful = true;
    /**
     * Private constructor. Use GccCCompiler.getInstance() to get singleton
     * instance of this class.
     */
    private GccCCompiler(String command, String[] sourceExtensions,
            String[] headerExtensions, boolean isLibtool,
            GccCCompiler libtoolCompiler, boolean newEnvironment,
            Environment env) {
        super(command, null, sourceExtensions, headerExtensions, isLibtool,
                libtoolCompiler, newEnvironment, env);
        isPICMeaningful = System.getProperty("os.name").indexOf("Windows") < 0;
    }
    public void addImpliedArgs(final Vector args, 
    		final boolean debug,
            final boolean multithreaded, 
			final boolean exceptions, 
			final LinkType linkType,
			final Boolean rtti,
			final OptimizationEnum optimization) {
        super.addImpliedArgs(args, debug, multithreaded, 
        		exceptions, linkType, rtti, optimization);
        if (isPICMeaningful && linkType.isSharedLibrary()) {
            args.addElement("-fPIC");
        }
// BEGINFREEHEP
        // Add -fno_rtti only for g++ and c++
        if (!getCommand().equals("g77") && !getCommand().equals("gcc") && (rtti != null) && (!rtti.booleanValue())) {
            // Darren Sargent: fix RTTI option
            args.addElement("-fno-rtti");
        }
// ENDFREEHEP
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new GccCCompiler(getCommand(), this.getSourceExtensions(),
                    this.getHeaderExtensions(), this.getLibtool(),
                    (GccCCompiler) this.getLibtoolCompiler(), newEnvironment,
                    env);
        }
        return this;
    }
    /**
     * Create parser to determine dependencies.
     * 
     * Will create appropriate parser (C++, FORTRAN) based on file extension.
     *  
     */
    protected Parser createParser(File source) {
        if (source != null) {
            String sourceName = source.getName();
            int lastDot = sourceName.lastIndexOf('.');
            if (lastDot >= 0 && lastDot + 1 < sourceName.length()) {
                char afterDot = sourceName.charAt(lastDot + 1);
                if (afterDot == 'f' || afterDot == 'F') {
                    return new FortranParser();
                }
            }
        }
        return new CParser();
    }
    public File[] getEnvironmentIncludePath() {
        if (includePath == null) {
            //
            //   construct default include path from machine id and version id
            //
            String[] defaultInclude = new String[1];
            StringBuffer buf = new StringBuffer("/lib/");
            buf.append(GccProcessor.getMachine());
            buf.append('/');
            buf.append(GccProcessor.getVersion());
            buf.append("/include");
            defaultInclude[0] = buf.toString();
            //
            //   read specs file and look for -istart and -idirafter
            //
            String[] specs = GccProcessor.getSpecs();
            String[][] optionValues = GccProcessor.parseSpecs(specs, "*cpp:",
                    new String[]{"-isystem ", "-idirafter "});
            //
            //   if no entries were found, then use a default path
            //
            if (optionValues[0].length == 0 && optionValues[1].length == 0) {
                optionValues[0] = new String[]{"/usr/local/include",
                        "/usr/include", "/usr/include/win32api"};
            }
            //
            //  remove mingw entries.
            //    For MinGW compiles this will mean the
            //      location of the sys includes will be
            //      wrong in dependencies.xml
            //      but that should have no significant effect
            for (int i = 0; i < optionValues.length; i++) {
                for (int j = 0; j < optionValues[i].length; j++) {
                    if (optionValues[i][j].indexOf("mingw") > 0) {
                        optionValues[i][j] = null;
                    }
                }
            }
            //
            //   if cygwin then
            //     we have to prepend location of gcc32
            //       and .. to start of absolute filenames to
            //       have something that will exist in the
            //       windows filesystem
            if (GccProcessor.isCygwin()) {
                GccProcessor.convertCygwinFilenames(optionValues[0]);
                GccProcessor.convertCygwinFilenames(optionValues[1]);
                GccProcessor.convertCygwinFilenames(defaultInclude);
            }
            int count = CUtil.checkDirectoryArray(optionValues[0]);
            count += CUtil.checkDirectoryArray(optionValues[1]);
            count += CUtil.checkDirectoryArray(defaultInclude);
            includePath = new File[count];
            int index = 0;
            for (int i = 0; i < optionValues.length; i++) {
                for (int j = 0; j < optionValues[i].length; j++) {
                    if (optionValues[i][j] != null) {
                        includePath[index++] = new File(optionValues[i][j]);
                    }
                }
            }
            for (int i = 0; i < defaultInclude.length; i++) {
                if (defaultInclude[i] != null) {
                    includePath[index++] = new File(defaultInclude[i]);
                }
            }
        }
        return (File[]) includePath.clone();
    }
    public String getIdentifier() throws BuildException {
        if (identifier == null) {
            StringBuffer buf;
            if (getLibtool()) {
                buf = new StringBuffer("libtool ");
            } else {
                buf = new StringBuffer(' ');
            }
            buf.append(getCommand());
            buf.append(' ');
            buf.append(GccProcessor.getVersion());
            buf.append(' ');
            buf.append(GccProcessor.getMachine());
            identifier = buf.toString();
        }
        return identifier;
    }
    public Linker getLinker(LinkType linkType) {
        return GccLinker.getInstance().getLinker(linkType);
    }
    // BEGINFREEHEP
    public int getMaximumCommandLength() {
        return isWindows() ?  20000 : Integer.MAX_VALUE;
    }
    // ENDFREEHEP
}
