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
package com.github.maven_nar.cpptasks.trolltech;

import java.io.IOException;
import java.io.Reader;

import com.github.maven_nar.cpptasks.parser.Parser;


/**
 * Dependency scanner for Trolltech Qt User Interface definition files.
 *
 * .ui files are XML documents that may contain an include elements,
 * however the includes are just copied to the generated files and
 * and changes to the includes do not need to trigger rerunning uic.
 *
 * @author Curt Arnold
 */
public final class UserInterfaceParser
    implements Parser {

  /**
   *   Constructor.
   *
   */
  public UserInterfaceParser() {
  }

  /**
   * Adds filename to the list of included files.
   *
   * @param include String included file name
   */
  public void addFilename(final String include) {
  }

  /**
   * Gets included files.
   * @return String[] included files
   */
  public String[] getIncludes() {
    return new String[0];
  }

  /**
   * Parses source file for dependencies.
   *
   * @param reader Reader reader
   * @throws IOException if error reading source file
   */
  public void parse(final Reader reader) throws IOException {
  }
}
