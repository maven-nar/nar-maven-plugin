package com.github.maven_nar.cpptasks.parser;

/**
 * This parser state checks consumed characters against a specific character.
 *
 * @author Curt Arnold
 */
public final class LetterState
    extends AbstractParserState {
  /**
   * Next state if a match is found.
   */
  private final AbstractParserState nextState;

  /**
   * Next state if not match is found.
   */
  private final AbstractParserState noMatchState;

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
   * @param noMatchStateArg
   *            state if no match on letter
   */
  public LetterState(final AbstractParser parser,
                     final char matchLetter,
                     final AbstractParserState nextStateArg,
                     final AbstractParserState noMatchStateArg) {
    super(parser);
    this.thisLetter = matchLetter;
    this.nextState = nextStateArg;
    this.noMatchState = noMatchStateArg;
  }

  /**
   * Consumes a character and returns the next state for the parser.
   *
   * @param ch
   *            next character
   * @return the configured nextState if ch is the expected character or the
   *         configure noMatchState otherwise.
   */
  public AbstractParserState consume(final char ch) {
    if (ch == thisLetter) {
      return nextState;
    }
    if (ch == '\n') {
      getParser().getNewLineState();
    }
    return noMatchState;
  }
}
