package com.github.maven_nar.cpptasks.trolltech;

import java.io.IOException;
import java.io.Reader;

import com.github.maven_nar.cpptasks.parser.Parser;


/**
 * Dependency scanner for Trolltech Qt User Interface definition files.
 *
 * .ui files are XML documents that may contain an include elements,
 * however the includes are just copied to the generated files and
 * and changes to the includes do not need to trigger rerunning uic.
 *
 * @author Curt Arnold
 */
public final class UserInterfaceParser
    implements Parser {

  /**
   *   Constructor.
   *
   */
  public UserInterfaceParser() {
  }

  /**
   * Adds filename to the list of included files.
   *
   * @param include String included file name
   */
  public void addFilename(final String include) {
  }

  /**
   * Gets included files.
   * @return String[] included files
   */
  public String[] getIncludes() {
    return new String[0];
  }

  /**
   * Parses source file for dependencies.
   *
   * @param reader Reader reader
   * @throws IOException if error reading source file
   */
  public void parse(final Reader reader) throws IOException {
  }
}
