package com.github.maven_nar.cpptasks.compiler;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.TargetDef;
import com.github.maven_nar.cpptasks.VersionInfo;
/**
 * A processor. Base interface for Compiler and Linker
 * 
 * @author Curt Arnold
 */
public interface Processor {
    /**
     * Returns a bid indicating the desire of this compiler to process the
     * file.
     * 
     * @param inputFile
     *            input file
     * @return 0 = no interest, 100 = high interest
     */
    int bid(String inputFile);
    Processor changeEnvironment(boolean newEnvironment, Environment env);
    /**
     * Returns the compiler configuration for <cc>or <compiler>element.
     * 
     * @param defaultProviders
     *            When specificConfig corresponds to a <compiler>or linker
     *            element, defaultProvider will be a zero to two element array.
     *            If there is an extends attribute, the first element will be
     *            the referenced ProcessorDef, unless inherit = false, the last
     *            element will be the containing <cc>element
     * @param specificConfig
     *            A <cc>or <compiler>element.
     * @return resulting configuration
     */
    ProcessorConfiguration createConfiguration(CCTask task, LinkType linkType,
            ProcessorDef[] defaultProviders, ProcessorDef specificConfig,
			TargetDef targetPlatform, VersionInfo versionInfo);
    /**
     * Retrieve an identifier that identifies the specific version of the
     * compiler. Compilers with the same identifier should produce the same
     * output files for the same input files and command line switches.
     */
    String getIdentifier();
    /**
     * Gets the linker that is associated with this processors
     */
    Linker getLinker(LinkType type);
    /**
     * Output file name (no path components) corresponding to source file
     * 
     * @param inputFile
     *            input file
     * @return output file name or null if no output file or name not
     *         determined by input file
     */
    String[] getOutputFileNames(String inputFile, VersionInfo versionInfo);
}
