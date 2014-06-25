package com.github.maven_nar.cpptasks.compiler;
import java.io.File;
/**
 * A compiler that can utilize precompilation of header files
 * 
 * @author Curt Arnold
 */
public interface PrecompilingCompiler {
    /**
     * 
     * This method may be used to get two distinct compiler configurations, one
     * for compiling the specified file and producing a precompiled header
     * file, and a second for compiling other files using the precompiled
     * header file.
     * 
     * The last (preferrably only) include directive in the prototype file will
     * be used to mark the boundary between pre-compiled and normally compiled
     * headers.
     * 
     * @param config
     *            base configuration
     * @param prototype
     *            A source file (for example, stdafx.cpp) that is used to build
     *            the precompiled header file. @returns null if precompiled
     *            headers are not supported or a two element array containing
     *            the precompiled header generation configuration and the
     *            consuming configuration
     *  
     */
    CompilerConfiguration[] createPrecompileConfigurations(
            CompilerConfiguration config, File prototype,
            String[] nonPrecompiledFiles);
}
