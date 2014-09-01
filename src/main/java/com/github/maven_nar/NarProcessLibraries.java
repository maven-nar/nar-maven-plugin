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
package com.github.maven_nar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * Adds the ability to run arbitrary command line tools to post-process the
 * compiled output (ie: ranlib/ar/etc)
 *
 * @author Richard Kerr
 * @goal nar-process-libraries
 * @phase process-classes
 * @requiresSession
 * @requiresProject
 * @author Richard Kerr
 */
public class NarProcessLibraries extends AbstractCompileMojo {

    /**
     * Defines a set of commands and their configuration to run on the output libraries
     *
     * <pre>
     * &lt;processLibraries&gt;
     *     &lt;libraryToken/&gt;
     *     &lt;commands&gt;
     *         &lt;command&gt;
     *             &lt;libraryType/&gt;
     *             &lt;executable/&gt;
     *             &lt;arguments&gt;
     *                 &lt;argument/&gt;
     *             &lt;/arguments&gt;
     *         &lt;/command&gt;
     *     &lt;/commands&gt;
     * &lt;/processLibraries&gt;
     * </pre>
     *
     * @parameter
     */
    private NarProcessLibrariesConfiguration processLibraries;

    private Log log = getLog();

    @Override
    public void narExecute() throws MojoFailureException, MojoExecutionException {
        log.info("Running process libraries");
        if(processLibraries == null)
            return;
        
        // For each of the libraries defined for this build
        for (Library library : getLibraries()) {
            log.info("Processing library " + library);
            String type = library.getType();
            File outFile;
            // Find what the output directory is
            if (type.equalsIgnoreCase(Library.EXECUTABLE)) {
                File outDir = getLayout().getBinDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
                        getMavenProject().getVersion(), getAOL().toString());
                outFile = new File(outDir, getOutput(false));
            } else {
                File outDir = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
                        getMavenProject().getVersion(), getAOL().toString(), type);
                outFile = new File(outDir, getOutput(true));
            }

            List<ProcessLibraryCommand> commands = processLibraries.getCommands();
            log.debug("Commands available: " + commands);
            log.debug("Token: " + processLibraries.getToken());

            // Then run the commands that are applicable for this library type
            for (ProcessLibraryCommand command : commands == null ? new ArrayList<ProcessLibraryCommand>() : commands) {
                if (command.getType().equalsIgnoreCase(type))
                    log.debug("Command type: " + command.getType());
                    log.debug("Command executable: " + command.getExecutable());
                    log.debug("Command arguments: " + command.getArguments());
                    runCommand(command, outFile);
            }
        }

    }

    private void runCommand(ProcessLibraryCommand command, File outputFile) throws MojoFailureException,
            MojoExecutionException {
        ProcessBuilder p = new ProcessBuilder();
        List<String> commands = command.getCommandList();
        String token = processLibraries.getToken();

        // Substitute the token for the output file name and add to the process builder
        for(int i = 0; i < commands.size(); i++) {
            String val = commands.get(i);
            if(val.contains(token)) {
                val = val.replace(token,outputFile.toString());
            }
            p.command().add(val);
        }

        p.redirectErrorStream(true);
        log.info("Running command \"" + p.command() + "\"");
        try {
            Process process = p.start();
            BufferedInputStream bis = new BufferedInputStream(process.getInputStream());
            byte[] buffer = new byte[1024];
            int endOfStream = 0;
            do {
                endOfStream = bis.read(buffer);
                log.debug(new String(buffer, 0, endOfStream == -1 ? 0 : endOfStream));
            } while (endOfStream != -1);

            if (process.waitFor() != 0) {
                // TODO: Maybe this shouldn't be an exception, it might have
                // still worked?!
                throw new MojoFailureException("Process exited abnormally");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new MojoFailureException("Failed to run the command \"" + p.command() + "\"", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new MojoFailureException("Failed to run the command \"" + p.command() + "\"", e);
        }
    }

}
