package com.github.maven_nar.cpptasks.borland;
import com.github.maven_nar.cpptasks.parser.AbstractParser;
import com.github.maven_nar.cpptasks.parser.AbstractParserState;
public class ConsumeToSpaceOrNewLine extends AbstractParserState {
    public ConsumeToSpaceOrNewLine(AbstractParser parser) {
        super(parser);
    }
    public AbstractParserState consume(char ch) {
        if (ch == ' ' || ch == '\t' || ch == '\n') {
            return getParser().getNewLineState();
        }
        return this;
    }
}
