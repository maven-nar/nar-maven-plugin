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
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;

/**
 * Create the nar.properties file.
 * 
 * @author GDomjan
 */
@Mojo(name = "nar-prepare-package", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, 
  requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class NarPreparePackageMojo extends AbstractCompileMojo {

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
        
        List<String> testCompileCommands = null;
        List<String> testLinkCommands = null;
        if (!this.skipTests) {
          testCompileCommands = Files.readAllLines(testCompileCommandsInFile.toPath());
          testLinkCommands = Files.readAllLines(testLinkCommandsInFile.toPath());
        }
        
        for (Script script : replay.getScripts()) {

          File scriptFile = new File(replay.getScriptDirectory(), script.getId() + "." + script.getExtension());

          try (PrintWriter writer = new PrintWriter(new FileWriter(scriptFile))) {
            
            for (String header : script.getHeaders()) {
              writer.println(header);
            }
            
            if (script.isCompile()) {
              writer.println();
              processReplayFile(compileCommands, script, writer);
              getLog().info("Wrote compile commands to file: " + scriptFile);
            }
            
            if (script.isLink()) {
              writer.println();
              processReplayFile(linkCommands, script, writer);
              getLog().info("Wrote link commands to file: " + scriptFile);
            }
            
            if (script.testCompile && !this.skipTests) {
              writer.println();
              processReplayFile(testCompileCommands, script, writer);
              getLog().info("Wrote test compile commands to file: " + scriptFile);
            }
            
            if (script.isTestLink() && !this.skipTests) {
              writer.println();
              processReplayFile(testLinkCommands, script, writer);
              getLog().info("Wrote test link commands to file: " + scriptFile);
            }
            
            for (String footer : script.getFooters()) {
              writer.println(footer);
            }

            Set<PosixFilePermission> perms = NarUtil.parseOctalPermission(script.getMode());
            Files.setPosixFilePermissions(scriptFile.toPath(), perms);
          }
          catch (IOException e) {
          throw new MojoExecutionException("Unable to write replay script to " + scriptFile, e);
        }
        }
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to read command history", e);
      }
    }
  }

  public void processReplayFile(List<String> lines, Script script, PrintWriter writer) throws MojoExecutionException {
    for (String line : lines) {
      String processed = line;
      if (script.getSubstitutions() != null) {
        for (Substitution sub : script.getSubstitutions()) {
          processed = sub.substitute(processed);
        }
      }
      if (script.isEchoLines()) {
        writer.print("echo " );
        writer.println(processed);
      }
      writer.println(processed);
    }
  }

  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    return new ScopeFilter(Artifact.SCOPE_COMPILE, null);
  }
}
