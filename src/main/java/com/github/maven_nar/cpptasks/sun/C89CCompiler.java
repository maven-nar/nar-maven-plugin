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
package com.github.maven_nar.cpptasks.sun;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.AbstractCompiler;
import com.github.maven_nar.cpptasks.compiler.CommandLineCCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;

/**
 * Adapter for the Sun C89 C++ Compiler
 *
 * @author Hiram Chirino (cojonudo14@hotmail.com)
 */
public class C89CCompiler extends CommandLineCCompiler {
  private static final AbstractCompiler instance = new C89CCompiler(false, null);

  public static AbstractCompiler getInstance() {
    return instance;
  }

  private C89CCompiler(final boolean newEnvironment, final Environment env) {
    super("c89", null, new String[] {
        ".c", ".cc", ".cpp", ".cxx", ".c++"
    }, new String[] {
        ".h", ".hpp"
    }, ".o", false, null, newEnvironment, env);
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    // Specifies that only compilations and assemblies be done.
    args.addElement("-c");
    /*
     * if (exceptions) { args.addElement("/GX"); }
     */
    if (debug) {
      args.addElement("-g");
      args.addElement("-D_DEBUG");
      /*
       * if (multithreaded) { args.addElement("/D_MT"); if (staticLink) {
       * args.addElement("/MTd"); } else { args.addElement("/MDd");
       * args.addElement("/D_DLL"); } } else { args.addElement("/MLd"); }
       */
    } else {
      args.addElement("-DNDEBUG");
      /*
       * if (multithreaded) { args.addElement("/D_MT"); if (staticLink) {
       * args.addElement("/MT"); } else { args.addElement("/MD");
       * args.addElement("/D_DLL"); } } else { args.addElement("/ML"); }
       */
    }
  }

  @Override
  protected void addWarningSwitch(final Vector<String> args, final int level) {
    C89Processor.addWarningSwitch(args, level);
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    if (newEnvironment || env != null) {
      return new C89CCompiler(newEnvironment, env);
    }
    return this;
  }

  @Override
  protected void getDefineSwitch(final StringBuffer buf, final String define, final String value) {
    C89Processor.getDefineSwitch(buf, define, value);
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    return CUtil.getPathFromEnvironment("INCLUDE", ":");
  }

  @Override
  protected String getIncludeDirSwitch(final String includeDir) {
    return C89Processor.getIncludeDirSwitch(includeDir);
  }

  @Override
  public Linker getLinker(final LinkType type) {
    return C89Linker.getInstance().getLinker(type);
  }

  @Override
  public int getMaximumCommandLength() {
    return Integer.MAX_VALUE;
  }

  /* Only compile one file at time for now */
  @Override
  protected int getMaximumInputFilesPerCommand() {
    return 1;
  }

  @Override
  protected void getUndefineSwitch(final StringBuffer buf, final String define) {
    C89Processor.getUndefineSwitch(buf, define);
  }
}
