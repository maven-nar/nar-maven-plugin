package com.github.maven_nar.cpptasks.parser;
public class FilenameState extends AbstractParserState {
    private final StringBuffer buf = new StringBuffer();
    private final char[] terminators;
    public FilenameState(AbstractParser parser, char[] terminators) {
        super(parser);
        this.terminators = (char[]) terminators.clone();
    }
    public AbstractParserState consume(char ch) {
        for (int i = 0; i < terminators.length; i++) {
            if (ch == terminators[i]) {
                getParser().addFilename(buf.toString());
                buf.setLength(0);
                return null;
            }
        }
        if (ch == '\n') {
            buf.setLength(0);
            return getParser().getNewLineState();
        } else {
            buf.append(ch);
        }
        return this;
    }
}
