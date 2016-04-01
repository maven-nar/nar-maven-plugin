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
package com.github.maven_nar.cpptasks;

import java.io.File;
import java.util.Map;
import java.util.Vector;

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.compiler.LinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;

/**
 * This class matches each visited file with an appropriate compiler
 *
 * @author Curt Arnold
 */
public final class TargetMatcher implements FileVisitor {
  private final LinkerConfiguration linker;
  private final Vector<File> objectFiles;
  private final File outputDir;
  private final ProcessorConfiguration[] processors;
  private final File sourceFiles[] = new File[1];
  private final Map<String, TargetInfo> targets;
  private final VersionInfo versionInfo;
  private final CCTask task;

  public TargetMatcher(final CCTask task, final File outputDir, final ProcessorConfiguration[] processors,
      final LinkerConfiguration linker, final Vector<File> objectFiles, final Map<String, TargetInfo> targets,
      final VersionInfo versionInfo) {
    this.task = task;
    this.outputDir = outputDir;
    this.processors = processors;
    this.targets = targets;
    this.linker = linker;
    this.objectFiles = objectFiles;
    this.versionInfo = versionInfo;
  }

  @Override
  public void visit(final File parentDir, final String filename) throws BuildException {
    final File fullPath = new File(parentDir, filename);
    //
    // see if any processor wants to bid
    // on this one
    ProcessorConfiguration selectedCompiler = null;
    int bid = 0;
    if (this.processors != null) {
      for (final ProcessorConfiguration processor : this.processors) {
        final int newBid = processor.bid(fullPath.toString());
        if (newBid > bid) {
          bid = newBid;
          selectedCompiler = processor;
        }
      }
    }
    //
    // no processor interested in file
    // log diagnostic message
    if (bid <= 0) {
      if (this.linker != null) {
        final int linkerbid = this.linker.bid(filename);
        if (linkerbid > 0) {
          this.objectFiles.addElement(fullPath);
          if (linkerbid == 1) {
            this.task.log("Unrecognized file type " + fullPath.toString() + " will be passed to linker");
          }
        }
      }
    } else {
      //
      // get output file name
      // requires full path as output name may be changed based on location
      //
      final String[] outputFileNames = selectedCompiler.getOutputFileNames(fullPath.getPath(), this.versionInfo);
      this.sourceFiles[0] = fullPath;
      //
      // if there is some output for this task
      // (that is a source file and not an header file)
      //
      for (final String outputFileName : outputFileNames) {
        //
        // see if the same output file has already been registered
        //
        final TargetInfo previousTarget = this.targets.get(outputFileName);
        if (previousTarget == null) {
          this.targets.put(outputFileName, new TargetInfo(selectedCompiler, this.sourceFiles, null, new File(
              this.outputDir, outputFileName), selectedCompiler.getRebuild()));
        } else {
          if (!previousTarget.getSources()[0].equals(this.sourceFiles[0])) {
            final String builder = "Output filename conflict: " + outputFileName +
                " would be produced from " +
                previousTarget.getSources()[0].toString() +
                " and " +
                filename;
            throw new BuildException(builder);
          }
        }
      }
    }
  }
}
