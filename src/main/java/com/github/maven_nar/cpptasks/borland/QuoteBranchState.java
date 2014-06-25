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
