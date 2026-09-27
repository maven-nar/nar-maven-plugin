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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.ReplayCommand;
import com.github.maven_nar.ReplayCommandList;
import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.TargetDef;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.types.CommandLineArgument;
import com.github.maven_nar.cpptasks.types.LibrarySet;

/**
 * An abstract Linker implementation that performs the link via an external
 * command.
 *
 * @author Adam Murdoch
 */
public abstract class CommandLineLinker extends AbstractLinker {
  private String command;
  private String prefix;
  private Environment env = null;
  private String identifier;
  private final String identifierArg;
  private final boolean isLibtool;
  private String[] librarySets;
  private final CommandLineLinker libtoolLinker;
  private final boolean newEnvironment = false;
  private final String outputSuffix;
  private List<String[]> commands;
  private boolean dryRun;

  // FREEHEP
  private final int maxPathLength = 250;

  /** Creates a comand line linker invocation */
  public CommandLineLinker(final String command, final String identifierArg, final String[] extensions,
      final String[] ignoredExtensions, final String outputSuffix, final boolean isLibtool,
      final CommandLineLinker libtoolLinker) {
    super(extensions, ignoredExtensions);
    this.command = command;
    this.identifierArg = identifierArg;
    this.outputSuffix = outputSuffix;
    this.isLibtool = isLibtool;
    this.libtoolLinker = libtoolLinker;
  }

  protected void addBase(final CCTask task, final long base, final Vector<String> args) {
    // NB: Do nothing by default.
  }

  protected void addEntry(final CCTask task, final String entry, final Vector<String> args) {
    // NB: Do nothing by default.
  }

  protected void addFixed(final CCTask task, final Boolean fixed, final Vector<String> args) {
    // NB: Do nothing by default.
  }

  protected void addImpliedArgs(final CCTask task, final boolean debug, final LinkType linkType,
      final Vector<String> args) {
    // NB: Do nothing by default.
  }

  protected void addIncremental(final CCTask task, final boolean incremental, final Vector<String> args) {
    // NB: Do nothing by default.
  }

  protected void addLibraryDirectory(final File libraryDirectory, final Vector<String> preargs) {
    try {
      if (libraryDirectory != null && libraryDirectory.exists()) {
        final File currentDir = new File(".").getParentFile();
        String path = libraryDirectory.getCanonicalPath();
        if (currentDir != null) {
          final String currentPath = currentDir.getCanonicalPath();
          path = CUtil.getRelativePath(currentPath, libraryDirectory);
        }
        addLibraryPath(preargs, path);
      }
    } catch (final IOException e) {
      throw new RuntimeException("Unable to add library path: " + libraryDirectory);
    }
  }

  protected void addLibraryPath(final Vector<String> preargs, final String path) {
  }

  //
  // Windows processors handle these through file list
  //
  protected String[] addLibrarySets(final CCTask task, final LibrarySet[] libsets, final Vector<String> preargs,
      final Vector<String> midargs, final Vector<String> endargs) {
    return null;
  }

  protected void addMap(final CCTask task, final boolean map, final Vector<String> args) {
    // NB: Do nothing by default.
  }

  /**
   * Returns the expected map output, or null when this adapter does not manage a map file.
   */
  protected File getMapFile(final File outputFile, final boolean map) {
    return null;
  }

  /**
   * Map arguments for a filename relative to the output directory. Returned arguments
   * must already include any forwarding required by the compiler driver.
   */
  protected String[] getMapFileSwitch(final CCTask task, final String mapFile) {
    return new String[0];
  }

  protected void addStack(final CCTask task, final int stack, final Vector<String> args) {
    // NB: Do nothing by default.
  }

