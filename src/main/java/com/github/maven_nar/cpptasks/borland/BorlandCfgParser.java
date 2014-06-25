package com.github.maven_nar.cpptasks.borland;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import com.github.maven_nar.cpptasks.parser.AbstractParser;
import com.github.maven_nar.cpptasks.parser.AbstractParserState;
import com.github.maven_nar.cpptasks.parser.LetterState;
import com.github.maven_nar.cpptasks.parser.WhitespaceOrLetterState;

/**
 * A parser that paths from a borland cfg file
 * 
 * @author Curt Arnold
 */
public final class BorlandCfgParser extends AbstractParser {
    private AbstractParserState newLineState;
    private final Vector path = new Vector();
    /**
     * 
     *  
     */
    public BorlandCfgParser(char switchChar) {
        //
        //   a quoted path (-I"some path")
        //      doesn't end till a close quote and will be abandoned
        //      if a new line is encountered first
        //
        AbstractParserState quote = new CfgFilenameState(this, new char[]{'"'});
        //
        //    an unquoted path (-Ic:\borland\include)
        //      ends at the first space or new line
        AbstractParserState unquote = new CfgFilenameState(this, new char[]{
                ' ', '\n', '\r'});
        AbstractParserState quoteBranch = new QuoteBranchState(this, quote,
                unquote);
        AbstractParserState toNextSwitch = new ConsumeToSpaceOrNewLine(this);
        AbstractParserState switchState = new LetterState(this, switchChar,
                quoteBranch, toNextSwitch);
        newLineState = new WhitespaceOrLetterState(this, '-', switchState);
    }
    public void addFilename(String include) {
        path.addElement(include);
    }
    public AbstractParserState getNewLineState() {
        return newLineState;
    }
    public String[] parsePath(Reader reader) throws IOException {
        path.setSize(0);
        super.parse(reader);
        String[] retval = new String[path.size()];
        path.copyInto(retval);
        return retval;
    }
}
