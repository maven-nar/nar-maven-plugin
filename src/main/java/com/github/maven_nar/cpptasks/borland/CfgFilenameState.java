package com.github.maven_nar.cpptasks.borland;
import com.github.maven_nar.cpptasks.parser.AbstractParser;
import com.github.maven_nar.cpptasks.parser.AbstractParserState;
import com.github.maven_nar.cpptasks.parser.FilenameState;
public class CfgFilenameState extends FilenameState {
    private char terminator;
    public CfgFilenameState(AbstractParser parser, char[] terminators) {
        super(parser, terminators);
        terminator = terminators[0];
    }
    public AbstractParserState consume(char ch) {
        //
        //   if a ';' is encountered then
        //      close the previous filename by sending a
        //         recognized terminator to our super class
        //      and stay in this state for more filenamese
        if (ch == ';') {
            super.consume(terminator);
            return this;
        }
        AbstractParserState newState = super.consume(ch);
        //
        //   change null (consume to end of line)
        //      to look for next switch character
        if (newState == null) {
            newState = getParser().getNewLineState();
        }
        return newState;
    }
}
