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
package com.github.maven_nar.cpptasks.ti;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;

/**
 * Adapter for TI DSP compilers with cl** commands
 *
 * @author CurtA
 */
public class ClxxCCompiler extends CommandLineCCompiler {
  /**
   * Header file extensions
   */
  private static final String[] headerExtensions = new String[] {
      ".h", ".hpp", ".inl"
  };
  /**
   * Source file extensions
   */
  private static final String[] sourceExtensions = new String[] {
      ".c", ".cc", ".cpp", ".cxx", ".c++"
  };
  /**
   * Singleton for TMS320C55x
   */
  private static final ClxxCCompiler cl55 = new ClxxCCompiler("cl55", false, null);
  /**
   * Singleton for TMS320C6000
   */
  private static final ClxxCCompiler cl6x = new ClxxCCompiler("cl6x", false, null);

  public static ClxxCCompiler getCl55Instance() {
    return cl55;
  }

  public static ClxxCCompiler getCl6xInstance() {
    return cl6x;
  }

  /**
   * Private constructor
   * 
   * @param command
   *          executable name
   * @param newEnvironment
   *          Change environment
   * @param env
   *          New environment
   */
  private ClxxCCompiler(final String command, final boolean newEnvironment, final Environment env) {
    super(command, "-h", sourceExtensions, headerExtensions, ".o", false, null, newEnvironment, env);
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    if (debug) {
      args.addElement("-gw");
    }
  }

  @Override
  protected void addWarningSwitch(final Vector<String> args, final int warnings) {
    // TODO Auto-generated method stub
  }

  @Override
  protected void getDefineSwitch(final StringBuffer buffer, final String define, final String value) {
    buffer.append("-d");
    buffer.append(define);
    if (value != null) {
      buffer.append('=');
      buffer.append(value);
    }
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    final File[] c_dir = CUtil.getPathFromEnvironment("C_DIR", ";");
    final File[] cx_dir = CUtil.getPathFromEnvironment("C6X_C_DIR", ";");
    if (c_dir.length == 0) {
      return cx_dir;
    }
    if (cx_dir.length == 0) {
      return c_dir;
    }
    final File[] combo = new File[c_dir.length + cx_dir.length];
    System.arraycopy(cx_dir, 0, combo, 0, cx_dir.length);
    System.arraycopy(c_dir, 0, combo, 0 + cx_dir.length, c_dir.length);
    return combo;
  }

  @Override
  protected String getIncludeDirSwitch(final String source) {
    return "-I" + source;
  }

  @Override
  public Linker getLinker(final LinkType type) {
    if (type.isStaticLibrary()) {
      if (this == cl6x) {
        return ClxxLibrarian.getCl6xInstance();
      }
      return ClxxLibrarian.getCl55Instance();
    }
    if (type.isSharedLibrary()) {
      if (this == cl6x) {
        return ClxxLinker.getCl6xDllInstance();
      }
      return ClxxLinker.getCl55DllInstance();
    }
    if (this == cl6x) {
      return ClxxLinker.getCl6xInstance();
    }
    return ClxxLinker.getCl55Instance();
  }

  @Override
  public int getMaximumCommandLength() {
    return 1024;
  }

  @Override
  protected void getUndefineSwitch(final StringBuffer buffer, final String define) {
    buffer.append("-u");
    buffer.append(define);
  }
}
