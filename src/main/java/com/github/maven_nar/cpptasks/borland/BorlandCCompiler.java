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

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.PrecompilingCommandLineCCompiler;
import com.github.maven_nar.cpptasks.compiler.Processor;
import org.apache.tools.ant.util.FileUtils;

/**
 * Adapter for the Borland(r) C/C++ compiler.
 *
 * @author Curt Arnold
 */
public class BorlandCCompiler extends PrecompilingCommandLineCCompiler {
  private static final String[] headerExtensions = new String[] {
      ".h", ".hpp", ".inl"
  };
  private static final String[] sourceExtensions = new String[] {
      ".c", ".cc", ".cpp", ".cxx", ".c++"
  };
  private static final BorlandCCompiler instance = new BorlandCCompiler(false, null);

  public static BorlandCCompiler getInstance() {
    return instance;
  }

  private BorlandCCompiler(final boolean newEnvironment, final Environment env) {
    super("bcc32", "--version", sourceExtensions, headerExtensions, ".obj", false, null, newEnvironment, env);
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    args.addElement("-c");
    //
    // turn off compiler autodependency since
    // we do it ourselves
    args.addElement("-X");
    if (exceptions) {
      args.addElement("-x");
    } else {
      args.addElement("-x-");
    }
    if (multithreaded) {
      args.addElement("-tWM");
    }
    if (debug) {
      args.addElement("-Od");
      args.addElement("-v");
    } else {
      if (optimization != null) {
        if (optimization.isSpeed()) {
          args.addElement("-O1");
        } else {
          if (optimization.isSpeed()) {
            args.addElement("-O2");
          } else {
            if (optimization.isNoOptimization()) {
              args.addElement("-Od");
            }
          }
        }
      }
    }
    if (rtti != null && !rtti.booleanValue()) {
      args.addElement("-RT-");
    }
  }

  @Override
  protected void addWarningSwitch(final Vector<String> args, final int level) {
    BorlandProcessor.addWarningSwitch(args, level);
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    if (newEnvironment || env != null) {
      return new BorlandCCompiler(newEnvironment, env);
    }
    return this;
  }

  @Override
  protected CompilerConfiguration createPrecompileGeneratingConfig(final CommandLineCompilerConfiguration baseConfig,
      final File prototype, final String lastInclude) {
    final String[] additionalArgs = new String[] {
        "-H=" + lastInclude, "-Hc"
    };
    return new CommandLineCompilerConfiguration(baseConfig, additionalArgs, null, true);
  }

  @Override
  protected CompilerConfiguration createPrecompileUsingConfig(final CommandLineCompilerConfiguration baseConfig,
      final File prototype, final String lastInclude, final String[] exceptFiles) {
    final String[] additionalArgs = new String[] {
      "-Hu"
    };
    return new CommandLineCompilerConfiguration(baseConfig, additionalArgs, exceptFiles, false);
  }

  @Override
  protected int getArgumentCountPerInputFile() {
    return 3;
  }

  @Override
  protected String getInputFileArgument(final File outputDir, final String filename, final int index) {
    switch (index) {
      case 0:
        return "-o";
      case 1:
        final String outputFileName = getOutputFileNames(filename, null)[0];
        final String objectName = new File(outputDir, outputFileName).toString();
        return objectName;
    }
    String relative="";
    try {
      relative = FileUtils.getRelativePath(workDir, new File(filename));
    } catch (Exception ex) {
    }
    if (relative.isEmpty()) {
      return filename;
    } else {
      return relative;
    }
  }

  @Override
  protected void getDefineSwitch(final StringBuffer buffer, final String define, final String value) {
    BorlandProcessor.getDefineSwitch(buffer, define, value);
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    return BorlandProcessor.getEnvironmentPath("bcc32", 'I', new String[] {
      "..\\include"
    });
  }

  @Override
  protected String getIncludeDirSwitch(final String includeDir) {
    return BorlandProcessor.getIncludeDirSwitch("-I", includeDir);
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
  protected void getUndefineSwitch(final StringBuffer buffer, final String define) {
    BorlandProcessor.getUndefineSwitch(buffer, define);
  }
}
