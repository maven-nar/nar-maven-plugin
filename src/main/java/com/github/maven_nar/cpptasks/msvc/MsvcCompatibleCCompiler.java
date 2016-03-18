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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.PrecompilingCommandLineCCompiler;
import org.apache.tools.ant.util.FileUtils;

/**
 * An abstract base class for compilers that are basically command line
 * compatible with Microsoft(r) C/C++ Optimizing Compiler
 *
 * @author Curt Arnold
 */
public abstract class MsvcCompatibleCCompiler extends PrecompilingCommandLineCCompiler {
  private static String[] mflags = new String[] {
      //
      // first four are single-threaded
      // (runtime=static,debug=false), (..,debug=true),
      // (runtime=dynamic,debug=true), (..,debug=false), (not supported)
      // next four are multi-threaded, same sequence
      "/ML", "/MLd", null, null, "/MT", "/MTd", "/MD", "/MDd"
  };

  protected MsvcCompatibleCCompiler(final String command, final String identifierArg, final boolean newEnvironment,
      final Environment env) {
    super(command, identifierArg, new String[] {
        ".c", ".cc", ".cpp", ".cxx", ".c++"
    }, new String[] {
        ".h", ".hpp", ".inl"
    }, ".obj", false, null, newEnvironment, env);
  }

  protected void addDebugSwitch(final Vector<String> args) {
    args.addElement("/Zi");
    args.addElement("/Od");
    args.addElement("/RTC1");
    args.addElement("/D_DEBUG");
  }

  protected void addPathSwitch(final Vector<String> args) {
    args.addElement("/Fd" + objDir.getAbsolutePath() + File.separator); // vc[version].pdb
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    args.addElement("/c");
    args.addElement("/nologo");
    if (exceptions) {
      // changed to eliminate warning on VC 2005, should support VC 6 and later
      // use /GX to support VC5 - 2005 (with warning)
      args.addElement("/EHsc");
    }
    int mindex = 0;
    if (multithreaded) {
      mindex += 4;
    }
    final boolean staticRuntime = linkType.isStaticRuntime();
    if (!staticRuntime) {
      mindex += 2;
    }
    if (debug) {
      mindex += 1;
      addDebugSwitch(args);
    } else {
      if (optimization != null) {
        if (optimization.isSize()) {
          args.addElement("/O1");
        }
        if (optimization.isSpeed()) {
          args.addElement("/O2");
        }
      }
      args.addElement("/DNDEBUG");
    }
    final String mflag = mflags[mindex];
    if (mflag == null) {
      throw new BuildException("multithread='false' and runtime='dynamic' not supported");
    }
    args.addElement(mflag);
    if (rtti != null && rtti.booleanValue()) {
      args.addElement("/GR");
    } else {
      // added by Darren Sargent, 21Mar2008 -- /GR is default so need
      // /GR- to disable it
      args.addElement("/GR-");
    }
    addPathSwitch(args);
  }

  @Override
  protected void addWarningSwitch(final Vector<String> args, final int level) {
    MsvcProcessor.addWarningSwitch(args, level);
  }

  @Override
  protected CompilerConfiguration createPrecompileGeneratingConfig(final CommandLineCompilerConfiguration baseConfig,
      final File prototype, final String lastInclude) {
    final String[] additionalArgs = new String[] {
        "/Fp" + CUtil.getBasename(prototype) + ".pch", "/Yc"
    };
    // FREEHEP FIXME we may need /Yd here, but only in debug mode, how do we
    // find out?
    return new CommandLineCompilerConfiguration(baseConfig, additionalArgs, null, true);
  }

  @Override
  protected CompilerConfiguration createPrecompileUsingConfig(final CommandLineCompilerConfiguration baseConfig,
      final File prototype, final String lastInclude, final String[] exceptFiles) {
    final String[] additionalArgs = new String[] {
        "/Fp" + CUtil.getBasename(prototype) + ".pch", "/Yu" + lastInclude
    };

    return new CommandLineCompilerConfiguration(baseConfig, additionalArgs, exceptFiles, false);
  }

  @Override
  protected int getArgumentCountPerInputFile() {
    return 2;
  }

  @Override
  protected String getInputFileArgument(final File outputDir, final String filename, final int index) {
    if (index == 0) {
      final String outputFileName = getOutputFileNames(filename, null)[0];
      final String fullOutputName = new File(outputDir, outputFileName).toString();
      return "/Fo" + fullOutputName;
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
    MsvcProcessor.getDefineSwitch(buffer, define, value);
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    return CUtil.getPathFromEnvironment("INCLUDE", ";");
  }

  @Override
  protected String getIncludeDirSwitch(final String includeDir) {
    return MsvcProcessor.getIncludeDirSwitch(includeDir);
  }

  @Override
  protected void getUndefineSwitch(final StringBuffer buffer, final String define) {
    MsvcProcessor.getUndefineSwitch(buffer, define);
  }
}
