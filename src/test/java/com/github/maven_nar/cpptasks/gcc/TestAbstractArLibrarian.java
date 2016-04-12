/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.gcc;

import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.compiler.TestAbstractLinker;

/**
 * Tests for classes that derive from AbstractArLibrarian
 *
 * @author CurtA
 */
public class TestAbstractArLibrarian extends TestAbstractLinker {
  /**
   * Constructor
   * 
   * @param name
   *          test name
   * @see junit.framework.TestCase#TestCase(String)
   */
  public TestAbstractArLibrarian(final String name) {
    super(name);
  }

  /**
   * Creates item under test @returns item under test
   * 
   * @see com.github.maven_nar.cpptasks.compiler.TestAbstractProcessor#create()
   */
  @Override
  protected AbstractProcessor create() {
    return GccLibrarian.getInstance();
  }

  /**
   * Override of
   * 
   * @see com.github.maven_nar.cpptasks.compiler.TestAbstractProcessor#testBid()
   */
  @Override
  public void testBid() {
    final AbstractProcessor compiler = create();
    final int bid = compiler.bid("c:/foo\\bar\\hello.o");
    assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, bid);
  }

  @Override
  public void testGetIdentfier() {
    final AbstractProcessor processor = create();
    final String id = processor.getIdentifier();
    assertTrue(id.contains("ar"));
  }

  /**
   * Tests for library patterns
   * 
   * See patch [ 676276 ] Enhanced support for Mac OS X
   */
  public void testGetLibraryPatterns() {
    final String[] libnames = new String[] {
      "foo"
    };
    final String[] patterns = ((AbstractArLibrarian) create()).getLibraryPatterns(libnames, null);
    assertEquals(0, patterns.length);
  }

  /**
   * Tests output file for ar library
   * 
   * See bug [ 687732 ] Filenames for gcc static library does start with lib
   */
  public void testOutputFileName() {
    final String[] outputFiles = GccLibrarian.getInstance().getOutputFileNames("x", null);
    assertEquals("libx.a", outputFiles[0]);
  }
}
