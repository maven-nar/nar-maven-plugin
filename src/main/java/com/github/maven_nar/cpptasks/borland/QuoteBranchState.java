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
package com.github.maven_nar.cpptasks.borland;
import com.github.maven_nar.cpptasks.parser.AbstractParser;
import com.github.maven_nar.cpptasks.parser.AbstractParserState;
public class QuoteBranchState extends AbstractParserState {
    private AbstractParserState quote;
    private AbstractParserState unquote;
    public QuoteBranchState(AbstractParser parser, AbstractParserState quote,
            AbstractParserState unquote) {
        super(parser);
        this.quote = quote;
        this.unquote = unquote;
    }
    public AbstractParserState consume(char ch) {
        if (ch == '"') {
            return quote;
        }
        return unquote.consume(ch);
    }
}
