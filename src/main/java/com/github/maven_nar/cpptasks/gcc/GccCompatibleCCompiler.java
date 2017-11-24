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
package com.github.maven_nar.cpptasks.gcc;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import org.apache.tools.ant.util.FileUtils;

/**
 * Abstract base class for compilers that attempt to be command line compatible
 * with GCC
 *
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public abstract class GccCompatibleCCompiler extends CommandLineCCompiler {
  private final static String[] headerExtensions = new String[] {
      ".h", ".hpp", ".inl"
  };
  private final static String[] sourceExtensions = new String[] {
      ".c", ".cc", ".cpp", ".cxx", ".c++", ".i", ".f", ".for", ".f90"
  };

  /**
   * Private constructor. Use GccCCompiler.getInstance() to get singleton
   * instance of this class.
   */
  protected GccCompatibleCCompiler(final String command, final String identifierArg, final boolean libtool,
      final GccCompatibleCCompiler libtoolCompiler, final boolean newEnvironment, final Environment env) {
    super(command, identifierArg, sourceExtensions, headerExtensions, libtool ? ".fo" : ".o", libtool, libtoolCompiler,
        newEnvironment, env);
  }

  /**
   * Private constructor. Use GccCCompiler.getInstance() to get singleton
   * instance of this class.
   */
  protected GccCompatibleCCompiler(final String command, final String identifierArg, final String[] sourceExtensions,
      final String[] headerExtensions, final boolean libtool, final GccCompatibleCCompiler libtoolCompiler,
      final boolean newEnvironment, final Environment env) {
    super(command, identifierArg, sourceExtensions, headerExtensions, libtool ? ".fo" : ".o", libtool, libtoolCompiler,
        newEnvironment, env);
  }

  @Override
  public void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    //
    // -fPIC is too much trouble
    // users have to manually add it for
    // operating systems that make sense
    //
    args.addElement("-c");
    if (debug) {
      args.addElement("-g");
    } else {
      if (optimization != null) {
        if (optimization.isSize()) {
          args.addElement("-Os");
        } else if (optimization.isSpeed()) {
          if ("full".equals(optimization.getValue())) {
            args.addElement("-O2");
          } else {
            if ("speed".equals(optimization.getValue())) {
              args.addElement("-O1");
            } else {
              args.addElement("-O3");
            }
          }
        }
      }
    }
    if (getIdentifier().contains("mingw")) {
      if (linkType.isSubsystemConsole()) {
        args.addElement("-mconsole");
      }
      if (linkType.isSubsystemGUI()) {
        args.addElement("-mwindows");
      }
    }
    // BEGINFREEHEP, tests have been modified
    if (!exceptions) {
      args.addElement("-fno-exceptions");
    }
    // ENDFREEHEP
    // BEGINFREEHEP moved to GccCCompiler
    // if (rtti != null && !rtti.booleanValue()) {
    // args.addElement("-fno-rtti");
    // }
    // ENDFREEHEP
  }

  /**
   * Adds an include path to the command.
   */
  public void addIncludePath(final String path, final Vector<String> cmd) {
    cmd.addElement("-I" + path);
  }

  @Override
  public void addWarningSwitch(final Vector<String> args, final int level) {
    switch (level) {
      case 0:
        args.addElement("-w");
        break;
      case 5:
        args.addElement("-Werror");
        /* nobreak */
      case 4:
        args.addElement("-W");
        /* nobreak */
      case 3:
        args.addElement("-Wall");
        break;
    }
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
    String relative = "";
    if ( this.gccFileAbsolutePath) {
      return filename;
    } else {
      try {
        relative = FileUtils.getRelativePath(workDir, new File(filename));
      } catch (Exception ex) {
      }
    }
	
    if (relative.isEmpty()) {
      return filename;
    } else {
      return relative;
    }
  }

  @Override
  public void getDefineSwitch(final StringBuffer buffer, final String define, final String value) {
    buffer.append("-D");
    buffer.append(define);
    if (value != null && value.length() > 0) {
      buffer.append('=');
      buffer.append(value);
    }
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    return CUtil.getPathFromEnvironment("INCLUDE", ":");
  }

  // Darren Sargent 22Oct2008 - added overloads to properly handle system paths
  @Override
  public String getIncludeDirSwitch(final String includeDir) {
    return getIncludeDirSwitch(includeDir, false);
  }

  @Override
  public String getIncludeDirSwitch(final String includeDir, final boolean isSystem) {
    if (isSystem) {
      return "-isystem" + includeDir;
    } else {
      return "-I" + includeDir;
    }
  }

  @Override
  public void getUndefineSwitch(final StringBuffer buffer, final String define) {
    buffer.append("-U");
    buffer.append(define);
  }
}
