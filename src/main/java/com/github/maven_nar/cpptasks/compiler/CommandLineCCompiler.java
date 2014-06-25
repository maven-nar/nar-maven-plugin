package com.github.maven_nar.cpptasks.compiler;
import java.io.File;


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;
/**
 * An abstract Compiler implementation which uses an external program to
 * perform the compile.
 * 
 * @author Adam Murdoch
 */
public abstract class CommandLineCCompiler extends CommandLineCompiler {
    protected CommandLineCCompiler(String command, String identifierArg,
            String[] sourceExtensions, String[] headerExtensions,
            String outputSuffix, boolean libtool,
            CommandLineCCompiler libtoolCompiler, boolean newEnvironment,
            Environment env) {
        super(command, identifierArg, sourceExtensions, headerExtensions,
                outputSuffix, libtool, libtoolCompiler, newEnvironment, env);
    }
    protected Parser createParser(File source) {
        return new CParser();
    }
}
