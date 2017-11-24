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
package com.github.maven_nar.cpptasks.borland;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.compiler.ProgressMonitor;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the Borland(r) brc32 Resource compiler.
 *
 * @author Curt Arnold
 */
public class BorlandResourceCompiler extends CommandLineCompiler {
  private static final BorlandResourceCompiler instance = new BorlandResourceCompiler(false, null);

  public static BorlandResourceCompiler getInstance() {
    return instance;
  }

  private BorlandResourceCompiler(final boolean newEnvironment, final Environment env) {
    super("brc32", "c:\\__bogus\\__bogus.rc", new String[] {
      ".rc"
    }, new String[] {
        ".h", ".hpp", ".inl"
    }, ".res", false, null, newEnvironment, env);
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    //
    // compile only
    //
    args.addElement("-r");
  }

  @Override
  protected void addWarningSwitch(final Vector<String> args, final int level) {
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    if (newEnvironment || env != null) {
      return new BorlandResourceCompiler(newEnvironment, env);
    }
    return this;
  }

  @Override
  public void compile(final CCTask task, final File outputDir, final String[] sourceFiles, final String[] args,
      final String[] endArgs, final boolean relentless, final CommandLineCompilerConfiguration config,
      final ProgressMonitor monitor) throws BuildException {
    super.compile(task, outputDir, sourceFiles, args, endArgs, relentless, config, monitor);
  }

  /**
   * The include parser for C will work just fine, but we didn't want to
   * inherit from CommandLineCCompiler
   */
  @Override
  protected Parser createParser(final File source) {
    return new CParser();
  }

  @Override
  protected int getArgumentCountPerInputFile() {
    return 2;
  }

  @Override
  protected void getDefineSwitch(final StringBuffer buffer, final String define, final String value) {
    buffer.append("-d");
    buffer.append(define);
    if (value != null && value.length() > 0) {
      buffer.append('=');
      buffer.append(value);
    }
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    return BorlandProcessor.getEnvironmentPath("brc32", 'i', new String[] {
      "..\\include"
    });
  }

  @Override
  protected String getIncludeDirSwitch(final String includeDir) {
    return BorlandProcessor.getIncludeDirSwitch("-i", includeDir);
  }

  @Override
  protected String getInputFileArgument(final File outputDir, final String filename, final int index) {
    if (index == 0) {
      final String[] outputFileNames = getOutputFileNames(filename, null);
      final String fullOutputName = new File(outputDir, outputFileNames[0]).toString();
      return "-fo" + fullOutputName;
    }
    return filename;
  }

  @Override
  public Linker getLinker(final LinkType type) {
    return BorlandLinker.getInstance().getLinker(type);
  }

  @Override
  public int getMaximumCommandLength() {
    return 1024;
  }

  @Override
  protected int getMaximumInputFilesPerCommand() {
    return 1;
  }

  @Override
  protected void getUndefineSwitch(final StringBuffer buffer, final String define) {
  }
}
