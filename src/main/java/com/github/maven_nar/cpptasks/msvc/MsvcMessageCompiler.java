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
package com.github.maven_nar.cpptasks.msvc;
import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.parser.Parser;
import com.github.maven_nar.cpptasks.OptimizationEnum;

import org.apache.tools.ant.types.Environment;
/**
 * Adapter for the Microsoft (r) Windows 32 Message Compiler
 * 
 * @author Greg Domjan
 * 
 * MC [-?aAbcdnouUv] [-co] [-cs namespace] [-css namespace] [-e extension]
 *    [-h path] [-km] [-m length] [-mof] [-p prefix] [-P prefix] [-r path]
 *    [-s path] [-t path] [-w path] [-W path] [-x path] [-z name]
 *    filename [filename]
 */
public final class MsvcMessageCompiler extends CommandLineCompiler {
    private static final MsvcMessageCompiler instance = new MsvcMessageCompiler(
            false, null);

    public static MsvcMessageCompiler getInstance() {
        return instance;
    }

    private MsvcMessageCompiler(boolean newEnvironment, Environment env) {
        super("mc", null, new String[]{".mc",".man"}, new String[]{},
            ".rc", false, null, newEnvironment, env);
    }
    protected void addImpliedArgs(final Vector<String> args,
            final boolean debug,
            final boolean multithreaded,
	    final boolean exceptions,
	    final LinkType linkType,
	    final Boolean rtti,
	    final OptimizationEnum optimization) {
        // no identified configuration compiler arguments implied from these options.
    }

    protected void addWarningSwitch(Vector<String> args, int level) {
    }

    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new MsvcMessageCompiler(newEnvironment, env);
        }
        return this;
    }

    protected boolean canParse(File sourceFile){ return false; }

    protected Parser createParser(File source) {
    	// neither file type has references to other elements that need to be found through parsing.
        return null;
    }

    protected int getArgumentCountPerInputFile() {
        return 3;
    }

    protected void getDefineSwitch(StringBuffer buffer, String define,
            String value) {
        // no define switch
    }

    protected File[] getEnvironmentIncludePath() {
        return CUtil.getPathFromEnvironment("INCLUDE", ";");
    }

    protected void addIncludes(String baseDirPath, File[] includeDirs,
            Vector<String> args, Vector<String> relativeArgs, StringBuffer includePathId,
            boolean isSystem) {
    	// no include switch
    	// for some reason we are still getting args in the output??
    }

    protected String getIncludeDirSwitch(String includeDir) {
        return null; // no include switch
    }

    protected String getInputFileArgument(File outputDir, String filename,
            int index) {
        switch (index) {
	        case 0 :
	            return "-r";
	        case 1 :
	            return outputDir.getAbsolutePath();
	    }
        return filename;
    }
    public Linker getLinker(LinkType type) {
        return MsvcLinker.getInstance().getLinker(type);
    }

    public int getMaximumCommandLength() {
        return 32000;
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
    	return "Microsoft (R) Windows (R) Message Compiler";
    }
}
