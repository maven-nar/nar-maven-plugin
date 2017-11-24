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
package com.github.maven_nar.cpptasks.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/**
 * A parser that extracts INCLUDE statements from a Reader.
 *
 * @author Curt Arnold
 */
public final class FortranParser extends AbstractParser implements Parser {
  /**
   * List of included filenames.
   */
  private final Vector<String> includes = new Vector<>();

  /**
   * State that starts consuming content at the beginning of a line.
   */
  private final AbstractParserState newLineState;

  /**
   * Default constructor.
   *
   */
  public FortranParser() {
    final AbstractParserState filename = new FilenameState(this, new char[] {
        '\'', '/'
    });
    final AbstractParserState apos = new WhitespaceOrLetterState(this, '\'', filename);
    final AbstractParserState blank = new LetterState(this, ' ', apos, null);
    final AbstractParserState e = new CaseInsensitiveLetterState(this, 'E', blank, null);
    final AbstractParserState d = new CaseInsensitiveLetterState(this, 'D', e, null);
    final AbstractParserState u = new CaseInsensitiveLetterState(this, 'U', d, null);
    final AbstractParserState l = new CaseInsensitiveLetterState(this, 'L', u, null);
    final AbstractParserState c = new CaseInsensitiveLetterState(this, 'C', l, null);
    final AbstractParserState n = new CaseInsensitiveLetterState(this, 'N', c, null);
    this.newLineState = new WhitespaceOrCaseInsensitiveLetterState(this, 'I', n);
  }

  /**
   * Called by FilenameState at completion of file name production.
   *
   * @param include
   *          include file name
   */
  @Override
  public void addFilename(final String include) {
    this.includes.addElement(include);
  }

  /**
   * Gets collection of include file names encountered in parse.
   * 
   * @return include file names
   */
  @Override
  public String[] getIncludes() {
    final String[] retval = new String[this.includes.size()];
    this.includes.copyInto(retval);
    return retval;
  }

  /**
   * Get the state for the beginning of a new line.
   * 
   * @return start of line state
   */
  @Override
  public AbstractParserState getNewLineState() {
    return this.newLineState;
  }

  /**
   * Collects all included files from the content of the reader.
   *
   * @param reader
   *          character reader containing a FORTRAN source module
   * @throws IOException
   *           throw if I/O error during parse
   */
  @Override
  public void parse(final Reader reader) throws IOException {
    this.includes.setSize(0);
    super.parse(reader);
  }
}
