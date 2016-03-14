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

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.TargetDef;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the Microsoft (r) Windows 32 Message Compiler
 *
 * @author Greg Domjan
 *
 *         MC [-?aAbcdnouUv] [-co] [-cs namespace] [-css namespace] [-e
 *         extension]
 *         [-h path] [-km] [-m length] [-mof] [-p prefix] [-P prefix] [-r path]
 *         [-s path] [-t path] [-w path] [-W path] [-x path] [-z name]
 *         filename [filename]
 */
public final class MsvcMessageCompiler extends CommandLineCompiler {
  private static final MsvcMessageCompiler instance = new MsvcMessageCompiler(false, null);

  public static MsvcMessageCompiler getInstance() {
    return instance;
  }

  private MsvcMessageCompiler(final boolean newEnvironment, final Environment env) {
    super("mc", null, new String[] {
        ".mc", ".man"
    }, new String[] {}, ".rc", false, null, newEnvironment, env);
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    // no identified configuration compiler arguments implied from these
    // options.
  }

  @Override
  protected void addIncludes(final String baseDirPath, final File[] includeDirs, final Vector<String> args,
      final Vector<String> relativeArgs, final StringBuffer includePathId, final boolean isSystem) {
    // no include switch
    // for some reason we are still getting args in the output??
  }

  @Override
  protected void addWarningSwitch(final Vector<String> args, final int level) {
  }

  @Override
  protected boolean canParse(final File sourceFile) {
    return false;
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    if (newEnvironment || env != null) {
      return new MsvcMessageCompiler(newEnvironment, env);
    }
    return this;
  }

  @Override
  protected Parser createParser(final File source) {
    // neither file type has references to other elements that need to be found
    // through parsing.
    return null;
  }
  
  @Override
  protected CompilerConfiguration createConfiguration(final CCTask task, final LinkType linkType,
      final ProcessorDef[] baseDefs, final CompilerDef specificDef, final TargetDef targetPlatform,
      final VersionInfo versionInfo) {
    return new CommandLineCompilerConfiguration((CommandLineCompilerConfiguration)super.createConfiguration(task, linkType, baseDefs, specificDef, targetPlatform, versionInfo), null, null, true);
  }

  @Override
  protected int getArgumentCountPerInputFile() {
    return 5;
  }

  @Override
  protected void getDefineSwitch(final StringBuffer buffer, final String define, final String value) {
    // no define switch
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    return CUtil.getPathFromEnvironment("INCLUDE", ";");
  }

  @Override
  public String getIdentifier() {
    return "Microsoft (R) Windows (R) Message Compiler";
  }

  @Override
  protected String getIncludeDirSwitch(final String includeDir) {
    return null; // no include switch
  }

  @Override
  protected String getInputFileArgument(final File outputDir, final String filename, final int index) {
    switch (index) {
      case 0:
        return "-r";
      case 1:
        return outputDir.getAbsolutePath();
      case 2:
        return "-h";
      case 3:
        return outputDir.getAbsolutePath();
    }
    return filename;
  }

  @Override
  public Linker getLinker(final LinkType type) {
    return MsvcLinker.getInstance().getLinker(type);
  }

  @Override
  public int getMaximumCommandLength() {
    return 32000;
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
