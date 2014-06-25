package com.github.maven_nar.cpptasks.compiler;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.VersionInfo;

/**
 * A configuration for a C++ compiler, linker or other processor
 * 
 * @author Curt Arnold
 */
public interface ProcessorConfiguration {
    /**
     * An indication of how much this compiler would like to process this file
     * 
     * @return 0 is no interest to process, 100 is strong interest to process
     */
    int bid(String filename);
    /**
     * Returns a string representation of this configuration. Should be
     * canonical so that equivalent configurations will have equivalent string
     * representations
     */
    String getIdentifier();
    /**
     * Output file name (no path components) corresponding to source file
     * 
     * @param inputFile
     *            input file
     * @return output file names or zero-length array if no output file or name not
     *         determined by input file
     */
    String[] getOutputFileNames(String inputFile, VersionInfo versionInfo);
    ProcessorParam[] getParams();
    /**
     * If true, all files using this configuration should be rebuilt and any
     * existing output files should be ignored
     */
    boolean getRebuild();
}
