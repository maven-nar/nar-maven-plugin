package com.github.maven_nar.cpptasks.parser;
public class PostE extends AbstractParserState {
    private AbstractParserState bracket;
    private AbstractParserState quote;
    public PostE(CParser parser, AbstractParserState bracket,
            AbstractParserState quote) {
        super(parser);
        this.bracket = bracket;
        this.quote = quote;
    }
    public AbstractParserState consume(char ch) {
        switch (ch) {
            case ' ' :
            case '\t' :
                return this;
            case '<' :
                return bracket;
            case '"' :
                return quote;
            case '\n' :
                return getParser().getNewLineState();
        }
        return null;
    }
}
