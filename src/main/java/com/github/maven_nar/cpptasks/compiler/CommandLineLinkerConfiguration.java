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

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.LinkerParam;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.TargetInfo;
import com.github.maven_nar.cpptasks.VersionInfo;

/**
 * A configuration for a command line linker
 *
 * @author Curt Arnold
 */
public final class CommandLineLinkerConfiguration implements LinkerConfiguration {
  private/* final */String[][] args;
  private final/* final */String identifier;
  private String[] libraryNames;
  private final/* final */CommandLineLinker linker;
  private final/* final */boolean map;
  private final/* final */ProcessorParam[] params;
  private final/* final */boolean rebuild;
  private/* final */String commandPath;
  private final boolean debug;
  private final String startupObject;

  public CommandLineLinkerConfiguration(final CommandLineLinker linker, final String identifier, final String[][] args,
      final ProcessorParam[] params, final boolean rebuild, final boolean map, final boolean debug,
      final String[] libraryNames, final String startupObject) {
    this(linker, identifier, args, params, rebuild, map, debug, libraryNames, startupObject, null);
  }

  public CommandLineLinkerConfiguration(final CommandLineLinker linker, final String identifier, final String[][] args,
      final ProcessorParam[] params, final boolean rebuild, final boolean map, final boolean debug,
      final String[] libraryNames, final String startupObject, final String commandPath) {
    if (linker == null) {
      throw new NullPointerException("linker");
    }
    if (args == null) {
      throw new NullPointerException("args");
    } else {
      this.args = args.clone();
    }
    this.linker = linker;
    this.params = params.clone();
    this.rebuild = rebuild;
    this.identifier = identifier;
    this.map = map;
    this.debug = debug;
    if (libraryNames == null) {
      this.libraryNames = new String[0];
    } else {
      this.libraryNames = libraryNames.clone();
    }
    this.startupObject = startupObject;
    this.commandPath = commandPath;
  }

  @Override
  public int bid(final String filename) {
    return this.linker.bid(filename);
  }

  public final String getCommandPath() {
    return this.commandPath;
  }

  public String[] getEndArguments() {
    final String[] clone = this.args[1].clone();
    return clone;
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

  public String[] getLibraryNames() {
    final String[] clone = this.libraryNames.clone();
    return clone;
  }

  @Override
  public Linker getLinker() {
    return this.linker;
  }

  public boolean getMap() {
    return this.map;
  }

  @Override
  public String[] getOutputFileNames(final String inputFile, final VersionInfo versionInfo) {
    return this.linker.getOutputFileNames(inputFile, versionInfo);
  }

  @Override
  public LinkerParam getParam(final String name) {
    for (final ProcessorParam param : this.params) {
      if (name.equals(param.getName())) {
        return (LinkerParam) param;
      }
    }
    return null;
  }

  @Override
  public ProcessorParam[] getParams() {
    return this.params;
  }

  public String[] getPreArguments() {
    final String[] clone = this.args[0].clone();
    return clone;
  }

  @Override
  public boolean getRebuild() {
    return this.rebuild;
  }

  public String getStartupObject() {
    return this.startupObject;
  }

  @Override
  public boolean isDebug() {
    return this.debug;
  }

  @Override
  public void link(final CCTask task, final TargetInfo linkTarget) throws BuildException {
    //
    // AllSourcePath's include any syslibsets
    //
    final String[] sourcePaths = linkTarget.getAllSourcePaths();
    this.linker.link(task, linkTarget.getOutput(), sourcePaths, this);
  }

  public final void setCommandPath(final String commandPath) {
    this.commandPath = commandPath;
  }

  @Override
  public String toString() {
    return this.identifier;
  }
}
