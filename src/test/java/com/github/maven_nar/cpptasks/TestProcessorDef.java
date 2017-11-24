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

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;
import com.github.maven_nar.cpptasks.types.ConditionalFileSet;

/**
 * Tests for ProcessorDef.
 */
public abstract class TestProcessorDef extends TestCase {

  /**
   * Constructor.
   *
   * @param name
   *          test name
   */
  public TestProcessorDef(final String name) {
    super(name);
  }

  /**
   * Creates a new processor definition.
   *
   * @return created processor definition
   */
  protected abstract ProcessorDef create();

  /**
   * Creates a processor initialized to be an extension of the base processor.
   *
   * @param baseProcessor
   *          base processor
   * @return extending processor
   */
  protected final ProcessorDef createExtendedProcessorDef(final ProcessorDef baseProcessor) {
    final Project project = new Project();
    baseProcessor.setProject(project);
    baseProcessor.setId("base");
    project.addReference("base", baseProcessor);
    final ProcessorDef extendedLinker = create();
    extendedLinker.setProject(project);
    extendedLinker.setExtends(new Reference("base"));
    return extendedLinker;
  }

  /**
   * Gets the processor configuration.
   *
   * @param extendedProcessor
   *          processor under test
   * @return configuration
   */
  protected final ProcessorConfiguration getConfiguration(final ProcessorDef extendedProcessor) {
    final CCTask cctask = new CCTask();
    final LinkType linkType = new LinkType();
    final File objDir = new File("dummy");
    cctask.setObjdir(objDir);
    return extendedProcessor.createConfiguration(cctask, linkType, null, null, null);
  }

  /**
   * Gets command line arguments that precede filenames.
   *
   * @param processor
   *          processor under test
   * @return array of command line parameters
   */
  protected abstract String[] getPreArguments(final ProcessorDef processor);

  /**
   * Tests that the debug attribute in the base processor is effective when
   * creating the command line for a processor that extends it.
   */
  public final void testExtendsDebug() {
    final ProcessorDef baseLinker = create();
    baseLinker.setDebug(true);
    final ProcessorDef extendedLinker = createExtendedProcessorDef(baseLinker);
    final String[] preArgs = getPreArguments(extendedLinker);
    // FREEHEP, passes (sometimes) extra option
    assertEquals("-g", preArgs[Math.max(preArgs.length - 2, 0)]);
  }

  /**
   * Tests that a fileset in the base processor is effective when evaluating
   * the files included in an extending processor.
   *
   * @param tempFile
   *          temporary file
   * @throws IOException
   *           if unable to delete file
   */
  protected final void testExtendsFileSet(final File tempFile) throws IOException {
    final ProcessorDef baseLinker = create();
    final ConditionalFileSet fileSet = new ConditionalFileSet();
    final ProcessorDef extendedLinker = createExtendedProcessorDef(baseLinker);
    fileSet.setProject(baseLinker.getProject());
    fileSet.setDir(new File(tempFile.getParent()));
    fileSet.setIncludes(tempFile.getName());
    baseLinker.addFileset(fileSet);
    final MockFileCollector collector = new MockFileCollector();
    extendedLinker.visitFiles(collector);
    tempFile.delete();
    assertEquals(1, collector.size());
  }

  /**
   * Tests that the if attribute in the base processor is effective when
   * evaluating if an extending processor is active.
   */
  public final void testExtendsIf() {
    final ProcessorDef baseLinker = create();
    baseLinker.setIf("bogus");
    final ProcessorDef extendedLinker = createExtendedProcessorDef(baseLinker);
    boolean isActive = extendedLinker.isActive();
    assertEquals(false, isActive);
    baseLinker.getProject().setProperty("bogus", "");
    isActive = extendedLinker.isActive();
    assertEquals(true, isActive);
  }

  /**
   * Tests that the rebuild attribute in the base processor is effective when
   * creating the command line for a processor that extends it.
   *
   * @param baseProcessor
   *          processor under test
   */
  protected final void testExtendsRebuild(final ProcessorDef baseProcessor) {
    baseProcessor.setRebuild(true);
    final ProcessorDef extendedLinker = createExtendedProcessorDef(baseProcessor);
    final ProcessorConfiguration config = getConfiguration(extendedLinker);
    final boolean rebuild = config.getRebuild();
    assertEquals(true, rebuild);
  }

  /**
   * Tests that the unless attribute in the base processor is effective when
   * evaluating if an extending processor is active.
   */
  public final void testExtendsUnless() {
    final ProcessorDef baseLinker = create();
    baseLinker.setUnless("bogus");
    final ProcessorDef extendedLinker = createExtendedProcessorDef(baseLinker);
    boolean isActive = extendedLinker.isActive();
    assertEquals(true, isActive);
    baseLinker.getProject().setProperty("bogus", "");
    isActive = extendedLinker.isActive();
    assertEquals(false, isActive);
  }

  /**
   * Tests that isActive returns true when "if" references a set property.
   */
  public final void testIsActive2() {
    final ProcessorDef arg = create();
    final Project project = new Project();
    project.setProperty("cond", "");
    arg.setProject(project);
    arg.setIf("cond");
    assertTrue(arg.isActive());
  }

  /**
   * Tests that isActive returns false when "if" references an unset property.
   */
  public final void testIsActive3() {
    final ProcessorDef arg = create();
    arg.setProject(new Project());
    arg.setIf("cond");
    assertTrue(!arg.isActive());
  }

  /**
   * Tests that evaluating isActive when "if" refernces a property with the
   * value "false" throws an exception to warn of a suspicious value.
   *
   */
  public final void testIsActive4() {
    final ProcessorDef arg = create();
    final Project project = new Project();
    project.setProperty("cond", "false");
    arg.setProject(project);
    arg.setIf("cond");
    try {
      final boolean isActive = arg.isActive();
    } catch (final BuildException ex) {
      return;
    }
    fail("Should throw exception for suspicious value");
  }

  /**
   * Tests that isActive returns false when "unless" references a set
   * property.
   */
  public final void testIsActive5() {
    final ProcessorDef arg = create();
    final Project project = new Project();
    project.setProperty("cond", "");
    arg.setProject(project);
    arg.setUnless("cond");
    assertTrue(!arg.isActive());
  }

  /**
   * Tests that isActive returns true when "unless" references an unset
   * property.
   */
  public final void testIsActive6() {
    final ProcessorDef arg = create();
    arg.setProject(new Project());
    arg.setUnless("cond");
    assertTrue(arg.isActive());
  }

  /**
   * Tests that evaluating isActive when "unless" references a property with
   * the value "false" throws an exception to warn of a suspicious value.
   *
   */
  public final void testIsActive7() {
    final ProcessorDef arg = create();
    final Project project = new Project();
    project.setProperty("cond", "false");
    arg.setProject(project);
    arg.setUnless("cond");
    try {
      final boolean isActive = arg.isActive();
    } catch (final BuildException ex) {
      return;
    }
    fail("Should throw exception for suspicious value");
  }

  /**
   * Tests if a processor is active when both "if" and "unless" are specified
   * and the associated properties are set.
   *
   */
  public final void testIsActive8() {
    final ProcessorDef arg = create();
    final Project project = new Project();
    project.setProperty("cond", "");
    arg.setProject(project);
    arg.setIf("cond");
    arg.setUnless("cond");
    assertTrue(!arg.isActive());
  }
}
