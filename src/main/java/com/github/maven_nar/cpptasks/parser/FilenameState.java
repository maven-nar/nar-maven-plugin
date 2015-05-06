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

public class FilenameState extends AbstractParserState {
  private final StringBuffer buf = new StringBuffer();
  private final char[] terminators;

  public FilenameState(final AbstractParser parser, final char[] terminators) {
    super(parser);
    this.terminators = terminators.clone();
  }

  @Override
  public AbstractParserState consume(final char ch) {
    for (final char terminator : this.terminators) {
      if (ch == terminator) {
        getParser().addFilename(this.buf.toString());
        this.buf.setLength(0);
        return null;
      }
    }
    if (ch == '\n') {
      this.buf.setLength(0);
      return getParser().getNewLineState();
    } else {
      this.buf.append(ch);
    }
    return this;
  }
}
