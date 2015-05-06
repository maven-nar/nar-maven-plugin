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
package com.github.maven_nar.cpptasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import junit.framework.TestCase;

import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.gcc.GccCCompiler;

/**
 * Tests for CCTask.
 *
 */
public final class TestCCTask extends TestCase {
  /**
   * Constructor.
   * 
   * @param name
   *          test name
   *
   */
  public TestCCTask(final String name) {
    super(name);
  }

  /**
   * Test checks for the presence of antlib.xml.
   * 
   * @throws IOException
   *           if stream can't be closed.
   *
   */
  public void testAntlibXmlPresent() throws IOException {
    final InputStream stream = TestCCTask.class.getClassLoader().getResourceAsStream(
        "com/github/maven_nar/cpptasks/antlib.xml");
    if (stream != null) {
      stream.close();
    }
    assertNotNull("antlib.xml missing", stream);
  }

  /**
   * Tests that the default value of failonerror is true.
   */
  public void testGetFailOnError() {
    final CCTask task = new CCTask();
    final boolean failOnError = task.getFailonerror();
    assertEquals(true, failOnError);
  }

  /**
   * Test that a target with no existing object file is
   * returned by getTargetsToBuildByConfiguration.
   */
  public void testGetTargetsToBuildByConfiguration1() {
    final CompilerConfiguration config1 = new CommandLineCompilerConfiguration(GccCCompiler.getInstance(), "dummy",
        new File[0], new File[0], new File[0], "", new String[0], new ProcessorParam[0], true, new String[0]);
    final TargetInfo target1 = new TargetInfo(config1, new File[] {
      new File("src/foo.bar")
    }, null, new File("foo.obj"), true);
    final Map targets = new HashMap();
    targets.put(target1.getOutput(), target1);
    final Map targetsByConfig = CCTask.getTargetsToBuildByConfiguration(targets);
    final Vector targetsForConfig1 = (Vector) targetsByConfig.get(config1);
    assertNotNull(targetsForConfig1);
    assertEquals(1, targetsForConfig1.size());
    final TargetInfo targetx = (TargetInfo) targetsForConfig1.elementAt(0);
    assertSame(target1, targetx);
  }

  /**
   * Test that a target that is up to date is not returned by
   * getTargetsToBuildByConfiguration.
   *
   */
  public void testGetTargetsToBuildByConfiguration2() {
    final CompilerConfiguration config1 = new CommandLineCompilerConfiguration(GccCCompiler.getInstance(), "dummy",
        new File[0], new File[0], new File[0], "", new String[0], new ProcessorParam[0], false, new String[0]);
    //
    // target doesn't need to be rebuilt
    //
    final TargetInfo target1 = new TargetInfo(config1, new File[] {
      new File("src/foo.bar")
    }, null, new File("foo.obj"), false);
    final Map targets = new HashMap();
    targets.put(target1.getOutput(), target1);
    //
    // no targets need to be built, return a zero-length hashtable
    //
    final Map targetsByConfig = CCTask.getTargetsToBuildByConfiguration(targets);
    assertEquals(0, targetsByConfig.size());
  }

  /**
   * Tests that setting failonerror is effective.
   */
  public void testSetFailOnError() {
    final CCTask task = new CCTask();
    task.setFailonerror(false);
    boolean failOnError = task.getFailonerror();
    assertEquals(false, failOnError);
    task.setFailonerror(true);
    failOnError = task.getFailonerror();
    assertEquals(true, failOnError);
  }
}
