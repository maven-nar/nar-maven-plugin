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
package com.github.maven_nar.cpptasks.msvc;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the Microsoft (r) Windows 32 Resource Compiler
 *
 * @author Curt Arnold
 */
public final class MsvcResourceCompiler extends CommandLineCompiler {
  private static final MsvcResourceCompiler instance = new MsvcResourceCompiler(false, null);

  public static MsvcResourceCompiler getInstance() {
    return instance;
  }

  private String identifier;

  private MsvcResourceCompiler(final boolean newEnvironment, final Environment env) {
    super("rc", null, new String[] {
      ".rc"
    }, new String[] {
        ".h", ".hpp", ".inl"
    }, ".res", false, null, newEnvironment, env);
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    if (debug) {
      args.addElement("/D_DEBUG");
    } else {
      args.addElement("/DNDEBUG");
    }
  }

  @Override
  protected void addWarningSwitch(final Vector<String> args, final int level) {
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    if (newEnvironment || env != null) {
      return new MsvcResourceCompiler(newEnvironment, env);
    }
    return this;
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
    MsvcProcessor.getDefineSwitch(buffer, define, value);
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    return CUtil.getPathFromEnvironment("INCLUDE", ";");
  }

  @Override
  public String getIdentifier() {
    return "Microsoft (R) Windows (R) Resource Compiler";
  }

  @Override
  protected String getIncludeDirSwitch(final String includeDir) {
    return MsvcProcessor.getIncludeDirSwitch(includeDir);
  }

  @Override
  protected String getInputFileArgument(final File outputDir, final String filename, final int index) {
    if (index == 0) {
      final String outputFileName = getOutputFileNames(filename, null)[0];
      final String fullOutputName = new File(outputDir, outputFileName).toString();
      return "/fo" + fullOutputName;
    }
    return filename;
  }

  @Override
  public Linker getLinker(final LinkType type) {
    return MsvcLinker.getInstance().getLinker(type);
  }

  @Override
  public int getMaximumCommandLength() {
    // FREEHEP stay on the safe side
    return 32000; // 32767;
  }

  @Override
  protected int getMaximumInputFilesPerCommand() {
    return 1;
  }

  @Override
  protected void getUndefineSwitch(final StringBuffer buffer, final String define) {
    MsvcProcessor.getUndefineSwitch(buffer, define);
  }
}
