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
package com.github.maven_nar.cpptasks.mozilla;

import java.io.File;
import java.util.Vector;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.compiler.ProgressMonitor;
import com.github.maven_nar.cpptasks.gcc.LdLinker;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the Mozilla Xpidl Compiler.
 *
 * @author Curt Arnold
 */
public final class XpidlCompiler
    extends CommandLineCompiler {
  /**
   * Singleton instance.
   */
  private static final XpidlCompiler INSTANCE = new XpidlCompiler(
      false, null);
  /**
   * Gets singleton instance of compiler.
   * @return XpidlCompiler singleton instance
   */
  public static XpidlCompiler getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   * @param newEnvironment boolean establish an new environment.
   * @param env Environment environment.
   */
  private XpidlCompiler(final boolean newEnvironment,
                        final Environment env) {
    super("xpidl", null, new String[] {".idl", ".xpidl"}
          , new String[0], ".xpt", false, null, newEnvironment, env);
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
  protected void addWarningSwitch(final Vector<String> args,
                                  final int level) {
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
   * Gets dependency parser.
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
        baseName + ".xpt",
        baseName + ".h"};
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
    return "";
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
   * Adds command line arguments for include paths.
   *
   * @param baseDirPath String base directory
   * @param includeDirs File[] include directories
   * @param args Vector command line arguments
   * @param relativeArgs Vector arguments for configuration identification
   * @param includePathId StringBuffer buffer for configuration identification
   */
  protected void addIncludes(final String baseDirPath,
                             final File[] includeDirs,
                             final Vector<String> args,
                             final Vector<String> relativeArgs,
                             final StringBuffer includePathId) {
    //
    //   requires space between switch and path
    //
    for (int i = 0; i < includeDirs.length; i++) {
      args.addElement("-I");
      args.addElement(includeDirs[i].getAbsolutePath());
      if (relativeArgs != null) {
        String relative = CUtil.getRelativePath(baseDirPath,
                                                includeDirs[i]);
        relativeArgs.addElement("-I");
        relativeArgs.addElement(relative);
        if (includePathId != null) {
          if (includePathId.length() == 0) {
            includePathId.append("-I ");
          } else {
            includePathId.append(" -I ");
          }
          includePathId.append(relative);
        }
      }
    }
  }

  /**
   * Gets include directory switch.
   * @param includeDir String include directory
   * @return String command switch to add specified directory to search path
   */
  protected String getIncludeDirSwitch(final String includeDir) {
    return "-I" + includeDir;
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
   * Compiles an .idl file into the corresponding .h and .xpt files.
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
                      final ProgressMonitor monitor)  {

    BuildException exc = null;
    String[] thisSource = new String[1];
    String[] tlbCommand = new String[args.length + endArgs.length + 6];
    tlbCommand[0] = "xpidl";
    tlbCommand[1] = "-m";
    tlbCommand[2] = "typelib";
    String[] headerCommand = new String[args.length + endArgs.length + 6];
    headerCommand[0] = "xpidl";
    headerCommand[1] = "-m";
    headerCommand[2] = "header";
    for (int i = 0; i < args.length; i++) {
      tlbCommand[i + 3] = args[i];
      headerCommand[i + 3] = args[i];
    }
    tlbCommand[args.length + 3] = "-e";
    headerCommand[args.length + 3] = "-e";

    int tlbIndex = args.length + 6;
    int headerIndex = args.length + 6;
    for (int i = 0; i < endArgs.length; i++) {
      tlbCommand[tlbIndex++] = endArgs[i];
      headerCommand[headerIndex++] = endArgs[i];
    }
    for (int j = 0; j < sourceFiles.length; j++) {
      tlbIndex = args.length + 4;
      headerIndex = args.length + 4;
      String[] outputFileNames = getOutputFileNames(sourceFiles[j], null);

      tlbCommand[tlbIndex++] = outputFileNames[0];
      tlbCommand[tlbIndex++] = sourceFiles[j];

      headerCommand[headerIndex++] = outputFileNames[1];
      headerCommand[headerIndex++] = sourceFiles[j];

      int retval = runCommand(task, outputDir, tlbCommand);
      if (retval == 0) {
        retval = runCommand(task, outputDir, headerCommand);
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
    return 0;
  }

  /**
   * Gets compiler identifier.
   * @return String compiler identification string
   */
  public String getIdentifier() {
    return "Mozilla xpidl";
  }

}
