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
package com.github.maven_nar.cpptasks.compaq;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineFortranCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;

/**
 * Adapter for the Compaq(r) Visual Fortran compiler.
 *
 * @author Curt Arnold
 */
public class CompaqVisualFortranCompiler extends CommandLineFortranCompiler {
  private static final CompaqVisualFortranCompiler[] instance = new CompaqVisualFortranCompiler[] {
    new CompaqVisualFortranCompiler(false, null)
  };

  public static CompaqVisualFortranCompiler getInstance() {
    return instance[0];
  }

  private CompaqVisualFortranCompiler(final boolean newEnvironment, final Environment env) {
    super("DF", null, new String[] {
        ".f90", ".for", ".f"
    }, new String[] {
        ".i", ".i90", ".fpp", ".inc", ".bak", ".exe"
    }, ".obj", false, null, newEnvironment, env);
  }

  @Override
  protected void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    args.addElement("/nologo");
    args.addElement("/compile_only");
    if (debug) {
      args.addElement("/debug:full");
      args.addElement("/define:_DEBUG");
    } else {
      args.addElement("/debug:none");
      args.addElement("/define:NDEBUG");
    }
    if (multithreaded) {
      args.addElement("/threads");
      args.addElement("/define:_MT");
    } else {
      args.addElement("/nothreads");
    }
    final boolean staticRuntime = linkType.isStaticRuntime();
    if (staticRuntime) {
      args.addElement("/libs:static");
    } else {
      args.addElement("/libs:dll");
    }
    if (linkType.isSharedLibrary()) {
      args.addElement("/dll");
      args.addElement("/define:_DLL");
    }
  }

  @Override
  public void addWarningSwitch(final Vector<String> args, final int level) {
    switch (level) {
      case 0:
        args.addElement("/nowarn");
        break;
      case 1:
        break;
      case 2:
        break;
      case 3:
        args.addElement("/warn:usage");
        break;
      case 4:
        args.addElement("/warn:all");
        break;
      case 5:
        args.addElement("/warn:errors");
        break;
    }
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    if (newEnvironment || env != null) {
      return new CompaqVisualFortranCompiler(newEnvironment, env);
    }
    return this;
  }

  @Override
  protected void getDefineSwitch(final StringBuffer buf, final String define, final String value) {
    buf.append("/define:");
    buf.append(define);
    if (value != null && value.length() > 0) {
      buf.append('=');
      buf.append(value);
    }
  }

  @Override
  protected File[] getEnvironmentIncludePath() {
    return CUtil.getPathFromEnvironment("INCLUDE", ";");
  }

  @Override
  protected String getIncludeDirSwitch(final String includeDir) {
    // BEGINFREEHEP quotes seem to confuse the compiler
    // if (includeDir.indexOf(' ') >= 0) {
    // buf.append('"');
    // buf.append(includeDir);
    // buf.append('"');
    // } else {
    // }
    // ENDFREEHEP
    return "/include:" + includeDir;
  }

  @Override
  public Linker getLinker(final LinkType type) {
    return CompaqVisualFortranLinker.getInstance().getLinker(type);
  }

  @Override
  public int getMaximumCommandLength() {
    return 1024;
  }

  @Override
  protected void getUndefineSwitch(final StringBuffer buf, final String define) {
    buf.append("/undefine:");
    buf.append(define);
  }
}
