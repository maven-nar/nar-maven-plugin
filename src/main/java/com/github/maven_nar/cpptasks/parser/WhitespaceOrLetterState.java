package com.github.maven_nar.cpptasks.parser;

/**
 * This parser state checks consumed characters against a specific character or
 * whitespace.
 *
 * @author Curt Arnold
 */
public final class WhitespaceOrLetterState
    extends AbstractParserState {
  /**
   * Next state if the character is found.
   */
  private final AbstractParserState nextState;

  /**
   * Character to match.
   */
  private final char thisLetter;

  /**
   * Constructor.
   *
   * @param parser
   *            parser
   * @param matchLetter
   *            letter to match
   * @param nextStateArg
   *            next state if a match on the letter
   */
  public WhitespaceOrLetterState(final AbstractParser parser,
                                 final char matchLetter,
                                 final AbstractParserState nextStateArg) {
    super(parser);
    this.thisLetter = matchLetter;
    this.nextState = nextStateArg;
  }

  /**
   * Consumes a character and returns the next state for the parser.
   *
   * @param ch
   *            next character @returns the configured nextState if ch is the
   *            expected character or the configure noMatchState otherwise.
   * @return next state
   */
  public AbstractParserState consume(final char ch) {
    if (ch == thisLetter) {
      return nextState;
    }
    if (ch == ' ' || ch == '\t') {
      return this;
    }
    if (ch == '\n') {
      getParser().getNewLineState();
    }
    return null;
  }
}
