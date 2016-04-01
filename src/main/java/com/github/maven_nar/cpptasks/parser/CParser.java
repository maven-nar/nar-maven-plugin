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
 * A parser that extracts #include statements from a Reader.
 *
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public final class CParser extends AbstractParser implements Parser {
  private final Vector<String> includes = new Vector<>();
  private final AbstractParserState newLineState;

  /**
     *
     *
     */
  public CParser() {
    final AbstractParserState quote = new FilenameState(this, new char[] {
      '"'
    });
    final AbstractParserState bracket = new FilenameState(this, new char[] {
      '>'
    });
    final AbstractParserState postE = new PostE(this, bracket, quote);
    //
    // nclude
    //
    final AbstractParserState e = new LetterState(this, 'e', postE, null);
    final AbstractParserState d = new LetterState(this, 'd', e, null);
    final AbstractParserState u = new LetterState(this, 'u', d, null);
    final AbstractParserState l = new LetterState(this, 'l', u, null);
    final AbstractParserState c = new LetterState(this, 'c', l, null);
    final AbstractParserState n = new LetterState(this, 'n', c, null);
    //
    // mport is equivalent to nclude
    //
    final AbstractParserState t = new LetterState(this, 't', postE, null);
    final AbstractParserState r = new LetterState(this, 'r', t, null);
    final AbstractParserState o = new LetterState(this, 'o', r, null);
    final AbstractParserState p = new LetterState(this, 'p', o, null);
    final AbstractParserState m = new LetterState(this, 'm', p, null);
    //
    // switch between
    //
    final AbstractParserState n_m = new BranchState(this, new char[] {
        'n', 'm'
    }, new AbstractParserState[] {
        n, m
    }, null);
    final AbstractParserState i = new WhitespaceOrLetterState(this, 'i', n_m);
    this.newLineState = new WhitespaceOrLetterState(this, '#', i);
  }

  @Override
  public void addFilename(final String include) {
    this.includes.addElement(include);
  }

  @Override
  public String[] getIncludes() {
    final String[] retval = new String[this.includes.size()];
    this.includes.copyInto(retval);
    return retval;
  }

  @Override
  public AbstractParserState getNewLineState() {
    return this.newLineState;
  }

  @Override
  public void parse(final Reader reader) throws IOException {
    this.includes.setSize(0);
    super.parse(reader);
  }
}
