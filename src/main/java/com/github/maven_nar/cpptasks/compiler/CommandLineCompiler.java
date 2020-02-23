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
package com.github.maven_nar.cpptasks.compiler;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;
import org.apache.commons.io.FilenameUtils;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.TargetDef;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.types.CommandLineArgument;
import com.github.maven_nar.cpptasks.types.UndefineArgument;
import com.google.common.collect.ObjectArrays;
import org.apache.tools.ant.util.FileUtils;

/**
 * An abstract Compiler implementation which uses an external program to
 * perform the compile.
 *
 * @author Adam Murdoch
 */
public abstract class CommandLineCompiler extends AbstractCompiler {
  /** Command used when invoking ccache */
  private static final String CCACHE_CMD = "ccache";
  private String command;
  private String prefix;
  private final Environment env;
  private String identifier;
  private final String identifierArg;
  private final boolean libtool;
  private final CommandLineCompiler libtoolCompiler;
  private final boolean newEnvironment;
  private String fortifyID="";

  protected CommandLineCompiler(final String command, final String identifierArg, final String[] sourceExtensions,
      final String[] headerExtensions,
      final String outputSuffix, final boolean libtool, final CommandLineCompiler libtoolCompiler,
      final boolean newEnvironment, final Environment env) {
    super(sourceExtensions, headerExtensions, outputSuffix);
    this.command = command;
    if (libtool && libtoolCompiler != null) {
      throw new java.lang.IllegalArgumentException("libtoolCompiler should be null when libtool is true");
    }
    this.libtool = libtool;
    this.libtoolCompiler = libtoolCompiler;
    this.identifierArg = identifierArg;
    this.newEnvironment = newEnvironment;
    this.env = env;
  }

  abstract protected void addImpliedArgs(Vector<String> args, boolean debug, boolean multithreaded, boolean exceptions,
      LinkType linkType, Boolean rtti, OptimizationEnum optimization);

  /**
   * Adds command-line arguments for include directories.
   * 
   * If relativeArgs is not null will add corresponding relative paths
   * include switches to that vector (for use in building a configuration
   * identifier that is consistent between machines).
   * 
   * @param baseDirPath
   *          Base directory path.
   * @param includeDirs
   *          Array of include directory paths
   * @param args
   *          Vector of command line arguments used to execute the task
   * @param relativeArgs
   *          Vector of command line arguments used to build the
   *          configuration identifier
   */
  protected void addIncludes(final String baseDirPath, final File[] includeDirs, final Vector<String> args,
      final Vector<String> relativeArgs, final StringBuffer includePathId, final boolean isSystem) {
    for (final File includeDir : includeDirs) {
      args.addElement(getIncludeDirSwitch(includeDir.getAbsolutePath(), isSystem));
      if (relativeArgs != null) {
        final String relative = CUtil.getRelativePath(baseDirPath, includeDir);
        relativeArgs.addElement(getIncludeDirSwitch(relative, isSystem));
        if (includePathId != null) {
          if (includePathId.length() == 0) {
            includePathId.append("/I");
          } else {
            includePathId.append(" /I");
          }
          includePathId.append(relative);
        }
      }
    }
  }

  abstract protected void addWarningSwitch(Vector<String> args, int warnings);

  protected void buildDefineArguments(final CompilerDef[] defs, final Vector<String> args) {
    //
    // assume that we aren't inheriting defines from containing <cc>
    //
    UndefineArgument[] merged = defs[0].getActiveDefines();
    for (int i = 1; i < defs.length; i++) {
      //
      // if we are inheriting, merge the specific defines with the
      // containing defines
      merged = UndefineArgument.merge(defs[i].getActiveDefines(), merged);
    }
    final StringBuffer buf = new StringBuffer(30);
    for (final UndefineArgument current : merged) {
      buf.setLength(0);
      if (current.isDefine()) {
        getDefineSwitch(buf, current.getName(), current.getValue());
      } else {
        getUndefineSwitch(buf, current.getName());
      }
      args.addElement(buf.toString());
    }
  }

  @Override
  public String[] getOutputFileNames(final String inputFile, final VersionInfo versionInfo) {
    //
    // if a recognized input file
    //
    if (bid(inputFile) > 1) {
      final String baseName = getBaseOutputName(inputFile);
      final File standardisedFile = new File(inputFile);
      try {
        return new String[] {
          baseName + FilenameUtils.EXTENSION_SEPARATOR + Integer.toHexString(standardisedFile.getCanonicalPath().hashCode()) + getOutputSuffix()
        };
      } catch (IOException e) {
        throw new BuildException("Source file not found", e);
      }
    }
    return new String[0];
  }

