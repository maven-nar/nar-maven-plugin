package com.github.maven_nar.cpptasks.compiler;
import java.io.File;


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.parser.FortranParser;
import com.github.maven_nar.cpptasks.parser.Parser;
/**
 * An abstract Compiler implementation which uses an external program to
 * perform the compile.
 * 
 * @author Curt Arnold
 */
public abstract class CommandLineFortranCompiler extends CommandLineCompiler {
    protected CommandLineFortranCompiler(String command, String identifierArg,
            String[] sourceExtensions, String[] headerExtensions,
            String outputSuffix, boolean libtool,
            CommandLineFortranCompiler libtoolCompiler, boolean newEnvironment,
            Environment env) {
        super(command, identifierArg, sourceExtensions, headerExtensions,
                outputSuffix, libtool, libtoolCompiler, newEnvironment, env);
    }
    protected Parser createParser(File source) {
        return new FortranParser();
    }
}
