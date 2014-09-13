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
package com.github.maven_nar.cpptasks.trolltech;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Vector;


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.gcc.LdLinker;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the Trolltech Qt MOC Compiler.
 *
 * @author Curt Arnold
 */
public final class MetaObjectCompiler
    extends CommandLineCompiler {
  /**
   * Singleton instance.
   */
  private static final MetaObjectCompiler INSTANCE = new MetaObjectCompiler(
      false, null);

  /**
   * Gets singleton instance of compiler.
   * @return MetaObjectCompiler singleton instance
   */
  public static MetaObjectCompiler getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   * @param newEnvironment boolean establish an new environment.
   * @param env Environment environment.
   */
  private MetaObjectCompiler(final boolean newEnvironment,
                             final Environment env) {
    super("moc", "-version", new String[] {".h", ".cpp"}
          , new String[0], ".moc", false, null, newEnvironment, env);
  }

  /**
   * Add arguments for debug, etc.
   * @param args Vector command argument list
   * @param debug boolean build for debug if true
   * @param multithreaded boolean build for multithreading if true
   * @param exceptions boolean enable exceptions if true
   * @param linkType LinkType output and runtime type
   * @param rtti Boolean enable run-time type identification if true
   * @param optimization OptimizationEnum optimization
   */
  protected void addImpliedArgs(final Vector<String> args,
                                final boolean debug,
                                final boolean multithreaded,
                                final boolean exceptions,
                                final LinkType linkType,
                                final Boolean rtti,
                                final OptimizationEnum optimization) {
  }

  /**
   * Add arguments for specified warning level.
   * @param args Vector command line arguments
   * @param level int warning level value
   */
  protected void addWarningSwitch(final Vector<String> args, final int level) {
  }

  /**
   * Change enviroment (deprecated).
   * @param newEnvironment boolean use new environment.
   * @param env Environment environment
   * @return Processor modified processor
   */
  public Processor changeEnvironment(final boolean newEnvironment,
                                     final Environment env) {
    return this;
  }

  /**
   * Gets a parser to scan source file for dependencies.
   * @param source source file
   * @return parser
   */
  protected Parser createParser(final File source) {
    return new CParser();
  }

  /**
   * Gets number of command line arguments per input file.
   * @return int number of command line arguments per input file.
   */
  protected int getArgumentCountPerInputFile() {
    return 3;
  }

  /**
   * Returns the bid of the processor for the file.
   *
   * @param inputFile
   *            filename of input file
   * @return bid for the file, 0 indicates no interest, 1 indicates that the
   *         processor recognizes the file but doesn't process it (header
   *         files, for example), 100 indicates strong interest
   */
  public int bid(final String inputFile) {
    //
    //   get base bid
    int baseBid = super.bid(inputFile);
    //
    //   if the base bid was non-zero (.h or .cpp extension)
    //
    if (baseBid > 0) {
      //
      //    scan the file for Q_OBJECT
      //        skip file if not present
      //
      try {
        Reader reader = new BufferedReader(new FileReader(inputFile));
        boolean hasQObject = MetaObjectParser.hasQObject(reader);
        reader.close();
        if (hasQObject) {
          return baseBid;
        }
      } catch (IOException ex) {
        return 0;
      }
    }
    return 0;
  }

  /**
   * Gets output file names.
   * @param inputFile String input file name
   * @param versionInfo version info, not used by this compiler.
   * @return String[] output file names
   */
  public String[] getOutputFileNames(final String inputFile,
                  final VersionInfo versionInfo) {
    if (inputFile.endsWith(".cpp")) {
      return super.getOutputFileNames(inputFile, versionInfo);
    }
    //
    //  if a recognized input file
    //
    String baseName = getBaseOutputName(inputFile);
    return new String[] {
        "moc_" + baseName + ".cpp"};
  }

  /**
   * Gets input file arguments.
   * @param outputDir File output directory
   * @param filename String input file name.
   * @param index int argument index,
   *         0 to getNumberOfArgumentsPerInputFile() -1
   * @return String input file argument
   */
  protected String getInputFileArgument(final File outputDir,
                                        final String filename,
                                        final int index) {
    switch (index) {
      case 0:
        return "-o";
      case 1:
        String outputFileName = getOutputFileNames(filename, null)[0];
        return new File(outputDir, outputFileName)
            .toString();

      case 2:
        return filename;

        default:
          return null;
    }
  }

  /**
   * Gets maximum length of command line.
   * @return int maximum length of command line
   */
  public int getMaximumCommandLength() {
    return 1024;
  }

  /**
   * Gets maximum number of input files processed per command.
   * @return int maximum number of input files processed per command.
   */
  protected int getMaximumInputFilesPerCommand() {
    return 1;
  }

  /**
   * Gets include directory switch.
   * @param includeDir String include directory
   * @return String command switch to add specified directory to search path
   */
  protected String getIncludeDirSwitch(final String includeDir) {
    return "";
  }

  /**
   * Gets switch to define preprocessor macro.
   * @param buffer StringBuffer command line argument
   * @param define String macro name
   * @param value String macro value, may be null.
   */
  protected void getDefineSwitch(final StringBuffer buffer,
                                 final String define,
                                 final String value) {
  }

  /**
   * Gets switch to undefine preprocessor macro.
   * @param buffer StringBuffer command line argument
   * @param define String macro name
   */
  protected void getUndefineSwitch(final StringBuffer buffer,
                                   final String define) {
  }

  /**
   * Gets standard include paths.
   * @return File[] standard include paths
   */
  protected File[] getEnvironmentIncludePath() {
    return new File[0];
  }

  /**
   * Gets linker associated with this type.
   * @param type LinkType linker, returns ld.
   * @return Linker
   */
  public Linker getLinker(final LinkType type) {
    return LdLinker.getInstance();
  }

  /**
   * Get total command line length due to the input file.
   * @param outputDir File output directory
   * @param inputFile String input file
   * @return int characters added to command line for the input file.
   */
  protected int getTotalArgumentLengthForInputFile(final File outputDir,
      final String inputFile) {
    String arg1 = getInputFileArgument(outputDir, inputFile, 0);
    String arg2 = getInputFileArgument(outputDir, inputFile, 1);
    return arg1.length() + arg2.length() + 3;
  }
}
