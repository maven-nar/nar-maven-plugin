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

import com.github.maven_nar.cpptasks.parser.AbstractParser;
import com.github.maven_nar.cpptasks.parser.AbstractParserState;
import com.github.maven_nar.cpptasks.parser.LetterState;
import com.github.maven_nar.cpptasks.parser.WhitespaceOrLetterState;


/**
 * Scans a source file for Q_OBJECT.
 *
 * @author Curt Arnold
 */
public final class MetaObjectParser
    extends AbstractParser {
  /**
   * Parser state that matches file T character.
   */
  private static final class FinalTState
      extends AbstractParserState {
    /**
     * Parser.
     */
    private final MetaObjectParser mocParser;

    /**
     * Constructor.
     * @param parser MetaObjectParser parser
     */
    public FinalTState(final MetaObjectParser parser) {
      super(parser);
      this.mocParser = parser;
    }

    /**
     * Consumes a character and returns the next state for the parser.
     *
     * @param ch
     *            next character
     * @return the configured nextState if ch is the expected character or the
     *         configure noMatchState otherwise.
     */
    public AbstractParserState consume(final char ch) {
      if (ch == 'T') {
        mocParser.setQObject(true);
        return null;
      }
      if (ch == '\n') {
        getParser().getNewLineState();
      }
      return null;
    }
  }

  /**
   * Determines if source file contains Q_OBJECT.
   * @param reader Reader source reader
   * @throws IOException if unable to read source file
   * @return boolean true if source contains Q_OBJECT
   */
  public static boolean hasQObject(final Reader reader) throws IOException {
    MetaObjectParser parser = new MetaObjectParser();
    parser.parse(reader);
    return parser.hasQObject;

  }

  /**
   * Has Q_OBJECT been encountered.
   */
  private boolean hasQObject = false;

  /**
   * Parser state for start of new line.
   */
  private AbstractParserState newLineState;

  /**
   * Constructor.
   *
   */
  private MetaObjectParser() {
    //
    //    search for Q_OBJECT
    //
    AbstractParserState t = new FinalTState(this);
    AbstractParserState c = new LetterState(this, 'C', t, null);
    AbstractParserState e = new LetterState(this, 'E', c, null);
    AbstractParserState j = new LetterState(this, 'J', e, null);
    AbstractParserState b = new LetterState(this, 'B', j, null);
    AbstractParserState o = new LetterState(this, 'O', b, null);
    AbstractParserState underline = new LetterState(this, '_', o, null);
    newLineState = new WhitespaceOrLetterState(this, 'Q', underline);
  }

  /**
   * Adds a filename to the list of included files.
   *
   * @param filename filename to be added
   */
  protected void addFilename(final String filename) {

  }

  /**
   * Gets new line state.
   * @return AbstractParserState new line state.
   */
  public AbstractParserState getNewLineState() {
    return newLineState;
  }

  /**
   * Parse input file.
   * @param reader Reader source file
   * @throws IOException if error reading source file
   */
  public void parse(final Reader reader) throws IOException {
    hasQObject = false;
    super.parse(reader);
  }

  /**
   * Called FinalTState to set that Q_OBJECT was found.
   * @param value boolean new value for hasQObject
   */
  public void setQObject(final boolean value) {
    this.hasQObject = value;
  }
}
