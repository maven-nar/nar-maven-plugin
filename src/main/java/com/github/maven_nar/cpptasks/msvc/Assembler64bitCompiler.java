/*
 * 
 * Copyright 2002-2004 The Ant-Contrib project
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
package com.github.maven_nar.cpptasks.msvc;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.Vector;

/**
 * Adapter for the Microsoft (R) Macro Assembler (x64)
 * 
 * @author Curt Arnold
 */
public final class Assembler64bitCompiler extends CommandLineCompiler {
    private static final Assembler64bitCompiler instance = new Assembler64bitCompiler(
            false, null);
    public static Assembler64bitCompiler getInstance() {
        return instance;
    }
    private String identifier;
    private Assembler64bitCompiler(boolean newEnvironment, Environment env) {
        super("ml64", null, new String[]{".asm"}, new String[] {}, ".obj", false, null, newEnvironment, env);
    }
    protected void addImpliedArgs(final Vector args, 
    		final boolean debug,
            final boolean multithreaded, 
			final boolean exceptions, 
			final LinkType linkType,
			final Boolean rtti,
			final OptimizationEnum optimization) {
        args.addElement("/c");
       /* if (debug) {
            args.addElement("/D_DEBUG");
        } else {
            args.addElement("/DNDEBUG");
        }*/
    }
    protected void addWarningSwitch(Vector args, int level) {
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new Assembler64bitCompiler(newEnvironment, env);
        }
        return this;
    }
    /**
     * The include parser for C will work just fine, but we didn't want to
     * inherit from CommandLineCCompiler
     */
    protected Parser createParser(File source) {
        return new CParser();
    }
    protected int getArgumentCountPerInputFile() {
        return 2;
    }
    protected void getDefineSwitch(StringBuffer buffer, String define,
            String value) {
        MsvcProcessor.getDefineSwitch(buffer, define, value);
    }
    protected File[] getEnvironmentIncludePath() {
        return CUtil.getPathFromEnvironment("INCLUDE", ";");
    }
    protected String getIncludeDirSwitch(String includeDir) {
        return MsvcProcessor.getIncludeDirSwitch(includeDir);
    }
    protected String getInputFileArgument(File outputDir, String filename,
            int index) {
        if (index == 0) {
            String outputFileName = getOutputFileNames(filename, null)[0];
            String fullOutputName = new File(outputDir, outputFileName)
                    .toString();
            return "/Fo" + fullOutputName;
        }
        return filename;
    }
    public Linker getLinker(LinkType type) {
        return MsvcLinker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
// FREEHEP stay on the safe side
        return 32000; // 32767;
    }
    protected int getMaximumInputFilesPerCommand() {
        return 1;
    }
    protected int getTotalArgumentLengthForInputFile(File outputDir,
            String inputFile) {
        String arg1 = getInputFileArgument(outputDir, inputFile, 0);
        String arg2 = getInputFileArgument(outputDir, inputFile, 1);
        return arg1.length() + arg2.length() + 2;
    }
    protected void getUndefineSwitch(StringBuffer buffer, String define) {
        MsvcProcessor.getUndefineSwitch(buffer, define);
    }
    public String getIdentifier() {
    	return "Microsoft (R) Macro Assembler (x64)";
    }

    /**
     *  Empty Implementation
     * @param baseDirPath Base directory path.
     * @param includeDirs
     *            Array of include directory paths
     * @param args
     *            Vector of command line arguments used to execute the task
     * @param relativeArgs
     *            Vector of command line arguments used to build the
     * @param includePathId
     * @param isSystem
     */
    protected void addIncludes(String baseDirPath, File[] includeDirs,
                               Vector args, Vector relativeArgs, StringBuffer includePathId,
                               boolean isSystem) {

    }
}