  /**
   * Compiles a source file.
   * 
   */
  public void compile(final CCTask task, final File outputDir, final String[] sourceFiles, String[] args,
      final String[] endArgs, final boolean relentless, final CommandLineCompilerConfiguration config,
      final ProgressMonitor monitor) throws BuildException {
    BuildException exc = null;
    //
    // determine length of executable name and args
    //
    String command = getCommandWithPath(config);
    if (config.isUseCcache()) {
      // Replace the command with "ccache" and push the old compiler
      // command into the args.
      final String compilerCommand = command;
      command = CCACHE_CMD;
      args = ObjectArrays.concat(compilerCommand, args);
    }
    int baseLength = command.length() + args.length + endArgs.length;
    if (this.libtool) {
      baseLength += 8;
    }
    for (final String arg : args) {
      baseLength += arg.length();
    }
    for (final String endArg : endArgs) {
      baseLength += endArg.length();
    }
    if (baseLength > getMaximumCommandLength()) {
      throw new BuildException("Command line is over maximum length without specifying source file");
    }
    //
    // typically either 1 or Integer.MAX_VALUE
    //
    final int maxInputFilesPerCommand = getMaximumInputFilesPerCommand();
    final int argumentCountPerInputFile = getArgumentCountPerInputFile();
    for (int sourceIndex = 0; sourceIndex < sourceFiles.length;) {
      int cmdLength = baseLength;
      int firstFileNextExec;
      for (firstFileNextExec = sourceIndex; firstFileNextExec < sourceFiles.length
          && firstFileNextExec - sourceIndex < maxInputFilesPerCommand; firstFileNextExec++) {
        cmdLength += getTotalArgumentLengthForInputFile(outputDir, sourceFiles[firstFileNextExec]);
        if (cmdLength >= getMaximumCommandLength()) {
          break;
        }
      }
      if (firstFileNextExec == sourceIndex) {
        throw new BuildException("Extremely long file name, can't fit on command line");
      }

      ArrayList<String> commandlinePrefix = new ArrayList<>();
      if (this.libtool) {
        commandlinePrefix.add("libtool");
      }
      if((this.fortifyID !=null) && (!this.fortifyID.equals("")))
      {// If FortifyID attribute was set, run the Fortify framework

        commandlinePrefix.add("sourceanalyzer");
        commandlinePrefix.add("-b");
        commandlinePrefix.add(this.fortifyID);
      }
      commandlinePrefix.add(command);
      Collections.addAll(commandlinePrefix, args);

      int retval = 0;
      for (int j = sourceIndex; j < firstFileNextExec; j++) {
        ArrayList<String> commandlineSuffix = new ArrayList<>();

        for (int k = 0; k < argumentCountPerInputFile; k++) {
          commandlineSuffix.add(getInputFileArgument(outputDir, sourceFiles[j], k));
        }
        Collections.addAll(commandlineSuffix, endArgs);

        ArrayList<String> commandline = new ArrayList<>(commandlinePrefix);
        commandline.addAll(commandlineSuffix);
        final int ret = runCommand(task, workDir,
            commandline.toArray(new String[commandline.size()]));
        if (ret != 0) { retval = ret; }
      }
      if (monitor != null) {
        final String[] fileNames = new String[firstFileNextExec - sourceIndex];

        System.arraycopy(sourceFiles, sourceIndex + 0, fileNames, 0, fileNames.length);
        monitor.progress(fileNames);
      }
      //
      // if the process returned a failure code and
      // we aren't holding an exception from an earlier
      // interation
      if (retval != 0 && exc == null) {
        //
        // construct the exception
        //
        exc = new BuildException(getCommandWithPath(config) + " failed with return code " + retval, task.getLocation());

        //
        // and throw it now unless we are relentless
        //
        if (!relentless) {
          throw exc;
        }
      }
      sourceIndex = firstFileNextExec;
    }
    //
    // if the compiler returned a failure value earlier
    // then throw an exception
    if (exc != null) {
      throw exc;
    }
  }

