package com.github.maven_nar.cpptasks.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/**
 * A parser that extracts INCLUDE statements from a Reader.
 *
 * @author Curt Arnold
 */
public final class FortranParser
    extends AbstractParser
    implements Parser {
  /**
   * List of included filenames.
   */
  private final Vector includes = new Vector();

  /**
   * State that starts consuming content at the beginning of a line.
   */
  private final AbstractParserState newLineState;

  /**
   * Default constructor.
   *
   */
  public FortranParser() {
    AbstractParserState filename = new FilenameState(this, new char[] {'\'',
        '/'});
    AbstractParserState apos = new WhitespaceOrLetterState(this, '\'',
        filename);
    AbstractParserState blank = new LetterState(this, ' ', apos, null);
    AbstractParserState e = new CaseInsensitiveLetterState(this, 'E',
        blank, null);
    AbstractParserState d = new CaseInsensitiveLetterState(this, 'D', e,
        null);
    AbstractParserState u = new CaseInsensitiveLetterState(this, 'U', d,
        null);
    AbstractParserState l = new CaseInsensitiveLetterState(this, 'L', u,
        null);
    AbstractParserState c = new CaseInsensitiveLetterState(this, 'C', l,
        null);
    AbstractParserState n = new CaseInsensitiveLetterState(this, 'N', c,
        null);
    newLineState = new WhitespaceOrCaseInsensitiveLetterState(this, 'I', n);
  }

  /**
   * Called by FilenameState at completion of file name production.
   *
   * @param include
   *            include file name
   */
  public void addFilename(final String include) {
    includes.addElement(include);
  }

  /**
   * Gets collection of include file names encountered in parse.
   * @return include file names
   */
  public String[] getIncludes() {
    String[] retval = new String[includes.size()];
    includes.copyInto(retval);
    return retval;
  }

  /**
   * Get the state for the beginning of a new line.
   * @return start of line state
   */
  public AbstractParserState getNewLineState() {
    return newLineState;
  }

  /**
   * Collects all included files from the content of the reader.
   *
   * @param reader
   *            character reader containing a FORTRAN source module
   * @throws IOException
   *             throw if I/O error during parse
   */
  public void parse(final Reader reader) throws IOException {
    includes.setSize(0);
    super.parse(reader);
  }
}
