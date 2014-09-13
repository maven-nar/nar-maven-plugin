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
package com.github.maven_nar.cpptasks.arm;
import java.io.File;
import java.util.Vector;


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
/**
 * Adapter for the ARM C Compilers
 *
 * See Doc No: ARM DUI 0151A, Issued: Nov 2001 at
 * http://www.arm.com/arm/User_Guides?OpenDocument
 *
 * @author Curt Arnold
 *
 */
public class ADSCCompiler extends CommandLineCCompiler {
    /**
     * Header file extensions
     */
    private static final String[] headerExtensions = new String[]{".h", ".hpp",
            ".inl"};
    /**
     * Source file extensions
     */
    private static final String[] sourceExtensions = new String[]{".c", ".cc",
            ".cpp", ".cxx", ".c++"};
    /**
     * Singleton for ARM 32-bit C compiler
     */
    private static final ADSCCompiler armcc = new ADSCCompiler("armcc", false,
            null);
    /**
     * Singleton for ARM 32-bit C++ compiler
     */
    private static final ADSCCompiler armcpp = new ADSCCompiler("armcpp",
            false, null);
    /**
     * Singleton for ARM 16-bit C compiler
     */
    private static final ADSCCompiler tcc = new ADSCCompiler("tcc", false, null);
    /**
     * Singleton for ARM 16-bit C++ compiler
     */
    private static final ADSCCompiler tcpp = new ADSCCompiler("tcpp", false,
            null);
    /**
     * Singleton for ARM 32-bit C compiler
     */
    public static ADSCCompiler getArmCC() {
        return armcc;
    }
    /**
     * Singleton for ARM 32-bit C++ compiler
     */
    public static ADSCCompiler getArmCpp() {
        return armcpp;
    }
    /**
     * Singleton for ARM 16-bit C compiler
     */
    public static ADSCCompiler getThumbCC() {
        return tcc;
    }
    /**
     * Singleton for ARM 16-bit C++ compiler
     */
    public static ADSCCompiler getThumbCpp() {
        return tcpp;
    }
    private static void quoteFile(StringBuffer buf, String outPath) {
        if (outPath.indexOf(' ') >= 0) {
            buf.append('\"');
            buf.append(outPath);
            buf.append('\"');
        } else {
            buf.append(outPath);
        }
    }
    /**
     * Private constructor
     * 
     * @param command
     *            executable name
     * @param newEnvironment
     *            Change environment
     * @param env
     *            New environment
     */
    private ADSCCompiler(String command, boolean newEnvironment, Environment env) {
        super(command, "-vsn", sourceExtensions, headerExtensions, ".o", false,
                null, newEnvironment, env);
    }
    /**
     * {@inheritDoc}
     */
    protected void addImpliedArgs(Vector<String> args, 
    		final boolean debug,
            final boolean multithreaded, 
			final boolean exceptions, 
			final LinkType linkType,
			final Boolean rtti,
			final OptimizationEnum optimization) {
        if (debug) {
            args.addElement("-g");
        }
        //
        //   didn't see anything about producing
        //     anything other than executables in the docs
        if (linkType.isExecutable()) {
        } else if (linkType.isSharedLibrary()) {
        }
    }
    /**
     * Adds flags that customize the warnings reported
     * 
     * Compiler does not appear to have warning levels but ability to turn off
     * specific errors by explicit switches, could fabricate levels by
     * prioritizing errors.
     * 
     * @see com.github.maven_nar.cpptasks.compiler.CommandLineCompiler#addWarningSwitch(java.util.Vector,
     *      int)
     */
    protected void addWarningSwitch(Vector<String> args, int warnings) {
    }
    /**
     * Add command line options for preprocessor macro
     * 
     * @see com.github.maven_nar.cpptasks.compiler.CommandLineCompiler#getDefineSwitch(java.lang.StringBuffer,
     *      java.lang.String, java.lang.String)
     */
    protected void getDefineSwitch(StringBuffer buffer, String define,
            String value) {
        buffer.append("-D");
        buffer.append(define);
        if (value != null) {
            buffer.append('=');
            buffer.append(value);
        }
    }
    /**
     * ARMINC environment variable contains the default include path
     * 
     * @see com.github.maven_nar.cpptasks.compiler.CommandLineCompiler#getEnvironmentIncludePath()
     */
    protected File[] getEnvironmentIncludePath() {
        return CUtil.getPathFromEnvironment("ARMINC", ";");
    }
    /**
     * Returns command line option to specify include directory
     *  
     */
    protected String getIncludeDirSwitch(String source) {
        StringBuffer buf = new StringBuffer("-I");
        quoteFile(buf, source);
        return buf.toString();
    }
    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return ADSLibrarian.getInstance();
        }
        if (type.isSharedLibrary()) {
            return ADSLinker.getDllInstance();
        }
        return ADSLinker.getInstance();
    }
    /**
     * Maximum command line length
     * 
     * @see com.github.maven_nar.cpptasks.compiler.CommandLineCompiler#getMaximumCommandLength()
     */
    public int getMaximumCommandLength() {
        return 1000;
    }
    /** Adds command to undefine preprocessor macro. */
    protected void getUndefineSwitch(StringBuffer buffer, String define) {
        buffer.append("-U");
        buffer.append(define);
    }
}
