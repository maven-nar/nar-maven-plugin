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
package com.github.maven_nar.cpptasks.os400;

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
 * Adapter for the IBM (R) OS/390 (tm) C++ Compiler
 *
 * @author Hiram Chirino (cojonudo14@hotmail.com)
 */
public class IccCompiler extends CommandLineCCompiler {
  private static final AbstractCompiler instance = new IccCompiler(false, null);

  public static AbstractCompiler getInstance() {
    return instance;
  }

  private IccCompiler(final boolean newEnvironment, final Environment env) {
    super("icc", null, new String[] {
        ".c", ".cc", ".cpp", ".cxx", ".c++", ".s"
    }, new String[] {
        ".h", ".hpp"
    }, ".o", false, null, newEnvironment, env);
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    // Specifies that only compilations and assemblies be done.
    // Link-edit is not done
    args.addElement("-c");
    /*
     * if (exceptions) { args.addElement("/GX"); }
     */
    if (debug) {
      args.addElement("-g");
      /*
       * args.addElement("-D"); args.addElement("_DEBUG"); if
       * (multithreaded) { args.addElement("/D_MT"); if (staticLink) {
       * args.addElement("/MTd"); } else { args.addElement("/MDd");
       * args.addElement("/D_DLL"); } } else { args.addElement("/MLd"); }
       */
    } else {
      /*
       * args.addElement("-D"); args.addElement("NEBUG"); if
       * (multithreaded) { args.addElement("/D_MT"); if (staticLink) {
       * args.addElement("/MT"); } else { args.addElement("/MD");
       * args.addElement("/D_DLL"); } } else { args.addElement("/ML"); }
       */
    }
  }

  @Override
  protected void addWarningSwitch(final Vector<String> args, final int level) {
    IccProcessor.addWarningSwitch(args, level);
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    if (newEnvironment || env != null) {
      return new IccCompiler(newEnvironment, env);
    }
    return this;
  }

  @Override
  protected void getDefineSwitch(final StringBuffer buffer, final String define, final String value) {
    buffer.append("-q");
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

  @Override
  protected String getIncludeDirSwitch(final String includeDir) {
    return IccProcessor.getIncludeDirSwitch(includeDir);
  }

  @Override
  public Linker getLinker(final LinkType type) {
    return IccLinker.getInstance().getLinker(type);
  }

  @Override
  public int getMaximumCommandLength() {
    return Integer.MAX_VALUE;
  }

  /* Only compile one file at time for now */
  @Override
  protected int getMaximumInputFilesPerCommand() {
    return 1;
    // return Integer.MAX_VALUE;
  }

  @Override
  protected void getUndefineSwitch(final StringBuffer buffer, final String define) {
    /*
     * buffer.addElement("-q"); buf.append(define);
     */
  }
}
