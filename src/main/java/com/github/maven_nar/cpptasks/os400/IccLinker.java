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
package com.github.maven_nar.cpptasks.os400;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * Adapter for the IBM (R) OS/390 (tm) Linker
 *
 * @author Hiram Chirino (cojonudo14@hotmail.com)
 */
public final class IccLinker extends CommandLineLinker {
  private static final IccLinker datasetLinker = new IccLinker();
  private static final IccLinker dllLinker = new IccLinker("", ".dll");
  private static final IccLinker instance = new IccLinker("", "");

  private static int addLibraryPatterns(final String[] libnames, final StringBuffer buf, final String prefix,
      final String extension, final String[] patterns, final int offset) {
    for (int i = 0; i < libnames.length; i++) {
      buf.setLength(0);
      buf.append(prefix);
      buf.append(libnames[i]);
      buf.append(extension);
      patterns[offset + i] = buf.toString();
    }
    return offset + libnames.length;
  }

  public static IccLinker getDataSetInstance() {
    return datasetLinker;
  }

  public static IccLinker getInstance() {
    return instance;
  }

  private final boolean isADatasetLinker;
  File outputFile;
  private final String outputPrefix;
  CCTask task;

  private IccLinker() {
    super("icc", "/bogus", new String[] {
        ".o", ".a", ".lib", ".xds"
    }, new String[] {
        ".dll", ".x"
    }, ".xds", false, null);
    this.outputPrefix = "";
    this.isADatasetLinker = true;
  }

  private IccLinker(final String outputPrefix, final String outputSuffix) {
    super("icc", "/bogus", new String[] {
        ".o", ".a", ".lib", ".x"
    }, new String[] {
      ".dll"
    }, outputSuffix, false, null);
    this.outputPrefix = outputPrefix;
    this.isADatasetLinker = false;
  }

  protected void addBase(final long base, final Vector<String> args) {
  }

  protected void addEntry(final String entry, final Vector<String> args) {
  }

  protected void addFixed(final Boolean fixed, final Vector<String> args) {
  }

  protected void addImpliedArgs(final boolean debug, final LinkType linkType, final Vector<String> args) {
    if (linkType.isSharedLibrary()) {
      args.addElement("-W");
      args.addElement("l,DLL");
    }
  }

  protected void addIncremental(final boolean incremental, final Vector<String> args) {
  }

  @Override
  protected String[] addLibrarySets(final CCTask task, final LibrarySet[] libsets, final Vector<String> preargs,
      final Vector<String> midargs, final Vector<String> endargs) {
    // If yo want to link against a library sitting in a dataset and
    // not in the HFS, you can just use the //'dataset' notation
    // to specify it. e.g:
    // <libset dir="." libs="//'MQM.V5R2M0.SCSQLOAD'"/>
    //
    // We have to have special handling here because the file is not
    // on the normal filesystem so the task will not noramly include it
    // as part of the link command.
    if (libsets != null) {
      for (final LibrarySet libset : libsets) {
        final String libs[] = libset.getLibs();
        for (final String lib : libs) {
          if (lib.startsWith("//")) {
            endargs.addElement("-l");
            endargs.addElement(lib);
          } else if (libset.getDataset() != null) {
            final String ds = libset.getDataset();
            endargs.addElement("//'" + ds + "(" + lib + ")'");
          }
        }
      }
    }
    return super.addLibrarySets(task, libsets, preargs, midargs, endargs);
  }

  protected void addMap(final boolean map, final Vector<String> args) {
  }

  protected void addStack(final int stack, final Vector<String> args) {
  }

  @Override
  public String getCommandFileSwitch(final String commandFile) {
    return "@" + commandFile;
  }

  @Override
  public File[] getLibraryPath() {
    return CUtil.getPathFromEnvironment("LIB", ";");
  }

  @Override
  public String[] getLibraryPatterns(final String[] libnames, final LibraryTypeEnum libType) {
    final StringBuffer buf = new StringBuffer();
    final String[] patterns = new String[libnames.length * 3];
    int offset = addLibraryPatterns(libnames, buf, "lib", ".a", patterns, 0);
    offset = addLibraryPatterns(libnames, buf, "", ".x", patterns, offset);
    offset = addLibraryPatterns(libnames, buf, "", ".o", patterns, offset);
    return patterns;
  }

  @Override
  public Linker getLinker(final LinkType linkType) {
    if (this == datasetLinker) {
      return datasetLinker;
    }
    if (linkType.isSharedLibrary()) {
      return dllLinker;
    }
    return instance;
  }

  @Override
  public int getMaximumCommandLength() {
    return Integer.MAX_VALUE;
  }

  @Override
  public String[] getOutputFileNames(final String baseName, final VersionInfo versionInfo) {
    final String[] baseNames = super.getOutputFileNames(baseName, versionInfo);
    if (this.outputPrefix.length() > 0) {
      for (int i = 0; i < baseNames.length; i++) {
        baseNames[i] = this.outputPrefix + baseNames[i];
      }
    }
    return baseNames;
  }

  @Override
  protected String[] getOutputFileSwitch(final CCTask task, String outputFile) {
    if (this.isADatasetLinker && task.getDataset() != null) {
      final String ds = task.getDataset();
      outputFile = "//'" + ds + "(" + outputFile + ")'";
    }
    return getOutputFileSwitch(outputFile);
  }

  @Override
  public String[] getOutputFileSwitch(final String outputFile) {
    return new String[] {
        "-o", outputFile
    };
  }

  @Override
  public boolean isCaseSensitive() {
    return IccProcessor.isCaseSensitive();
  }

  @Override
  public void link(final CCTask task, File outputFile, final String[] sourceFiles,
      final CommandLineLinkerConfiguration config) throws BuildException {
    this.task = task;
    this.outputFile = outputFile;
    if (this.isADatasetLinker) {
      final int p = outputFile.getName().indexOf(".");
      if (p >= 0) {
        final String newname = outputFile.getName().substring(0, p);
        outputFile = new File(outputFile.getParent(), newname);
      }
    }
    super.link(task, outputFile, sourceFiles, config);
  }

  @Override
  protected int runCommand(final CCTask task, final File workingDir, final String[] cmdline) throws BuildException {
    final int rc = super.runCommand(task, workingDir, cmdline);
    // create the .xds file if everything was ok.
    if (rc == 0) {
      try {
        this.outputFile.delete();
        new FileOutputStream(this.outputFile).close();
      } catch (final IOException e) {
        throw new BuildException(e.getMessage());
      }
    }
    return rc;
  }
}
