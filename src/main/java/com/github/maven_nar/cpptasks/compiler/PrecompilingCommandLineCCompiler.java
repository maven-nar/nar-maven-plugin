package com.github.maven_nar.cpptasks.compiler;
import java.io.File;


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;
/**
 * A command line C compiler that can utilize precompilation of header files
 * 
 * @author Curt Arnold
 */
public abstract class PrecompilingCommandLineCCompiler
        extends
            PrecompilingCommandLineCompiler {
    protected PrecompilingCommandLineCCompiler(String command,
            String identifierArg, String[] sourceExtensions,
            String[] headerExtensions, String outputSuffix, boolean libtool,
            PrecompilingCommandLineCCompiler libtoolCompiler,
            boolean newEnvironment, Environment env) {
        super(command, identifierArg, sourceExtensions, headerExtensions,
                outputSuffix, libtool, libtoolCompiler, newEnvironment, env);
    }
    protected Parser createParser(File source) {
        return new CParser();
    }
}
