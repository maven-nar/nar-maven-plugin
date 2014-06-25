package com.github.maven_nar.cpptasks.parser;
/**
 * An base class for objects that represent the state of an AbstractParser.
 * 
 * @author CurtArnold
 * @see AbstractParser
 */
public abstract class AbstractParserState {
    private AbstractParser parser;
    protected AbstractParserState(AbstractParser parser) {
        if (parser == null) {
            throw new NullPointerException("parser");
        }
        this.parser = parser;
    }
    /**
     * Consume a character
     * 
     * @return new state, may be null to ignore the rest of the line
     */
    public abstract AbstractParserState consume(char ch);
    protected AbstractParser getParser() {
        return parser;
    }
}
