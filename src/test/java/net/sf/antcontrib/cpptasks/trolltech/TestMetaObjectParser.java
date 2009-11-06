/*
 *
 * Copyright 2004 The Ant-Contrib project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.antcontrib.cpptasks.trolltech;

import java.io.CharArrayReader;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * Tests for the MetaObjectParser class.
 */
public final class TestMetaObjectParser
    extends TestCase {
  /**
   * Constructor.
   * @param name String test name
   */
  public TestMetaObjectParser(final String name) {
    super(name);
  }

  /**
   * Test that the presence of Q_OBJECT causes hasQObject to return true.
   * @throws IOException test fails on IOException
   */
  public void testHasQObject1() throws IOException {
    CharArrayReader reader = new CharArrayReader(
        "    Q_OBJECT  ".toCharArray());
    boolean hasQObject = MetaObjectParser.hasQObject(reader);
    assertTrue(hasQObject);
  }

  /**
   * Test that the lack of Q_OBJECT causes hasQObject to return false.
   * @throws IOException test fails on IOException
   */
  public void testHasQObject2() throws IOException {
    CharArrayReader reader = new CharArrayReader(
        "    Q_OBJ ECT  ".toCharArray());
    boolean hasQObject = MetaObjectParser.hasQObject(reader);
    assertFalse(hasQObject);
  }

}
