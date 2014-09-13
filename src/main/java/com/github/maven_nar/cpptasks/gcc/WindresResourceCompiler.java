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


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;
/**
 * Adapter for the GNU windres resource compiler.
 * 
 * @author Curt Arnold
 */
public final class WindresResourceCompiler extends CommandLineCompiler {
    private static final WindresResourceCompiler instance = new WindresResourceCompiler(
            false, null);
    public static WindresResourceCompiler getInstance() {
        return instance;
    }
    private WindresResourceCompiler(boolean newEnvironment, Environment env) {
        super("windres", null, new String[]{".rc"}, new String[]{".h", ".hpp",
                ".inl"}, ".o", false, null, newEnvironment, env);
    }
    protected void addImpliedArgs(final Vector<String> args, 
            final boolean debug,
            final boolean multithreaded, 
			final boolean exceptions, 
			final LinkType linkType,
			final Boolean rtti,
			final OptimizationEnum optimization) {
        if (debug) {
            args.addElement("-D_DEBUG");
        } else {
            args.addElement("-DNDEBUG");
        }
    }
    protected void addWarningSwitch(Vector args, int level) {
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new WindresResourceCompiler(newEnvironment, env);
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
        buffer.append("-D");
        buffer.append(define);
        if (value != null && value.length() > 0) {
            buffer.append('=');
            buffer.append(value);
        }
    }
    protected File[] getEnvironmentIncludePath() {
        return new File[0];
    }
    protected String getIncludeDirSwitch(String includeDir) {
        return "-I" + includeDir;
    }
    protected String getInputFileArgument(File outputDir, String filename,
            int index) {
        if (index == 0) {
            String outputFileName = getOutputFileNames(filename, null)[0];
            return "-o" + outputFileName;
        }
        return filename;
    }
    public Linker getLinker(LinkType type) {
        return GccLinker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return 32767;
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
        buffer.append("-U");
        buffer.append(define);
    }
    public String getIdentifier() {
    	return "GNU windres";
    }
}
