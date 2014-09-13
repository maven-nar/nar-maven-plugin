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
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.openwatcom;

import java.io.File;
import java.util.Vector;


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Processor;

/**
 * An abstract base class for the OpenWatcom C and Fortran compilers.
 *
 * @author Curt Arnold
 */
public abstract class OpenWatcomCompiler
    extends CommandLineCompiler {
  /**
   * Constructor.
   * @param command String command
   * @param identifierArg String identifier
   * @param sourceExtensions String[] source extension
   * @param headerExtensions String[] header extension
   * @param newEnvironment boolean use new enviroment
   * @param env Environment environment
   */
  protected OpenWatcomCompiler(final String command,
                               final String identifierArg,
                               final String[] sourceExtensions,
                               final String[] headerExtensions,
                               final boolean newEnvironment,
                               final Environment env) {
    super(command, identifierArg, sourceExtensions,
          headerExtensions, ".obj", false,
          null, newEnvironment, env);
  }

  /**
   * Add implied arguments.
   * @param args Vector command line arguments
   * @param debug boolean is debug
   * @param multithreaded boolean multithreaderd
   * @param exceptions boolean support exceptions
   * @param linkType LinkType link type
   * @param rtti Boolean run time type information
   * @param optimization OptimizationEnum
   */
  protected final void addImpliedArgs(final Vector<String> args,
                                final boolean debug,
                                final boolean multithreaded,
                                final boolean exceptions,
                                final LinkType linkType,
                                final Boolean rtti,
                                final OptimizationEnum optimization) {
    args.addElement("/c");
    if (exceptions) {
      args.addElement("/xs");
    }
    if (multithreaded) {
      args.addElement("/bm");
    }
    if (debug) {
      args.addElement("/d2");
      args.addElement("/od");
      args.addElement("/d_DEBUG");
    } else {
      if (optimization != null) {
        if (optimization.isSize()) {
          args.addElement("/os");
        }
        if (optimization.isSpeed()) {
          args.addElement("/ot");
        }
      }
      args.addElement("/dNDEBUG");
    }
    if (rtti != null && rtti.booleanValue()) {
      args.addElement("/xr");
    }
  }

  /**
   * Add warning switch.
   * @param args Vector command line arguments
   * @param level int warning level
   */
  protected final void addWarningSwitch(final Vector<String> args, final int level) {
    OpenWatcomProcessor.addWarningSwitch(args, level);
  }

  /**
   * Change enviroment.
   * @param newEnvironment boolean use new enviroment
   * @param env Environment environment
   * @return Processor modified processor
   */
  public final Processor changeEnvironment(final boolean newEnvironment,
                                     final Environment env) {
    return this;
  }

  /**
   * Get define switch.
   * @param buffer StringBuffer buffer
   * @param define String preprocessor macro
   * @param value String value, may be null.
   */
  protected final void getDefineSwitch(final StringBuffer buffer,
                                       final String define,
                                 final String value) {
    OpenWatcomProcessor.getDefineSwitch(buffer, define, value);
  }

  /**
   * Get include path from environment.
   * @return File[]
   */
  protected final File[] getEnvironmentIncludePath() {
    return CUtil.getPathFromEnvironment("INCLUDE", ";");
  }

  /**
   * Get include directory switch.
   * @param includeDir String include directory
   * @return String command line argument
   */
  protected final String getIncludeDirSwitch(final String includeDir) {
    return OpenWatcomProcessor.getIncludeDirSwitch(includeDir);
  }


  /**
   * Get maximum command line length.
   * @return int maximum command line length
   */
  public final int getMaximumCommandLength() {
    return 4096;
  }

  /**
   * Get undefine switch.
   * @param buffer StringBuffer argument destination
   * @param define String preprocessor macro
   */
  protected final void getUndefineSwitch(final StringBuffer buffer,
                                   final String define) {
    OpenWatcomProcessor.getUndefineSwitch(buffer, define);
  }

}
