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
    private final Vector includes = new Vector();
    private AbstractParserState newLineState;
    /**
     * 
     *  
     */
    public CParser() {
        AbstractParserState quote = new FilenameState(this, new char[]{'"'});
        AbstractParserState bracket = new FilenameState(this, new char[]{'>'});
        AbstractParserState postE = new PostE(this, bracket, quote);
        //
        //    nclude
        //
        AbstractParserState e = new LetterState(this, 'e', postE, null);
        AbstractParserState d = new LetterState(this, 'd', e, null);
        AbstractParserState u = new LetterState(this, 'u', d, null);
        AbstractParserState l = new LetterState(this, 'l', u, null);
        AbstractParserState c = new LetterState(this, 'c', l, null);
        AbstractParserState n = new LetterState(this, 'n', c, null);
        //
        //   mport is equivalent to nclude
        //
        AbstractParserState t = new LetterState(this, 't', postE, null);
        AbstractParserState r = new LetterState(this, 'r', t, null);
        AbstractParserState o = new LetterState(this, 'o', r, null);
        AbstractParserState p = new LetterState(this, 'p', o, null);
        AbstractParserState m = new LetterState(this, 'm', p, null);
        //
        //   switch between
        //
        AbstractParserState n_m = new BranchState(this, new char[]{'n', 'm'},
                new AbstractParserState[]{n, m}, null);
        AbstractParserState i = new WhitespaceOrLetterState(this, 'i', n_m);
        newLineState = new WhitespaceOrLetterState(this, '#', i);
    }
    public void addFilename(String include) {
        includes.addElement(include);
    }
    public String[] getIncludes() {
        String[] retval = new String[includes.size()];
        includes.copyInto(retval);
        return retval;
    }
    public AbstractParserState getNewLineState() {
        return newLineState;
    }
    public void parse(Reader reader) throws IOException {
        includes.setSize(0);
        super.parse(reader);
    }
}
