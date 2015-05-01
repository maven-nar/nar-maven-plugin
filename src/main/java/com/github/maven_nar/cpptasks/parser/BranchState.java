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

public class BranchState extends AbstractParserState {
  private final char[] branchChars;
  private final AbstractParserState[] branchStates;
  private final AbstractParserState noMatchState;

  public BranchState(final AbstractParser parser, final char[] branchChars, final AbstractParserState[] branchStates,
      final AbstractParserState noMatchState) {
    super(parser);
    this.branchChars = branchChars.clone();
    this.branchStates = branchStates.clone();
    this.noMatchState = noMatchState;
  }

  @Override
  public AbstractParserState consume(final char ch) {
    AbstractParserState state;
    for (int i = 0; i < this.branchChars.length; i++) {
      if (ch == this.branchChars[i]) {
        state = this.branchStates[i];
        return state.consume(ch);
      }
    }
    state = getNoMatchState();
    if (state != null) {
      return state.consume(ch);
    }
    return state;
  }

  protected AbstractParserState getNoMatchState() {
    return this.noMatchState;
  }
}