  @Override
  protected LinkerConfiguration createConfiguration(final CCTask task, final LinkType linkType,
      final ProcessorDef[] baseDefs, final LinkerDef specificDef, final TargetDef targetPlatform,
      final VersionInfo versionInfo) {

    final Vector<String> preargs = new Vector<>();
    final Vector<String> midargs = new Vector<>();
    final Vector<String> endargs = new Vector<>();
    final Vector<String>[] args = new Vector[] {
        preargs, midargs, endargs
    };

    this.prefix  = specificDef.getLinkerPrefix();

    final LinkerDef[] defaultProviders = new LinkerDef[baseDefs.length + 1];
    defaultProviders[0] = specificDef;
    for (int i = 0; i < baseDefs.length; i++) {
      defaultProviders[i + 1] = (LinkerDef) baseDefs[i];
    }

    //
    // add command line arguments inherited from <cc> element
    // any "extends" and finally the specific CompilerDef
    CommandLineArgument[] commandArgs;
    for (int i = defaultProviders.length - 1; i >= 0; i--) {
      final LinkerDef linkerDef = defaultProviders[i];
      commandArgs = linkerDef.getActiveProcessorArgs();
      for (final CommandLineArgument commandArg : commandArgs) {
        args[commandArg.getLocation()].addElement(commandArg.getValue());
      }
    }

    final Set<File> libraryDirectories = new LinkedHashSet<>();
    for (int i = defaultProviders.length - 1; i >= 0; i--) {
      final LinkerDef linkerDef = defaultProviders[i];
      for (final File libraryDirectory : linkerDef.getLibraryDirectories()) {
        if (libraryDirectories.add(libraryDirectory)) {
          addLibraryDirectory(libraryDirectory, preargs);
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

    final boolean debug = specificDef.getDebug(baseDefs, 0);

    final String startupObject = getStartupObject(linkType);
    final boolean map = specificDef.getMap(defaultProviders, 1);

    addImpliedArgs(task, debug, linkType, preargs);
    addIncremental(task, specificDef.getIncremental(defaultProviders, 1), preargs);
    addFixed(task, specificDef.getFixed(defaultProviders, 1), preargs);
    addMap(task, map, preargs);
    addBase(task, specificDef.getBase(defaultProviders, 1), preargs);
    addStack(task, specificDef.getStack(defaultProviders, 1), preargs);
    addEntry(task, specificDef.getEntry(defaultProviders, 1), preargs);

    String[] libnames = null;
    final LibrarySet[] libsets = specificDef.getActiveLibrarySets(defaultProviders, 1);
    // FREEHEP call at all times
    // if (libsets.length > 0) {
    libnames = addLibrarySets(task, libsets, preargs, midargs, endargs);
    // }

    final StringBuffer buf = new StringBuffer(getIdentifier());
    for (int i = 0; i < 3; i++) {
      final Enumeration<String> argenum = args[i].elements();
      while (argenum.hasMoreElements()) {
        buf.append(' ');
        buf.append(argenum.nextElement());
      }
    }
    // Keep map generation in the build history even when its arguments depend on the final output name.
    if (map) {
      buf.append(" [map]");
      // Include the target's map syntax, but never a random temporary filename, in the history.
      for (final String argument : getMapFileSwitch(task, "output.map")) {
        buf.append(' ').append(argument);
      }
    }
    final String configId = buf.toString();

    final String[][] options = new String[][] {
        new String[args[0].size() + args[1].size()], new String[args[2].size()]
    };
    args[0].copyInto(options[0]);
    final int offset = args[0].size();
    for (int i = 0; i < args[1].size(); i++) {
      options[0][i + offset] = args[1].elementAt(i);
    }
    args[2].copyInto(options[1]);

    // if this linker doesn't have an env, and there is a more generically
    // definition for environment, use it.
    if (null != specificDef.getEnv() && null == this.env) {
      this.env = specificDef.getEnv();
    }
    for (final ProcessorDef processorDef : baseDefs) {
      final Environment environment = processorDef.getEnv();
      if (null != environment && null == this.env) {
        this.env = environment;
      }
    }
    final boolean rebuild = specificDef.getRebuild(baseDefs, 0);
    final String toolPath = specificDef.getToolPath();
    
    setCommands(specificDef.getCommands());
    setDryRun(specificDef.isDryRun());

    // task.log("libnames:"+libnames.length, Project.MSG_VERBOSE);
    return new CommandLineLinkerConfiguration(this, configId, options, paramArray, rebuild, map, debug, libnames,
        startupObject, toolPath);
  }

  /**
   * Allows drived linker to decorate linker option.
   * Override by GccLinker to prepend a "-Wl," to
   * pass option to through gcc to linker.
   *
   * @param buf
   *          buffer that may be used and abused in the decoration process,
   *          must not be null.
   * @param arg
   *          linker argument
   */
  protected String decorateLinkerOption(final StringBuffer buf, final String arg) {
    return arg;
  }

  protected final String getCommand() {
    if (this.prefix != null && (!this.prefix.isEmpty())) {
      return this.prefix + this.command;
    } else {
      return this.command;
    }
  }

  protected abstract String getCommandFileSwitch(String commandFile);

  public String getCommandWithPath(final CommandLineLinkerConfiguration config) {
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

  public final CommandLineLinker getLibtoolLinker() {
    if (this.libtoolLinker != null) {
      return this.libtoolLinker;
    }
    return this;
  }

  protected abstract int getMaximumCommandLength();

  @Override
  public String[] getOutputFileNames(final String baseName, final VersionInfo versionInfo) {
    return new String[] {
      baseName + this.outputSuffix
    };
  }

  protected String[] getOutputFileSwitch(final CCTask task, final String outputFile) {
    // FREEHEP BEGIN
    if (isWindows() && outputFile.length() > this.maxPathLength) {
      throw new BuildException("Absolute path too long, " + outputFile.length() + " > " + this.maxPathLength + ": '"
          + outputFile);
    }
    // FREEHEP END
    return getOutputFileSwitch(outputFile);
  }

  protected abstract String[] getOutputFileSwitch(String outputFile);

  protected String getStartupObject(final LinkType linkType) {
    return null;
  }

  /**
   * Performs a link using a command line linker
   *
   */
  public void link(final CCTask task, final File outputFile, final String[] sourceFiles,
      final CommandLineLinkerConfiguration config) throws BuildException {
    final File parentDir = new File(outputFile.getParent());
    String parentPath;
    try {
      parentPath = parentDir.getCanonicalPath();
    } catch (final IOException ex) {
      parentPath = parentDir.getAbsolutePath();
    }
    final File mapFile = config.getMapFile(outputFile);
    File temporaryMap = null;
    String temporaryMapName = null;
    try {
      CommandLineLinkerConfiguration invocation = config;
      if (mapFile != null) {
        // GNU ld expands '%' in map paths. Pass only a safe basename, then publish under
        // the literal final name after success. A failed link must not replace an old map.
        if (!isDryRun()) {
          temporaryMap = File.createTempFile("nar-map-", ".tmp", parentDir);
        }
        temporaryMapName = temporaryMap == null ? "nar-map-" + UUID.randomUUID() + ".tmp" : temporaryMap.getName();
        invocation = config.withMapFileName(temporaryMapName);
      }
      String[] execArgs = prepareArguments(task, parentPath, outputFile.getName(), sourceFiles, invocation);
      int commandLength = 0;
      for (final String execArg : execArgs) {
        commandLength += execArg.length() + 1;
      }

      // Use a response file when the command exceeds the linker's limit.
      if (commandLength >= this.getMaximumCommandLength()) {
        execArgs = prepareResponseFile(outputFile, execArgs);
      }

      recordCommand(parentDir, execArgs, temporaryMapName, mapFile == null ? null : mapFile.getName());
      final int retval = runCommand(task, parentDir, execArgs);
      if (retval != 0) {
        throw new BuildException(getCommandWithPath(config) + " failed with return code " + retval, task.getLocation());
      }
      if (temporaryMap != null) {
        if (!temporaryMap.isFile() || temporaryMap.length() == 0) {
          throw new BuildException("Linker did not produce the requested map file: " + mapFile, task.getLocation());
        }
        Files.move(temporaryMap.toPath(), mapFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (final IOException ex) {
      throw new BuildException(ex, task.getLocation());
    } finally {
      if (temporaryMap != null && temporaryMap.exists() && !temporaryMap.delete()) {
        task.log("Could not remove temporary linker map: " + temporaryMap, org.apache.tools.ant.Project.MSG_WARN);
      }
    }

  }

  /**
   * Prepares argument list for exec command. Will return null
   * if command line would exceed allowable command line buffer.
   *
   * @param task
   *          compilation task.
   * @param outputFile
   *          linker output file
   * @param sourceFiles
   *          linker input files (.obj, .o, .res)
   * @param config
   *          linker configuration
   * @return arguments for runTask
   */
  protected String[] prepareArguments(final CCTask task, final String outputDir, final String outputFile,
      final String[] sourceFiles, final CommandLineLinkerConfiguration config) {

    final String[] preargs = config.getPreArguments();
    final String[] endargs = config.getEndArguments();
    final String outputSwitch[] = getOutputFileSwitch(task, outputFile);
    final String[] mapSwitch = config.getMap()
        ? getMapFileSwitch(task, config.getMapFileName(outputFile)) : new String[0];
    int allArgsCount = preargs.length + 1 + outputSwitch.length + mapSwitch.length + sourceFiles.length + endargs.length;
    if (this.isLibtool) {
      allArgsCount++;
    }
    final String[] allArgs = new String[allArgsCount];
    int index = 0;
    if (this.isLibtool) {
      allArgs[index++] = "libtool";
    }
    allArgs[index++] = getCommandWithPath(config);
    final StringBuffer buf = new StringBuffer();

    for (final String prearg : preargs) {
      allArgs[index++] = task.isDecorateLinkerOptions() ? decorateLinkerOption(buf, prearg) : prearg;
    }

    for (final String element : outputSwitch) {
      allArgs[index++] = element;
    }
    for (final String element : mapSwitch) {
      allArgs[index++] = element;
    }
    for (final String sourceFile : sourceFiles) {
      allArgs[index++] = prepareFilename(buf, outputDir, sourceFile);
    }
    for (final String endarg : endargs) {
      allArgs[index++] = task.isDecorateLinkerOptions() ? decorateLinkerOption(buf, endarg) : endarg;
    }

    return allArgs;
  }

  /**
   * Processes filename into argument form
   *
   */
  protected String prepareFilename(final StringBuffer buf, final String outputDir, final String sourceFile) {
    // FREEHEP BEGIN exit if absolute path is too long. Max length on relative
    // paths in windows is even shorter.
    if (isWindows() && sourceFile.length() > this.maxPathLength) {
      throw new BuildException("Absolute path too long, " + sourceFile.length() + " > " + this.maxPathLength + ": '"
          + sourceFile);
    }
    // FREEHEP END
    return quoteFilename(buf, sourceFile);
  }

  /**
   * Prepares argument list to execute the linker using a
   * response file.
   *
   * @param outputFile
   *          linker output file
   * @param args
   *          output of prepareArguments
   * @return arguments for runTask
   */
  protected String[] prepareResponseFile(final File outputFile, final String[] args) throws IOException {
    final String baseName = outputFile.getName();
    final File commandFile = new File(outputFile.getParent(), baseName + ".rsp");
    final FileWriter writer = new FileWriter(commandFile);
    int execArgCount = 1;
    if (this.isLibtool) {
      execArgCount++;
    }
    final String[] execArgs = new String[execArgCount + 1];
    System.arraycopy(args, 0, execArgs, 0, execArgCount);
    execArgs[execArgCount] = getCommandFileSwitch(commandFile.toString());
    for (int i = execArgCount; i < args.length; i++) {
      //
      // if embedded space and not quoted then
      // quote argument
      if (args[i].contains(" ") && args[i].charAt(0) != '\"') {
        writer.write('\"');
        writer.write(args[i]);
        writer.write("\"\n");
      } else {
        writer.write(args[i]);
        writer.write('\n');
      }
    }
    writer.close();
    return execArgs;
  }

  protected String quoteFilename(final StringBuffer buf, final String filename) {
    if (filename.indexOf(' ') >= 0) {
      buf.setLength(0);
      buf.append('\"');
      buf.append(filename);
      buf.append('\"');
      return buf.toString();
    }
    return filename;
  }

  /**
   * This method is exposed so test classes can overload
   * and test the arguments without actually spawning the
   * compiler
   */
  protected int runCommand(final CCTask task, final File workingDir, final String[] cmdline) throws BuildException {
    if (dryRun) return 0;
    return CUtil.runCommand(task, workingDir, cmdline, this.newEnvironment, this.env);
  }

  /** True when this adapter supplies literal arguments without embedded shell quoting. */
  protected boolean hasRawReplayArguments() {
    return false;
  }

  private void recordCommand(final File workingDir, final String[] arguments, final String temporaryMap,
      final String finalMap) {
    if (this.commands instanceof ReplayCommandList && hasRawReplayArguments()) {
      ((ReplayCommandList) this.commands).add(arguments, new ReplayCommand(workingDir, arguments, temporaryMap, finalMap));
    } else if (this.commands != null) {
      this.commands.add(arguments);
    }
  }

  protected final void setCommand(final String command) {
    this.command = command;
  }

  public void setCommands(List<String[]> commands) {
    this.commands = commands;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

}
