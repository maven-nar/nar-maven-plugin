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

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CompilerParam;
import com.github.maven_nar.cpptasks.DependencyInfo;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.VersionInfo;

/**
 * A configuration for a C++ compiler
 *
 * @author Curt Arnold
 */
public final class CommandLineCompilerConfiguration implements CompilerConfiguration {
  private boolean useCcache;
  private/* final */String[] args;
  private final/* final */CommandLineCompiler compiler;
  private final String[] endArgs;
  //
  // include path from environment variable not
  // explicitly stated in Ant script
  private/* final */File[] envIncludePath;
  private String[] exceptFiles;
  private final/* final */String identifier;
  private/* final */File[] includePath;
  private final/* final */String includePathIdentifier;
  private final boolean isPrecompiledHeaderGeneration;
  private/* final */ProcessorParam[] params;
  private final/* final */boolean rebuild;
  private/* final */File[] sysIncludePath;
  private/* final */String commandPath;

  public CommandLineCompilerConfiguration(final CommandLineCompiler compiler, final String identifier,
      final File[] includePath, final File[] sysIncludePath, final File[] envIncludePath,
      final String includePathIdentifier, final String[] args, final ProcessorParam[] params, final boolean rebuild,
      final String[] endArgs) {
    this(compiler, identifier, includePath, sysIncludePath, envIncludePath, includePathIdentifier, args, params,
        rebuild, endArgs, null);
  }

  public CommandLineCompilerConfiguration(final CommandLineCompiler compiler, final String identifier,
      final File[] includePath, final File[] sysIncludePath, final File[] envIncludePath,
      final String includePathIdentifier, final String[] args, final ProcessorParam[] params, final boolean rebuild,
      final String[] endArgs, final String commandPath) {
    this(compiler, identifier, includePath, sysIncludePath, envIncludePath, includePathIdentifier, args, params,
        rebuild, endArgs, commandPath, false);
  }

  public CommandLineCompilerConfiguration(final CommandLineCompiler compiler, final String identifier,
      final File[] includePath, final File[] sysIncludePath, final File[] envIncludePath,
      final String includePathIdentifier, final String[] args, final ProcessorParam[] params, final boolean rebuild,
      final String[] endArgs, final String commandPath, final boolean useCcache) {
    if (compiler == null) {
      throw new NullPointerException("compiler");
    }
    if (identifier == null) {
      throw new NullPointerException("identifier");
    }
    if (includePathIdentifier == null) {
      throw new NullPointerException("includePathIdentifier");
    }
    if (args == null) {
      this.args = new String[0];
    } else {
      this.args = args.clone();
    }
    if (includePath == null) {
      this.includePath = new File[0];
    } else {
      this.includePath = includePath.clone();
    }
    if (sysIncludePath == null) {
      this.sysIncludePath = new File[0];
    } else {
      this.sysIncludePath = sysIncludePath.clone();
    }
    if (envIncludePath == null) {
      this.envIncludePath = new File[0];
    } else {
      this.envIncludePath = envIncludePath.clone();
    }
    this.useCcache = useCcache;
    this.compiler = compiler;
    this.params = params.clone();
    this.rebuild = rebuild;
    this.identifier = identifier;
    this.includePathIdentifier = includePathIdentifier;
    this.endArgs = endArgs.clone();
    this.exceptFiles = null;
    this.isPrecompiledHeaderGeneration = false;
    this.commandPath = commandPath;
  }

  public CommandLineCompilerConfiguration(final CommandLineCompilerConfiguration base, final String[] additionalArgs,
      final String[] exceptFiles, final boolean isPrecompileHeaderGeneration) {
    this.compiler = base.compiler;
    this.identifier = base.identifier;
    this.rebuild = base.rebuild;
    this.includePath = base.includePath.clone();
    this.sysIncludePath = base.sysIncludePath.clone();
    this.endArgs = base.endArgs.clone();
    this.envIncludePath = base.envIncludePath.clone();
    this.includePathIdentifier = base.includePathIdentifier;
    if (exceptFiles != null) {
      this.exceptFiles = exceptFiles.clone();
    }
    this.isPrecompiledHeaderGeneration = isPrecompileHeaderGeneration;
    if (additionalArgs != null) {
      this.args = new String[base.args.length + additionalArgs.length];
      System.arraycopy(base.args, 0, this.args, 0, base.args.length);
      int index = base.args.length;
      for (final String additionalArg : additionalArgs) {
        this.args[index++] = additionalArg;
      }
    } else {
      this.args = base.args.clone();
    }
    this.commandPath = base.commandPath;
  }

