package com.github.maven_nar.cpptasks.parser;

import junit.framework.TestCase;

import java.io.CharArrayReader;
import java.io.IOException;

import com.github.maven_nar.cpptasks.parser.FortranParser;

/**
 * Tests for the CParser class.
 */
public final class TestFortranParser
    extends TestCase {
  /**
   * Constructor.
   * @param name String test name
   */
  public TestFortranParser(final String name) {
    super(name);
  }

  /**
   * Checks parsing of INCLUDE 'foo.inc'.
   * @throws IOException test fails on IOException
   */
  public void testINCLUDE() throws IOException {
    CharArrayReader reader = new CharArrayReader(
        "INCLUDE 'foo.inc' nowhatever  ".toCharArray());
    FortranParser parser = new FortranParser();
    parser.parse(reader);
    String[] includes = parser.getIncludes();
    assertEquals(includes.length, 1);
    assertEquals("foo.inc", includes[0]);
  }

  /**
   * Checks parsing of InClUdE 'foo.inc'.
   * @throws IOException test fails on IOException
   */
  public void testInClUdE() throws IOException {
    CharArrayReader reader = new CharArrayReader("InClUdE 'foo.inc'  "
                                                 .toCharArray());
    FortranParser parser = new FortranParser();
    parser.parse(reader);
    String[] includes = parser.getIncludes();
    assertEquals(includes.length, 1);
    assertEquals("foo.inc", includes[0]);
  }

  /**
   * Checks parsing of InClUdE 'foo.inc'.
   * @throws IOException test fails on IOException
   */
  public void testMultipleInClUdE() throws IOException {
    CharArrayReader reader = new CharArrayReader(
        "InClUdE 'foo.inc'\ninclude 'bar.inc'  "
        .toCharArray());
    FortranParser parser = new FortranParser();
    parser.parse(reader);
    String[] includes = parser.getIncludes();
    assertEquals(includes.length, 2);
    assertEquals("foo.inc", includes[0]);
    assertEquals("bar.inc", includes[1]);
  }

}
