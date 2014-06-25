package com.github.maven_nar.cpptasks.parser;
import java.io.IOException;
import java.io.Reader;
/**
 * An abstract base class for simple parsers
 * 
 * @author Curt Arnold
 */
public abstract class AbstractParser {
    /**
     * 
     *  
     */
    protected AbstractParser() {
    }
    protected abstract void addFilename(String filename);
    public abstract AbstractParserState getNewLineState();
    protected void parse(Reader reader) throws IOException {
        char[] buf = new char[4096];
        AbstractParserState newLineState = getNewLineState();
        AbstractParserState state = newLineState;
        int charsRead = -1;
        do {
            charsRead = reader.read(buf, 0, buf.length);
            if (state == null) {
                for (int i = 0; i < charsRead; i++) {
                    if (buf[i] == '\n') {
                        state = newLineState;
                        break;
                    }
                }
            }
            if (state != null) {
                for (int i = 0; i < charsRead; i++) {
                    state = state.consume(buf[i]);
                    //
                    //  didn't match a production, skip to a new line
                    //
                    if (state == null) {
                        for (; i < charsRead; i++) {
                            if (buf[i] == '\n') {
                                state = newLineState;
                                break;
                            }
                        }
                    }
                }
            }
        } while (charsRead >= 0);
    }
}
