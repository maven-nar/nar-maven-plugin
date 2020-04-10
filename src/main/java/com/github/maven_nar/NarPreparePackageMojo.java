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

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Create the nar.properties file.
 * 
 * @author GDomjan
 */
@Mojo(name = "nar-prepare-package", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresProject = true)
public class NarPreparePackageMojo extends AbstractNarMojo {

  // TODO: this is working of what is present rather than what was requested to
  // be built, POM ~/= artifacts!
  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {
    // let the layout decide which (additional) nars to attach
    getLayout().prepareNarInfo(getTargetDirectory(), getMavenProject(), getNarInfo(), this);
    getNarInfo().writeToDirectory(this.classesDirectory);

    final String artifactIdVersion = getMavenProject().getArtifactId() + "-" + getMavenProject().getVersion();

    // Scan target directory to identify project classifier directories, skipping noarch
    File[] files = getTargetDirectory().listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().startsWith(artifactIdVersion) && (!file.getName().endsWith(NarConstants.NAR_NO_ARCH));
      }
    });
    
    // Write nar info to project classifier directories
    getNarInfo().writeToDirectory(files);
    
    // process the replay files here
    if (replay != null && replay.getScripts() != null && !replay.getScripts().isEmpty()) {

      File compileCommandsInFile = new File(replay.getOutputDirectory(), NarConstants.REPLAY_COMPILE_NAME);
      File linkCommandsInputInFile = new File(replay.getOutputDirectory(), NarConstants.REPLAY_LINK_NAME);
      File testCompileCommandsInFile = new File(replay.getOutputDirectory(), NarConstants.REPLAY_TEST_COMPILE_NAME);
      File testLinkCommandsInFile = new File(replay.getOutputDirectory(), NarConstants.REPLAY_TEST_LINK_NAME);
      try {
        List<String> compileCommands = Files.readAllLines(compileCommandsInFile.toPath());
        List<String> linkCommands = Files.readAllLines(linkCommandsInputInFile.toPath());
        List<String> testCompileCommands = Files.readAllLines(testCompileCommandsInFile.toPath());
        List<String> testLinkCommands = Files.readAllLines(testLinkCommandsInFile.toPath());
        
        for (Script script : replay.getScripts()) {
          File outDir = new File(replay.getOutputDirectory(), script.getId());

          File compileCommandsOutFile = new File(outDir, NarConstants.REPLAY_COMPILE_NAME + "." + script.getExtension());
          File linkCommandsOutputInFile = new File(outDir, NarConstants.REPLAY_LINK_NAME + "." + script.getExtension());
          File testCompileCommandsOutFile = new File(outDir, NarConstants.REPLAY_TEST_COMPILE_NAME + "." + script.getExtension());
          File testLinkCommandsOutFile = new File(outDir, NarConstants.REPLAY_TEST_LINK_NAME + "." + script.getExtension());

          if (script.isCompile()) {
            processReplayFile(compileCommands, script, compileCommandsOutFile);
            getLog().info("Wrote compile replay file: " + compileCommandsOutFile);
          }
          
          if (script.isLink()) {
            processReplayFile(linkCommands, script, linkCommandsOutputInFile);
            getLog().info("Wrote link replay file: " + linkCommandsOutputInFile);
          }
          
          if (script.testCompile) {
            processReplayFile(testCompileCommands, script, testCompileCommandsOutFile);
            getLog().info("Wrote test compile replay file: " + testCompileCommandsOutFile);
          }
          
          if (script.isTestLink()) {
            processReplayFile(testLinkCommands, script, testLinkCommandsOutFile);
            getLog().info("Wrote link replay file: " + testLinkCommandsOutFile);
            
          }
        }
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to read command history", e);
      }
    }
  }

  public void processReplayFile(List<String> lines, Script script, File outFile) throws MojoExecutionException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(outFile))) {
      for (String line : lines) {
        String processed = line;
        if (script.getSubstitutions() != null) {
          for (Substitution sub : script.getSubstitutions()) {
            processed = sub.substitute(processed);
          }
        }
        writer.println(processed);
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Unable to write replay script to " + outFile, e);
    }
  }
}
