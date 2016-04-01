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
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;

/**
 * Adds the ability to run arbitrary command line tools to post-process the
 * compiled output (ie: ranlib/ar/etc)
 *
 * @author Richard Kerr
 * @author Richard Kerr
 */
@Mojo(name = "nar-process-libraries", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true)
public class NarProcessLibraries extends AbstractCompileMojo {

  /**
   * List of commands to execute
   */
  @Parameter
  private List<ProcessLibraryCommand> commands;

  private final Log log = getLog();

  /**
   * The method must be implemented but will not be called.
   */
  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    return null;
  }

  @Override
  public void narExecute() throws MojoFailureException, MojoExecutionException {
    this.log.info("Running process libraries");
    // For each of the libraries defined for this build
    for (final Library library : getLibraries()) {
      this.log.info("Processing library " + library);
      final String type = library.getType();
      File outFile;
      // Find what the output directory is
      if (type.equalsIgnoreCase(Library.EXECUTABLE)) {
        final File outDir = getLayout().getBinDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
            getMavenProject().getVersion(), getAOL().toString());
        outFile = new File(outDir, getOutput(false));
      } else {
        final File outDir = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
            getMavenProject().getVersion(), getAOL().toString(), type);
        outFile = new File(outDir, getOutput(true));
      }

      // Then run the commands that are applicable for this library type
      for (final ProcessLibraryCommand command : this.commands == null ? new ArrayList<ProcessLibraryCommand>()
          : this.commands) {
        if (command.getType().equalsIgnoreCase(type)) {
          runCommand(command, outFile);
        }
      }
    }

  }

  private void runCommand(final ProcessLibraryCommand command, final File outputFile)
      throws MojoFailureException, MojoExecutionException {
    final ProcessBuilder p = new ProcessBuilder(command.getCommandList());
    p.command().add(outputFile.toString());
    p.redirectErrorStream(true);
    this.log.info("Running command \"" + p.command() + "\"");
    try {
      final Process process = p.start();
      final BufferedInputStream bis = new BufferedInputStream(process.getInputStream());
      final byte[] buffer = new byte[1024];
      int endOfStream = 0;
      do {
        endOfStream = bis.read(buffer);
        this.log.debug(new String(buffer, 0, endOfStream == -1 ? 0 : endOfStream));
      } while (endOfStream != -1);

      if (process.waitFor() != 0) {
        // TODO: Maybe this shouldn't be an exception, it might have
        // still worked?!
        throw new MojoFailureException("Process exited abnormally");
      }
    } catch (final IOException | InterruptedException e) {
      e.printStackTrace();
      throw new MojoFailureException("Failed to run the command \"" + p.command() + "\"", e);
    }
  }

}
