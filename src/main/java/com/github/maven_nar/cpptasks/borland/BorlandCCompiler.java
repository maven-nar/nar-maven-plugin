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
package com.github.maven_nar.cpptasks.borland;
import java.io.File;
import java.util.Vector;


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.PrecompilingCommandLineCCompiler;
import com.github.maven_nar.cpptasks.compiler.Processor;
/**
 * Adapter for the Borland(r) C/C++ compiler.
 * 
 * @author Curt Arnold
 */
public class BorlandCCompiler extends PrecompilingCommandLineCCompiler {
    private static final String[] headerExtensions = new String[]{".h", ".hpp",
            ".inl"};
    private static final String[] sourceExtensions = new String[]{".c", ".cc",
            ".cpp", ".cxx", ".c++"};
    private static final BorlandCCompiler instance = new BorlandCCompiler(
            false, null);
    public static BorlandCCompiler getInstance() {
        return instance;
    }
    private BorlandCCompiler(boolean newEnvironment, Environment env) {
        super("bcc32", "--version", sourceExtensions, headerExtensions, ".obj", false,
                null, newEnvironment, env);
    }
    protected void addImpliedArgs(final Vector<String> args, 
    		final boolean debug,
            final boolean multithreaded, 
			final boolean exceptions, 
			final LinkType linkType,
			final Boolean rtti,
			final OptimizationEnum optimization) {
        args.addElement("-c");
        //
        //  turn off compiler autodependency since
        //     we do it ourselves
        args.addElement("-X");
        if (exceptions) {
            args.addElement("-x");
        } else {
            args.addElement("-x-");
        }
        if (multithreaded) {
            args.addElement("-tWM");
        }
        if (debug) {
            args.addElement("-Od");
            args.addElement("-v");
        } else {
        	if (optimization != null) {
        		if (optimization.isSpeed()) {
        			args.addElement("-O1");
        		} else {
        			if (optimization.isSpeed()) {
        				args.addElement("-O2");
        			} else {
        				if (optimization.isNoOptimization()) {
        					args.addElement("-Od");
        				}
        			}
        		}
        	}
        }
        if (rtti != null && !rtti.booleanValue()) {
        	args.addElement("-RT-");
        }
    }
    protected void addWarningSwitch(Vector<String> args, int level) {
        BorlandProcessor.addWarningSwitch(args, level);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new BorlandCCompiler(newEnvironment, env);
        }
        return this;
    }
    protected CompilerConfiguration createPrecompileGeneratingConfig(
            CommandLineCompilerConfiguration baseConfig, File prototype,
            String lastInclude) {
        String[] additionalArgs = new String[]{"-H=" + lastInclude, "-Hc"};
        return new CommandLineCompilerConfiguration(baseConfig, additionalArgs,
                null, true);
    }
    protected CompilerConfiguration createPrecompileUsingConfig(
            CommandLineCompilerConfiguration baseConfig, File prototype,
            String lastInclude, String[] exceptFiles) {
        String[] additionalArgs = new String[]{"-Hu"};
        return new CommandLineCompilerConfiguration(baseConfig, additionalArgs,
                exceptFiles, false);
    }
    protected void getDefineSwitch(StringBuffer buffer, String define,
            String value) {
        BorlandProcessor.getDefineSwitch(buffer, define, value);
    }
    protected File[] getEnvironmentIncludePath() {
        return BorlandProcessor.getEnvironmentPath("bcc32", 'I',
                new String[]{"..\\include"});
    }
    protected String getIncludeDirSwitch(String includeDir) {
        return BorlandProcessor.getIncludeDirSwitch("-I", includeDir);
    }
    public Linker getLinker(LinkType type) {
        return BorlandLinker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return 1024;
    }
    protected void getUndefineSwitch(StringBuffer buffer, String define) {
        BorlandProcessor.getUndefineSwitch(buffer, define);
    }
}