  @Override
  protected CompilerConfiguration createConfiguration(final CCTask task, final LinkType linkType,
      final ProcessorDef[] baseDefs, final CompilerDef specificDef, final TargetDef targetPlatform,
      final VersionInfo versionInfo) {

    this.prefix = specificDef.getCompilerPrefix();
    this.objDir = task.getObjdir();
    final Vector<String> args = new Vector<>();
    final CompilerDef[] defaultProviders = new CompilerDef[baseDefs.length + 1];
    for (int i = 0; i < baseDefs.length; i++) {
      defaultProviders[i + 1] = (CompilerDef) baseDefs[i];
    }
    defaultProviders[0] = specificDef;
    final Vector<CommandLineArgument> cmdArgs = new Vector<>();

    //
    // add command line arguments inherited from <cc> element
    // any "extends" and finally the specific CompilerDef
    CommandLineArgument[] commandArgs;
    for (int i = defaultProviders.length - 1; i >= 0; i--) {
      commandArgs = defaultProviders[i].getActiveProcessorArgs();
      for (final CommandLineArgument commandArg : commandArgs) {
        if (commandArg.getLocation() == 0) {
          String arg = commandArg.getValue();
          if (isWindows() && arg.matches(".*[ \"].*")) {
            // Work around inconsistent quoting by Ant
            arg = "\"" + arg.replaceAll("[\\\\\"]", "\\\\$0") + "\"";
          }
          args.addElement(arg);
        } else {
          cmdArgs.addElement(commandArg);
        }
      }
    }
    final Vector<ProcessorParam> params = new Vector<>();
    //
    // add command line arguments inherited from <cc> element
    // any "extends" and finally the specific CompilerDef
    ProcessorParam[] paramArray;
    for (int i = defaultProviders.length - 1; i >= 0; i--) {
      paramArray = defaultProviders[i].getActiveProcessorParams();
      Collections.addAll(params, paramArray);
    }
    paramArray = params.toArray(new ProcessorParam[params.size()]);

    if (specificDef.isClearDefaultOptions() == false) {
      final boolean multithreaded = specificDef.getMultithreaded(defaultProviders, 1);
      final boolean debug = specificDef.getDebug(baseDefs, 0);
      final boolean exceptions = specificDef.getExceptions(defaultProviders, 1);
      final Boolean rtti = specificDef.getRtti(defaultProviders, 1);
      final OptimizationEnum optimization = specificDef.getOptimization(defaultProviders, 1);
      this.addImpliedArgs(args, debug, multithreaded, exceptions, linkType, rtti, optimization);
    }

    //
    // add all appropriate defines and undefines
    //
    buildDefineArguments(defaultProviders, args);
    final int warnings = specificDef.getWarnings(defaultProviders, 0);
    addWarningSwitch(args, warnings);
    Enumeration<CommandLineArgument> argEnum = cmdArgs.elements();
    int endCount = 0;
    while (argEnum.hasMoreElements()) {
      final CommandLineArgument arg = argEnum.nextElement();
      switch (arg.getLocation()) {
        case 1:
          args.addElement(arg.getValue());
          break;
        case 2:
          endCount++;
          break;
      }
    }
    final String[] endArgs = new String[endCount];
    argEnum = cmdArgs.elements();
    int index = 0;
    while (argEnum.hasMoreElements()) {
      final CommandLineArgument arg = argEnum.nextElement();
      if (arg.getLocation() == 2) {
        endArgs[index++] = arg.getValue();
      }
    }
    //
    // Want to have distinct set of arguments with relative
    // path names for includes that are used to build
    // the configuration identifier
    //
    final Vector<String> relativeArgs = (Vector) args.clone();
    //
    // add all active include and sysincludes
    //
    final StringBuffer includePathIdentifier = new StringBuffer();
    final File baseDir = specificDef.getProject().getBaseDir();
    String baseDirPath;
    try {
      baseDirPath = baseDir.getCanonicalPath();
    } catch (final IOException ex) {
      baseDirPath = baseDir.toString();
    }
    final Vector<String> includePath = new Vector<>();
    final Vector<String> sysIncludePath = new Vector<>();
    for (int i = defaultProviders.length - 1; i >= 0; i--) {
      String[] incPath = defaultProviders[i].getActiveIncludePaths();
      for (final String element : incPath) {
        includePath.addElement(element);
      }
      incPath = defaultProviders[i].getActiveSysIncludePaths();
      for (final String element : incPath) {
        sysIncludePath.addElement(element);
      }
    }
    final File[] incPath = new File[includePath.size()];
    for (int i = 0; i < includePath.size(); i++) {
      incPath[i] = new File(includePath.elementAt(i));
    }
    final File[] sysIncPath = new File[sysIncludePath.size()];
    for (int i = 0; i < sysIncludePath.size(); i++) {
      sysIncPath[i] = new File(sysIncludePath.elementAt(i));
    }
    addIncludes(baseDirPath, incPath, args, relativeArgs, includePathIdentifier, false);
    addIncludes(baseDirPath, sysIncPath, args, null, null, true);
    final StringBuffer buf = new StringBuffer(getIdentifier());
    for (int i = 0; i < relativeArgs.size(); i++) {
      buf.append(' ');
      buf.append(relativeArgs.elementAt(i));
    }
    for (final String endArg : endArgs) {
      buf.append(' ');
      buf.append(endArg);
    }
    final String configId = buf.toString();
    final String[] argArray = new String[args.size()];
    args.copyInto(argArray);
    final boolean rebuild = specificDef.getRebuild(baseDefs, 0);
    final File[] envIncludePath = getEnvironmentIncludePath();
    final String path = specificDef.getToolPath();

    CommandLineCompiler compiler = this;

    Environment environment = specificDef.getEnv();
    if (environment == null) {
      for (final ProcessorDef baseDef : baseDefs) {
        environment = baseDef.getEnv();
        if (environment != null) {
          compiler = (CommandLineCompiler) compiler.changeEnvironment(baseDef.isNewEnvironment(), environment);
        }
      }
    } else {
      compiler = (CommandLineCompiler) compiler.changeEnvironment(specificDef.isNewEnvironment(), environment);
    }
    // Pass the fortifyID for compiler
    compiler.fortifyID = specificDef.getFortifyID();

    return new CommandLineCompilerConfiguration(compiler, configId, incPath, sysIncPath, envIncludePath,
        includePathIdentifier.toString(), argArray, paramArray, rebuild, endArgs, path, specificDef.getCcache());
  }

