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
package com.github.maven_nar.cpptasks.compiler;

import junit.framework.TestCase;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.VersionInfo;

/**
 * Test for abstract compiler class
 *
 * Override create to test concrete compiler implementions
 */
public class TestAbstractProcessor extends TestCase {
  private class DummyAbstractProcessor extends AbstractProcessor {
    public DummyAbstractProcessor() {
      super(new String[] {
          ".cpp", ".c"
      }, new String[] {
          ".hpp", ".h", ".inl"
      });
    }

    @Override
    public ProcessorConfiguration createConfiguration(final CCTask task, final LinkType linkType,
        final ProcessorDef[] defaultProvider, final ProcessorDef specificProvider,
        final com.github.maven_nar.cpptasks.TargetDef targetPlatform, final VersionInfo versionInfo) {
      return null;
    }

    @Override
    public String getIdentifier() {
      return "dummy";
    }

    @Override
    public Linker getLinker(final LinkType type) {
      return null;
    }

    @Override
    public String[] getOutputFileNames(final String sourceFile, final VersionInfo versionInfo) {
      return new String[0];
    }

    public String[][] getRuntimeLibraries(final boolean debug, final boolean multithreaded, final boolean staticLink) {
      return new String[2][0];
    }
  }

  public TestAbstractProcessor(final String name) {
    super(name);
  }

  protected AbstractProcessor create() {
    return new DummyAbstractProcessor();
  }

  public void failingtestBid() {
    final AbstractProcessor compiler = create();
    int bid = compiler.bid("c:/foo\\bar\\hello.c");
    assertEquals(100, bid);
    bid = compiler.bid("c:/foo\\bar/hello.c");
    assertEquals(100, bid);
    bid = compiler.bid("c:/foo\\bar\\hello.h");
    assertEquals(1, bid);
    bid = compiler.bid("c:/foo\\bar/hello.h");
    assertEquals(1, bid);
    bid = compiler.bid("c:/foo\\bar/hello.pas");
    assertEquals(0, bid);
    bid = compiler.bid("c:/foo\\bar/hello.java");
    assertEquals(0, bid);
  }

  public void testGetIdentfier() {
    final AbstractProcessor compiler = create();
    final String id = compiler.getIdentifier();
    assertEquals("dummy", id);
  }
}
