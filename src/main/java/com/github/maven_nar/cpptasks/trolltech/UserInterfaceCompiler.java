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

import java.io.File;
import java.util.Vector;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.compiler.ProgressMonitor;
import com.github.maven_nar.cpptasks.gcc.LdLinker;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the Trolltech Qt UIC Compiler.
 *
 * @author Curt Arnold
 */
public final class UserInterfaceCompiler
    extends CommandLineCompiler {
  /**
   * Singleton instance.
   */
  private static final UserInterfaceCompiler INSTANCE = new
      UserInterfaceCompiler(
      false, null);

  /**
   * Gets singleton instance of compiler.
   * @return MetaObjectCompiler singleton instance
   */
  public static UserInterfaceCompiler getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   * @param newEnvironment boolean establish an new environment.
   * @param env Environment environment.
   */
  private UserInterfaceCompiler(final boolean newEnvironment,
                                final Environment env) {
    super("uic", "-version", new String[] {".ui"}
          , new String[0], ".h", false, null, newEnvironment, env);
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
   * The include parser for C will work just fine, but we didn't want to
   * inherit from CommandLineCCompiler.
   * @param source source file to be parsed
   * @return parser
   */
  protected Parser createParser(final File source) {
    return new UserInterfaceParser();
  }

  /**
   * Gets number of command line arguments per input file.
   * @return int number of command line arguments per input file.
   */
  protected int getArgumentCountPerInputFile() {
    return 3;
  }

  /**
   * Gets output file names.
   * @param inputFile String input file name
   * @param versionInfo version info, not used by this compiler.
   * @return String[] output file names
   */
  public String[] getOutputFileNames(final String inputFile,
                                     final VersionInfo versionInfo) {
    //
    //  if a recognized input file
    //
    String baseName = getBaseOutputName(inputFile);
    return new String[] {
        baseName + ".h",
        baseName + ".cpp",
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
   * Compiles an .ui file into the corresponding .h, .cpp and moc_*.cpp files.
   * @param task current cc task
   * @param outputDir output directory
   * @param sourceFiles source files
   * @param args command line arguments that appear before input files
   * @param endArgs command line arguments that appear after input files
   * @param relentless if true, do not stop at first compilation error
   * @param config compiler configuration
   * @param monitor progress monitor
   */
  public void compile(final CCTask task,
                      final File outputDir,
                      final String[] sourceFiles,
                      final String[] args,
                      final String[] endArgs,
                      final boolean relentless,
                      final CommandLineCompilerConfiguration config,
                      final ProgressMonitor monitor) {

    BuildException exc = null;
    String[] thisSource = new String[1];
    String[] uicCommand = new String[args.length + endArgs.length + 4];
    uicCommand[0] = "uic";
    String[] uicImplCommand = new String[args.length + endArgs.length + 6];
    uicImplCommand[0] = "uic";
    String[] mocCommand = new String[args.length + endArgs.length + 4];
    mocCommand[0] = "moc";
    for (int i = 0; i < args.length; i++) {
      uicCommand[i + 1] = args[i];
      uicImplCommand[i + 1] = args[i];
      mocCommand[i + i] = args[i];
    }
    uicCommand[args.length + 1] = "-o";
    uicImplCommand[args.length + 1] = "-o";
    mocCommand[args.length + 1] = "-o";

    int uicIndex = args.length + 4;
    int uicImplIndex = args.length + 6;
    int mocIndex = args.length + 4;
    for (int i = 0; i < endArgs.length; i++) {
      uicCommand[uicIndex++] = endArgs[i];
      uicImplCommand[uicImplIndex++] = endArgs[i];
      mocCommand[mocIndex++] = endArgs[i];
    }
    for (int j = 0; j < sourceFiles.length; j++) {
      uicIndex = args.length + 2;
      uicImplIndex = args.length + 2;
      mocIndex = args.length + 2;
      String[] outputFileNames = getOutputFileNames(sourceFiles[j], null);

      uicCommand[uicIndex++] = outputFileNames[0];
      uicCommand[uicIndex++] = sourceFiles[j];

      uicImplCommand[uicImplIndex++] = outputFileNames[1];
      uicImplCommand[uicImplIndex++] = "-impl";
      uicImplCommand[uicImplIndex++] = outputFileNames[0];
      uicImplCommand[uicImplIndex++] = sourceFiles[j];

      mocCommand[mocIndex++] = outputFileNames[2];
      mocCommand[mocIndex++] = outputFileNames[0];

      int retval = runCommand(task, outputDir, uicCommand);
      if (retval == 0) {
        retval = runCommand(task, outputDir, uicImplCommand);
        if (retval == 0) {
          retval = runCommand(task, outputDir, mocCommand);
        }
      }
      if (monitor != null) {
        thisSource[0] = sourceFiles[j];
        monitor.progress(thisSource);
      }
      //
      //   if the process returned a failure code and
      //      we aren't holding an exception from an earlier
      //      interation
      if (retval != 0 && exc == null) {
        //
        //   construct the exception
        //
        exc = new BuildException(this.getCommand()
                                 + " failed with return code " + retval, task
                                 .getLocation());
        //
        //   and throw it now unless we are relentless
        //
        if (!relentless) {
          throw exc;
        }
      }
    }
    //
    //   if the compiler returned a failure value earlier
    //      then throw an exception
    if (exc != null) {
      throw exc;
    }
  }

  /**
   * Get total command line length due to the input file.
   * @param outputDir File output directory
   * @param inputFile String input file
   * @return int characters added to command line for the input file.
   */
  protected int getTotalArgumentLengthForInputFile(
      final File outputDir,
      final String inputFile) {
    String arg1 = getInputFileArgument(outputDir, inputFile, 1);
    String arg2 = getInputFileArgument(outputDir, inputFile, 2);
    return arg1.length() + arg2.length() + 4;
  }
}
