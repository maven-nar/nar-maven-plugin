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
package com.github.maven_nar.cpptasks.openwatcom;

import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;


/**
 * Adapter for the OpenWatcom Librarian.
 *
 * @author Curt Arnold
 */
public final class OpenWatcomLibrarian
    extends CommandLineLinker {
  /**
   * Singleton.
   */
  private static final OpenWatcomLibrarian INSTANCE = new OpenWatcomLibrarian();
  /**
   * Singleton accessor.
   * @return OpenWatcomLibrarian librarian instance
   */
  public static OpenWatcomLibrarian getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   */
  private OpenWatcomLibrarian() {
    super("wlib", null, new String[] {".obj"}
          , new String[0], ".lib", false,
          null);
  }

  /**
   * Add base address.
   * @param base long base address
   * @param args Vector command line arguments
   */
  protected void addBase(final long base, final Vector<String> args) {
  }

  /**
   * Add alternative entry point.
   * @param entry String entry point
   * @param args Vector command line arguments
   */
  protected void addEntry(final String entry, final Vector<String> args) {
  }

  /**
   * Add fixed parameter.
   * @param fixed Boolean true if fixed
   * @param args Vector command line arguments
   */
  protected void addFixed(final Boolean fixed, final Vector<String> args) {
  }

  /**
   * Add implied arguments.
   * @param debug boolean true if debugging
   * @param linkType LinkType link type
   * @param args Vector command line arguments
   */
  protected void addImpliedArgs(final boolean debug,
                                final LinkType linkType,
                                final Vector<String> args) {
  }

  /**
   * Add incremental option.
   * @param incremental boolean true if incremental
   * @param args Vector command line arguments
   */
  protected void addIncremental(final boolean incremental,
                                final Vector<String> args) {
  }

  /**
   * Add map option.
   * @param map boolean true to create map file
   * @param args Vector command line argument
   */
  protected void addMap(final boolean map,
                        final Vector<String> args) {
  }

  /**
   * Add stack size option.
   * @param stack int stack size
   * @param args Vector command line arguments
   */
  protected void addStack(final int stack,
                          final Vector<String> args) {
  }

  /**
   * Get command file switch.
   * @param cmdFile String command file
   * @return String command file switch
   */
  protected String getCommandFileSwitch(final String cmdFile) {
    return OpenWatcomProcessor.getCommandFileSwitch(cmdFile);
  }


  /**
   * Get library search path.
   * @return File[] library search path
   */
  public File[] getLibraryPath() {
    return CUtil.getPathFromEnvironment("LIB", ";");
  }

  /**
   * Get file selectors for specified library names.
   * @param libnames String[] library names
   * @param libType LibraryTypeEnum library type enum
   * @return String[] file selection patterns
   */
  public String[] getLibraryPatterns(final String[] libnames,
                                     final LibraryTypeEnum libType) {
    return OpenWatcomProcessor.getLibraryPatterns(libnames, libType);
  }

  /**
   * Get linker.
   * @param type LinkType link type
   * @return Linker linker
   */
  public Linker getLinker(final LinkType type) {
    return OpenWatcomCLinker.getInstance().getLinker(type);
  }

  /**
   * Gets maximum command line.
   * @return int maximum command line
   */
  public int getMaximumCommandLength() {
    return 1024;
  }

  /**
   * Create output file switch.
   * @param outFile String output file switch
   * @return String[] output file switch
   */
  public String[] getOutputFileSwitch(final String outFile) {
    return OpenWatcomProcessor.getOutputFileSwitch(outFile);
  }

  /**
   * Gets case-sensisitivity of processor.
   * @return boolean true if case sensitive
   */
  public boolean isCaseSensitive() {
    return OpenWatcomProcessor.isCaseSensitive();
  }

  /**
   * Builds a library.
   * @param task task
   * @param outputFile generated library
   * @param sourceFiles object files
   * @param config linker configuration
   */
  public void link(final CCTask task,
                   final File outputFile,
                   final String[] sourceFiles,
                   final CommandLineLinkerConfiguration config) {
    //
    //  delete any existing library
    outputFile.delete();
    //
    //  build a new library
    super.link(task, outputFile, sourceFiles, config);
  }

  /**
   * Prepares argument list for exec command.
   * @param task task
   * @param outputDir output directory
   * @param outputName output file name
   * @param sourceFiles object files
   * @param config linker configuration
   * @return arguments for runTask
   */
  protected String[] prepareArguments(
      final CCTask task,
      final String outputDir,
      final String outputName,
      final String[] sourceFiles,
      final CommandLineLinkerConfiguration config) {
    String[] preargs = config.getPreArguments();
    String[] endargs = config.getEndArguments();
    StringBuffer buf = new StringBuffer();
    Vector<String> execArgs = new Vector<String>(preargs.length + endargs.length + 10
                                 + sourceFiles.length);

    execArgs.addElement(this.getCommand());
    String outputFileName = new File(outputDir, outputName).toString();
    execArgs.addElement(quoteFilename(buf, outputFileName));

    for (int i = 0; i < preargs.length; i++) {
      execArgs.addElement(preargs[i]);
    }

    int objBytes = 0;

    for (int i = 0; i < sourceFiles.length; i++) {
      String last4 = sourceFiles[i]
          .substring(sourceFiles[i].length() - 4).toLowerCase();
      if (!last4.equals(".def")
          && !last4.equals(".res")
          && !last4.equals(".lib")) {
            execArgs.addElement("+" + quoteFilename(buf, sourceFiles[i]));
            objBytes += new File(sourceFiles[i]).length();
      }
    }

    for (int i = 0; i < endargs.length; i++) {
      execArgs.addElement(endargs[i]);
    }

    String[] execArguments = new String[execArgs.size()];
    execArgs.copyInto(execArguments);

    return execArguments;
  }

}