  protected int getArgumentCountPerInputFile() {
    return 1;
  }

  protected final String getCommand() {
    if (this.prefix != null && (!this.prefix.isEmpty())) {
      return this.prefix + this.command;
    } else {
      return this.command;
    }
  }

  public String getCommandWithPath(final CommandLineCompilerConfiguration config) {
    if (config.getCommandPath() != null) {
      final File command = new File(config.getCommandPath(), this.getCommand());
      try {
        return command.getCanonicalPath();
      } catch (final IOException e) {
        e.printStackTrace();
        return command.getAbsolutePath();
      }
    } else {
      return this.getCommand();
    }
  }

  abstract protected void getDefineSwitch(StringBuffer buffer, String define, String value);

  protected abstract File[] getEnvironmentIncludePath();

  @Override
  public String getIdentifier() {
    if (this.identifier == null) {
      if (this.identifierArg == null) {
        this.identifier = getIdentifier(new String[] {
          this.getCommand()
        }, this.getCommand());
      } else {
        this.identifier = getIdentifier(new String[] {
          this.getCommand(), this.identifierArg
        }, this.getCommand());
      }
    }
    return this.identifier;
  }

  abstract protected String getIncludeDirSwitch(String source);

  /**
   * Added by Darren Sargent 22Oct2008 Returns the include dir switch value.
   * Default implementation doesn't treat system includes specially, for
   * compilers which don't care.
   * 
   * @param source
   *          the given source value.
   * @param isSystem
   *          "true" if this is a system include path
   * 
   * @return the include dir switch value.
   */
  protected String getIncludeDirSwitch(final String source, final boolean isSystem) {
    return getIncludeDirSwitch(source);
  }

  protected String getInputFileArgument(final File outputDir, final String filename, final int index) {
    //
    // if there is an embedded space,
    // must enclose in quotes
    String relative="";
    String inputFile;
    try {
      relative = FileUtils.getRelativePath(workDir, new File(filename));
    } catch (Exception ex) {
    }
    if (relative.isEmpty()) {
      inputFile = filename;
    } else {
      inputFile = relative;
    }
    if (inputFile.indexOf(' ') >= 0) {
      final String buf = "\"" + inputFile +
          "\"";
      return buf;
    }
    return inputFile;
  }

  protected final boolean getLibtool() {
    return this.libtool;
  }

  /**
   * Obtains the same compiler, but with libtool set
   * 
   * Default behavior is to ignore libtool
   */
  public final CommandLineCompiler getLibtoolCompiler() {
    if (this.libtoolCompiler != null) {
      return this.libtoolCompiler;
    }
    return this;
  }

  abstract public int getMaximumCommandLength();

  protected int getMaximumInputFilesPerCommand() {
    return Integer.MAX_VALUE;
  }

  /**
   * Get total command line length due to the input file.
   * 
   * @param outputDir
   *          File output directory
   * @param inputFile
   *          String input file
   * @return int characters added to command line for the input file.
   */
  protected int getTotalArgumentLengthForInputFile(final File outputDir, final String inputFile) {
    final int argumentCountPerInputFile = getArgumentCountPerInputFile();
    int len=0;
    for (int k = 0; k < argumentCountPerInputFile; k++) {
      len+=getInputFileArgument(outputDir, inputFile, k).length();
    }
    return len + argumentCountPerInputFile; // argumentCountPerInputFile added for spaces
  }

  abstract protected void getUndefineSwitch(StringBuffer buffer, String define);

  /**
   * This method is exposed so test classes can overload and test the
   * arguments without actually spawning the compiler
   */
  protected int runCommand(final CCTask task, final File workingDir, final String[] cmdline) throws BuildException {
    return CUtil.runCommand(task, workingDir, cmdline, this.newEnvironment, this.env);
  }

  protected final void setCommand(final String command) {
    this.command = command;
  }
}
