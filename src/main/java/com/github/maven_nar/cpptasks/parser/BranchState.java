package com.github.maven_nar.cpptasks.parser;
public class BranchState extends AbstractParserState {
    private char[] branchChars;
    private AbstractParserState[] branchStates;
    private AbstractParserState noMatchState;
    public BranchState(AbstractParser parser, char[] branchChars,
            AbstractParserState[] branchStates, AbstractParserState noMatchState) {
        super(parser);
        this.branchChars = (char[]) branchChars.clone();
        this.branchStates = (AbstractParserState[]) branchStates.clone();
        this.noMatchState = noMatchState;
    }
    public AbstractParserState consume(char ch) {
        AbstractParserState state;
        for (int i = 0; i < branchChars.length; i++) {
            if (ch == branchChars[i]) {
                state = branchStates[i];
                return state.consume(ch);
            }
        }
        state = getNoMatchState();
        if (state != null) {
            return state.consume(ch);
        }
        return state;
    }
    protected AbstractParserState getNoMatchState() {
        return noMatchState;
    }
}