  @Override
  public int bid(final String inputFile) {
    final int compilerBid = this.compiler.bid(inputFile);
    if (compilerBid > 0 && this.exceptFiles != null) {
      for (final String exceptFile : this.exceptFiles) {
        if (inputFile.equals(exceptFile)) {
          return 0;
        }
      }
    }
    return compilerBid;
  }

  @Override
  public void compile(final CCTask task, final File outputDir, final String[] sourceFiles, final boolean relentless,
      final ProgressMonitor monitor) throws BuildException {
    if (monitor != null) {
      monitor.start(this);
    }
    try {
      this.compiler.compile(task, outputDir, sourceFiles, this.args, this.endArgs, relentless, this, monitor);
      if (monitor != null) {
        monitor.finish(this, true);
      }
    } catch (final BuildException ex) {
      if (monitor != null) {
        monitor.finish(this, false);
      }
      throw ex;
    }
  }

  /**
   * 
   * This method may be used to get two distinct compiler configurations, one
   * for compiling the specified file and producing a precompiled header
   * file, and a second for compiling other files using the precompiled
   * header file.
   * 
   * The last (preferrably only) include directive in the prototype file will
   * be used to mark the boundary between pre-compiled and normally compiled
   * headers.
   * 
   * @param prototype
   *          A source file (for example, stdafx.cpp) that is used to build
   *          the precompiled header file. @returns null if precompiled
   *          headers are not supported or a two element array containing
   *          the precompiled header generation configuration and the
   *          consuming configuration
   * 
   */
  @Override
  public CompilerConfiguration[]
      createPrecompileConfigurations(final File prototype, final String[] nonPrecompiledFiles) {
    if (this.compiler instanceof PrecompilingCompiler) {
      return ((PrecompilingCompiler) this.compiler)
          .createPrecompileConfigurations(this, prototype, nonPrecompiledFiles);
    }
    return null;
  }

  public String getCommand() {
    return this.compiler.getCommand();
  }

  public final String getCommandPath() {
    return this.commandPath;
  }

  public Compiler getCompiler() {
    return this.compiler;
  }

  public String[] getEndArguments() {
    return this.endArgs.clone();
  }

  /**
   * Returns a string representation of this configuration. Should be
   * canonical so that equivalent configurations will have equivalent string
   * representations
   */
  @Override
  public String getIdentifier() {
    return this.identifier;
  }

  public File[] getIncludePath() {
    return this.includePath.clone();
  }

  @Override
  public String getIncludePathIdentifier() {
    return this.includePathIdentifier;
  }

  @Override
  public String[] getOutputFileNames(final String inputFile, final VersionInfo versionInfo) {
    return this.compiler.getOutputFileNames(inputFile, versionInfo);
  }

  @Override
  public CompilerParam getParam(final String name) {
    for (final ProcessorParam param : this.params) {
      if (name.equals(param.getName())) {
        return (CompilerParam) param;
      }
    }
    return null;
  }

  @Override
  public ProcessorParam[] getParams() {
    return this.params;
  }

  public String[] getPreArguments() {
    return this.args.clone();
  }

  @Override
  public boolean getRebuild() {
    return this.rebuild;
  }

  @Override
  public boolean isPrecompileGeneration() {
    return this.isPrecompiledHeaderGeneration;
  }

  public boolean isUseCcache() {
    return this.useCcache;
  }

  @Override
  public DependencyInfo parseIncludes(final CCTask task, final File baseDir, final File source) {
    return this.compiler.parseIncludes(task, source, this.includePath, this.sysIncludePath, this.envIncludePath,
        baseDir, getIncludePathIdentifier());
  }

  public final void setCommandPath(final String commandPath) {
    this.commandPath = commandPath;
  }

  @Override
  public String toString() {
    return this.identifier;
  }
}
