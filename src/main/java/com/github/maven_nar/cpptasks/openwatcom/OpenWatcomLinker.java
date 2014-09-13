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
import java.io.IOException;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.TargetMatcher;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.platforms.WindowsPlatform;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;


/**
 * Adapter for the OpenWatcom linker.
 *
 * @author Curt Arnold
 */
public abstract class OpenWatcomLinker
    extends CommandLineLinker {
  /**
   * Constructor.
   * @param command String command string (wcl386 or wfl386)
   * @param outputSuffix String output suffix
   */
  protected OpenWatcomLinker(final String command,
                             final String outputSuffix) {
    super(command, "-r", new String[] {".obj", ".lib", ".res"}
          ,
          new String[] {".map", ".pdb", ".lnk"}
          , outputSuffix, false, null);
  }

  /**
   * Add specified base address to linker options.
   * @param base long base address
   * @param args Vector command options
   */
  protected final void addBase(final long base, final Vector<String> args) {
  }

  /**
   * Adds non-default entry point.
   * @param entry entry point name
   * @param args command line parameters
   */
  protected final void addEntry(final String entry, final Vector<String> args) {
  }

  /**
   * Adds fixed option.
   * @param fixed if executable is fixed
   * @param args command line parameters
   */
  protected final void addFixed(final Boolean fixed, final Vector<String> args) {
  }

   /**
   *  Adds other command line parameters.
   * @param debug boolean is debug
   * @param linkType LinkType link type
   * @param args Vector command line arguments
   */
  protected final void addImpliedArgs(final boolean debug,
                                final LinkType linkType,
                                final Vector<String> args) {
    if (linkType.isExecutable()) {
      if (linkType.isSubsystemConsole()) {
        args.addElement("/bc");
      } else {
        if (linkType.isSubsystemGUI()) {
          args.addElement("/bg");
        }
      }
    }
    if (linkType.isSharedLibrary()) {
      args.addElement("/bd");
    }
  }

  /**
   * Add command line switch to force incremental linking.
   * @param incremental boolean do incremental linking
   * @param args Vector command line arguments
   */
  protected final void addIncremental(final boolean incremental,
                                      final Vector<String> args) {
  }

  /**
   * Add command line switch to force map generation.
   * @param map boolean build map
   * @param args Vector command line arguments
   */
  protected final void addMap(final boolean map, final Vector<String> args) {
    if (map) {
      args.addElement("/fm");
    }
  }

  /**
   * Add command line switch for stack reservation.
   * @param stack int stack size.
   * @param args Vector command line arguments.
   */
  protected final void addStack(final int stack, final Vector<String> args) {
    if (stack >= 0) {
      String stackStr = Integer.toString(stack);
      args.addElement("/k" + stackStr);
    }
  }

  /**
   * Adds source or object files to the bidded fileset to
   * support version information.
   *
   * @param versionInfo version information
   * @param linkType link type
   * @param isDebug true if debug build
   * @param outputFile name of generated executable
   * @param objDir directory for generated files
   * @param matcher bidded fileset
   * @throws IOException if unable to write version resource
   */
  public final void addVersionFiles(final VersionInfo versionInfo,
                              final LinkType linkType,
                              final File outputFile,
                              final boolean isDebug,
                              final File objDir,
                              final TargetMatcher matcher) throws IOException {
    WindowsPlatform.addVersionFiles(versionInfo, linkType, outputFile, isDebug,
                                    objDir, matcher);
  }

  /**
   * Get command file switch.
   * @param commandFile String command file name
   * @return String command line option
   */
  public final String getCommandFileSwitch(final String commandFile) {
    return "@" + commandFile;
  }

  /**
   * Get search path for libraries.
   * @return File[] library path
   */
  public final File[] getLibraryPath() {
    return CUtil.getPathFromEnvironment("LIB", ";");
  }

  /**
   * Get file selectors for libraries.
   * @param libnames String[]
   * @param libType LibraryTypeEnum
   * @return String[]
   */
  public final String[] getLibraryPatterns(final String[] libnames,
                                           final LibraryTypeEnum libType) {
    return OpenWatcomProcessor.getLibraryPatterns(libnames, libType);
  }

  /**
   * Get maximum command line length.
   * @return int command line length
   */
  public final int getMaximumCommandLength() {
    return 1024;
  }

  /**
   * Get output file switch.
   * @param outFile Output file name
   * @return String[] command line switches
   */
  public final String[] getOutputFileSwitch(final String outFile) {
    return OpenWatcomProcessor.getOutputFileSwitch(outFile);
  }

  /**
   * Gets file name sensitivity of processors.
   * @return boolean true if case sensitive.
   */
  public final boolean isCaseSensitive() {
    return OpenWatcomProcessor.isCaseSensitive();
  }

}
