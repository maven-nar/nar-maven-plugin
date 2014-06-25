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


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the OpenWatcom C Compiler.
 *
 * @author Curt Arnold
 */
public final class OpenWatcomCCompiler
    extends OpenWatcomCompiler {
  /**
   * Singleton.
   */
  private static final OpenWatcomCCompiler INSTANCE = new OpenWatcomCCompiler(
      "wcl386",
      false, null);

  /**
   * Get compiler.
   * @return OpenWatcomCCompiler compiler
   */
  public static OpenWatcomCCompiler getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   * @param command String command
   * @param newEnvironment boolean use new environment
   * @param env Environment environment
   */
  private OpenWatcomCCompiler(final String command,
                              final boolean newEnvironment,
                              final Environment env) {
    super(command, "/?",
          new String[] {".c", ".cc", ".cpp", ".cxx", ".c++"}
          ,
          new String[] {".h", ".hpp", ".inl"}
          ,
          newEnvironment, env);
  }

  /**
   * Create parser.
   * @param source File file to be parsed.
   * @return Parser parser
   */
  public Parser createParser(final File source) {
    return new CParser();
  }

  /**
   * Get linker.
   * @param type link type
   * @return linker
   */
  public Linker getLinker(final LinkType type) {
    return OpenWatcomCLinker.getInstance().getLinker(type);
  }
}
