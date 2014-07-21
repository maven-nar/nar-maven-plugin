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
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FlexInteger;
import org.apache.tools.ant.types.Reference;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.LinkerEnum;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.msvc.MsvcLinker;
import com.github.maven_nar.cpptasks.gcc.GccLinker;
import com.github.maven_nar.cpptasks.types.FlexLong;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

/**
 * Tests for LinkerDef class.
 */
public final class TestLinkerDef
    extends TestProcessorDef {
  /**
   * Constructor.
   *
   * @param name
   *            test name
   */
  public TestLinkerDef(final String name) {
    super(name);
  }

  /**
   * Creates a processor.
   *
   * @return new linker
   */
  protected ProcessorDef create() {
    return new LinkerDef();
  }

  /**
   * Test if setting the classname attribute to the name of the GCC linker
   * results in the singleton GCC linker.
   */
  public void testGetGcc() {
    LinkerDef linkerDef = (LinkerDef) create();
    linkerDef.setClassname("com.github.maven_nar.cpptasks.gcc.GccLinker");
    Linker comp = (Linker) linkerDef.getProcessor();
    assertNotNull(comp);
    assertSame(GccLinker.getInstance(), comp);
  }

  /**
   * Test if setting the classname attribute to the name of the MSVC linker
   * results in the singleton MSVC linker.
   */
  public void testGetMSVC() {
    LinkerDef linkerDef = (LinkerDef) create();
    linkerDef
        .setClassname("com.github.maven_nar.cpptasks.msvc.MsvcLinker");
    Linker comp = (Linker) linkerDef.getProcessor();
    assertNotNull(comp);
    assertSame(MsvcLinker.getInstance(), comp);
  }

  /**
   * Tests if setting the classname attribute to an bogus classname results in
   * a BuildException.
   *
   */
  public void testUnknownClass() {
    LinkerDef linkerDef = (LinkerDef) create();
    try {
      linkerDef
          .setClassname("com.github.maven_nar.cpptasks.bogus.BogusLinker");
    } catch (BuildException ex) {
      return;
    }
    fail("should have thrown exception");
  }

  /**
   * Tests if setting the classname to the name of a class that doesn't
   * support Linker throws a BuildException.
   *
   */
  public void testWrongType() {
    LinkerDef linkerDef = (LinkerDef) create();
    try {
      linkerDef.setClassname("com.github.maven_nar.cpptasks.CCTask");
    } catch (ClassCastException ex) {
      return;
    }
    fail("should have thrown exception");
  }

  /**
   * Gets the command line arguments that appear before the filenames.
   *
   * @param processor processor under test
   * @return command line arguments
   */
  protected String[] getPreArguments(final ProcessorDef processor) {
    return ((CommandLineLinkerConfiguration) getConfiguration(processor))
        .getPreArguments();
  }

  /**
   * Sets the name attribute.
   *
   * @param linker
   *            linker defintion
   * @param name
   *            linker name
   */
  private static void setLinkerName(final LinkerDef linker,
                                    final String name) {
    LinkerEnum linkerName = new LinkerEnum();
    linkerName.setValue(name);
    linker.setName(linkerName);
  }

  /**
   * Tests that linkerarg's that appear in the base linker are effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsLinkerArgs() {
    LinkerDef baseLinker = new LinkerDef();
    LinkerArgument linkerArg = new LinkerArgument();
    linkerArg.setValue("/base");
    baseLinker.addConfiguredLinkerArg(linkerArg);
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals(1, preArgs.length);
    assertEquals("/base", preArgs[0]);
  }

  /**
   * Verify linkerarg's that appear in the base linker are effective when 
   * creating the command line for a linker that extends it, even if the 
   * linker is brought in through a reference.
   */
  public void testExtendsLinkerArgsViaReference() {
    Project project = new Project();
    LinkerDef baseLinker = new LinkerDef();
    baseLinker.setProject(project);
    baseLinker.setId("base");
    project.addReference("base", baseLinker);
    LinkerArgument linkerArg = new LinkerArgument();
    linkerArg.setValue("/base");
    baseLinker.addConfiguredLinkerArg(linkerArg);

    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    extendedLinker.setProject(project);
    extendedLinker.setId("extended");
    project.addReference("extended", extendedLinker);

    LinkerDef linkerRef = new LinkerDef();
    linkerRef.setProject(project);
    linkerRef.setRefid(new Reference(project, "extended"));
    String[] preArgs = getPreArguments(linkerRef);
    assertEquals(1, preArgs.length);
    assertEquals("/base", preArgs[0]);
  }

  /**
   * Tests that fileset's that appear in the base linker are effective when
   * creating the command line for a linker that extends it.
   * @throws IOException if unable to create or delete temporary file
   */
  public void testExtendsFileSet() throws IOException {
    super.testExtendsFileSet(File.createTempFile("cpptaskstest", ".o"));
  }

  /**
   * Tests that libset's that appear in the base linker are effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsLibSet() {
    LinkerDef baseLinker = new LinkerDef();
    LibrarySet libset = new LibrarySet();
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    libset.setProject(baseLinker.getProject());
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("advapi32");
    libset.setLibs(libs);
    baseLinker.addLibset(libset);
    CommandLineLinkerConfiguration config = (CommandLineLinkerConfiguration)
        getConfiguration(extendedLinker);
    String[] libnames = config.getLibraryNames();
    assertEquals(1, libnames.length);
    assertEquals("advapi32", libnames[0]);
  }

  /**
   * Tests that syslibset's that appear in the base linker are effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsSysLibSet() {
    LinkerDef baseLinker = new LinkerDef();
    SystemLibrarySet libset = new SystemLibrarySet();
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    libset.setProject(baseLinker.getProject());
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("advapi32");
    libset.setLibs(libs);
    baseLinker.addSyslibset(libset);
    CommandLineLinkerConfiguration config = (CommandLineLinkerConfiguration)
        getConfiguration(extendedLinker);
    String[] libnames = config.getLibraryNames();
    assertEquals(1, libnames.length);
    assertEquals("advapi32", libnames[0]);
  }

  /**
   * Tests that the base attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsBase() {
    LinkerDef baseLinker = new LinkerDef();
    baseLinker.setBase(new FlexLong("10000"));
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    setLinkerName(extendedLinker, "msvc");
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("/NOLOGO", preArgs[0]);
    assertEquals("/SUBSYSTEM:WINDOWS", preArgs[1]);
    assertEquals("/INCREMENTAL:NO", preArgs[2]);
    assertEquals("/BASE:0x2710", preArgs[3]);
  }

  /**
   * Tests that the stack attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsStack() {
    LinkerDef baseLinker = new LinkerDef();
    baseLinker.setStack(new FlexInteger("10000"));
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    setLinkerName(extendedLinker, "msvc");
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("/NOLOGO", preArgs[0]);
    assertEquals("/SUBSYSTEM:WINDOWS", preArgs[1]);
    assertEquals("/INCREMENTAL:NO", preArgs[2]);
    assertEquals("/STACK:0x2710", preArgs[3]);
  }

  /**
   * Tests that the entry attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsEntry() {
    LinkerDef baseLinker = new LinkerDef();
    baseLinker.setEntry("foo");
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("-e", preArgs[0]);
    assertEquals("foo", preArgs[1]);
  }

  /**
   * Tests that the fixed attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsFixed() {
    LinkerDef baseLinker = new LinkerDef();
    baseLinker.setFixed(true);
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    setLinkerName(extendedLinker, "msvc");
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("/NOLOGO", preArgs[0]);
    assertEquals("/SUBSYSTEM:WINDOWS", preArgs[1]);
    assertEquals("/INCREMENTAL:NO", preArgs[2]);
    assertEquals("/FIXED", preArgs[3]);
  }

  /**
   * Tests that the incremental attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsIncremental() {
    LinkerDef baseLinker = new LinkerDef();
    baseLinker.setIncremental(true);
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    setLinkerName(extendedLinker, "msvc");
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("/NOLOGO", preArgs[0]);
    assertEquals("/SUBSYSTEM:WINDOWS", preArgs[1]);
    assertEquals("/INCREMENTAL:YES", preArgs[2]);
  }

  /**
   * Tests that the map attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsMap() {
    LinkerDef baseLinker = new LinkerDef();
    baseLinker.setMap(true);
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    setLinkerName(extendedLinker, "msvc");
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("/NOLOGO", preArgs[0]);
    assertEquals("/SUBSYSTEM:WINDOWS", preArgs[1]);
    assertEquals("/INCREMENTAL:NO", preArgs[2]);
    assertEquals("/MAP", preArgs[3]);
  }

  /**
   * Tests that the rebuild attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsRebuild() {
    testExtendsRebuild(new LinkerDef());
  }

  /**
   * Tests that the name attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsName() {
    LinkerDef baseLinker = new LinkerDef();
    setLinkerName(baseLinker, "msvc");
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    extendedLinker.setBase(new FlexLong("10000"));
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("/NOLOGO", preArgs[0]);
    assertEquals("/SUBSYSTEM:WINDOWS", preArgs[1]);
    assertEquals("/INCREMENTAL:NO", preArgs[2]);
    assertEquals("/BASE:0x2710", preArgs[3]);
  }

  /**
   * Tests that the classname attribute in the base linker is effective when
   * creating the command line for a linker that extends it.
   */
  public void testExtendsClassname() {
    LinkerDef baseLinker = new LinkerDef();
    baseLinker
        .setClassname("com.github.maven_nar.cpptasks.msvc.MsvcLinker");
    LinkerDef extendedLinker = (LinkerDef) createExtendedProcessorDef(
        baseLinker);
    extendedLinker.setBase(new FlexLong("10000"));
    String[] preArgs = getPreArguments(extendedLinker);
    assertEquals("/NOLOGO", preArgs[0]);
    assertEquals("/SUBSYSTEM:WINDOWS", preArgs[1]);
    assertEquals("/INCREMENTAL:NO", preArgs[2]);
    assertEquals("/BASE:0x2710", preArgs[3]);
  }
}
