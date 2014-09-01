package com.github.maven_nar;

import java.util.List;

/**
 * Configuration definition for nar-process-libraries
 * @author Richard Kerr
 */
public class NarProcessLibrariesConfiguration {
    /**
     * List of commands to execute
     *
     * @parameter expression="${nar.process-libraries.commands} "default-value=""
     */
    private List<ProcessLibraryCommand> commands;

    /**
     * The string token to be replaced by the target file name
     *
     * @parameter expression="${nar.process-libraries.libraryToken}" default-value="\\${library}"
     */
    private String libraryToken = "\\${library}";

    public List<ProcessLibraryCommand> getCommands() {
        return commands;
    }

    public String getToken() {
        return libraryToken;
    }
}
