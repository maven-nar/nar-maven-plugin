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
package com.github.maven_nar.cpptasks.ibm;

import junit.framework.TestCase;

import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;

/**
 * Test IBM Visual Age compiler adapter
 * 
 */
// TODO Since VisualAgeCCompiler extends GccCompatibleCCompiler, this test
// should probably extend TestGccCompatibleCCompiler.
public class TestVisualAgeCCompiler extends TestCase {
  public TestVisualAgeCCompiler(final String name) {
    super(name);
  }

  public void testBidAssembly() {
    final VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
    assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.s"));
  }

  public void testBidC() {
    final VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
    assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.c"));
  }

  public void testBidCpp() {
    final VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
    assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.C"));
  }

  public void testBidCpp2() {
    final VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
    assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.cc"));
  }

  public void testBidCpp3() {
    final VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
    assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.cxx"));
  }

  public void testBidCpp4() {
    final VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
    assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.cpp"));
  }

  public void testBidPreprocessed() {
    final VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
    assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler.bid("foo.i"));
  }
}
