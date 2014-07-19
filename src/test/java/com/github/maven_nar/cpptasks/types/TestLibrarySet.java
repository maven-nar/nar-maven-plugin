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
package com.github.maven_nar.cpptasks.types;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.MockBuildListener;
import com.github.maven_nar.cpptasks.MockFileCollector;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.msvc.MsvcLibrarian;
import com.github.maven_nar.cpptasks.msvc.MsvcLinker;
import com.github.maven_nar.cpptasks.types.LibrarySet;

/**
 * Tests for the LibrarySet class.
 */
public class TestLibrarySet
    extends TestCase {

  /**
   * Constructor.
   *
   * @param name
   *            test name
   */
  public TestLibrarySet(final String name) {
    super(name);
  }

  /**
   * Evaluate isActive when "if" specifies a property that is set.
   */
  public final void testIsActive1() {
    LibrarySet libset = new LibrarySet();
    Project project = new Project();
    project.setProperty("windows", "");
    libset.setProject(project);
    libset.setIf("windows");
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("kernel32");
    libset.setLibs(libs);
    boolean isActive = libset.isActive(project);
    assertTrue(isActive);
  }

  /**
   * Evaluate isActive when "if" specifies a property whose value suggests the
   * user thinks the value is significant.
   *
   */
  public final void testIsActive2() {
    LibrarySet libset = new LibrarySet();
    Project project = new Project();
    //
    // setting the value to false should throw
    //    exception to warn user that they are misusing if
    //
    project.setProperty("windows", "false");
    libset.setIf("windows");
    try {
      boolean isActive = libset.isActive(project);
    } catch (BuildException ex) {
      return;
    }
    fail();
  }

  /**
   * Evaluate isActive when "if" specifies a property that is not set.
   */
  public final void testIsActive3() {
    LibrarySet libset = new LibrarySet();
    Project project = new Project();
    libset.setIf("windows");
    boolean isActive = libset.isActive(project);
    assertTrue(!isActive);
  }

  /**
   * Evaluate isActive when "unless" specifies a property that is set.
   *
   */
  public final void testIsActive4() {
    LibrarySet libset = new LibrarySet();
    Project project = new Project();
    project.setProperty("windows", "");
    libset.setUnless("windows");
    boolean isActive = libset.isActive(project);
    assertTrue(!isActive);
  }

  /**
   * Evaluate isActive when "unless" specifies a property whose value suggests
   * the user thinks the value is significant.
   *
   */
  public final void testIsActive5() {
    LibrarySet libset = new LibrarySet();
    Project project = new Project();
    //
    // setting the value to false should throw
    //    exception to warn user that they are misusing if
    //
    project.setProperty("windows", "false");
    libset.setUnless("windows");
    try {
      boolean isActive = libset.isActive(project);
    } catch (BuildException ex) {
      return;
    }
    fail();
  }

  /**
   * Evaluate isActive when "unless" specifies a property that is not set.
   */
  public final void testIsActive6() {
    LibrarySet libset = new LibrarySet();
    Project project = new Project();
    libset.setProject(project);
    libset.setUnless("windows");
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("kernel32");
    libset.setLibs(libs);
    boolean isActive = libset.isActive(project);
    assertTrue(isActive);
  }

  /**
   * The libs parameter should not end with .lib, .so, .a etc New behavior is
   * to warn if it ends in a suspicious extension.
   */
  public final void testLibContainsDot() {
    LibrarySet libset = new LibrarySet();
    Project p = new Project();
    MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("mylib1.1");
    libset.setLibs(libs);
    assertEquals(0, listener.getMessageLoggedEvents().size());
  }

  /**
   * The libs parameter should not end with .lib, .so, .a (that is,
   * should be kernel, not kernel.lib).  Previously the libset would
   * warn on configuration, now provides more feedback
   * when library is not found.
   */
  public final void testLibContainsDotLib() {
    LibrarySet libset = new LibrarySet();
    Project p = new Project();
    MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder(
        "mylib1.lib");
    libset.setLibs(libs);
    assertEquals(0, listener.getMessageLoggedEvents().size());
  }

  /**
   * Use of a libset or syslibset without a libs attribute should log a
   * warning message.
   */
  public final void testLibNotSpecified() {
    LibrarySet libset = new LibrarySet();
    Project p = new Project();
    MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    boolean isActive = libset.isActive(p);
    assertEquals(false, isActive);
    assertEquals(1, listener.getMessageLoggedEvents().size());
  }

  /**
   * this threw an exception prior to 2002-09-05 and started to throw one
   * again 2002-11-19 up to 2002-12-11.
   */
  public final void testShortLibName() {
    LibrarySet libset = new LibrarySet();
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("li");
    libset.setProject(new Project());
    libset.setLibs(libs);
  }

  /**
   * The libs parameter should contain not a lib prefix (that is,
   * pthread not libpthread).  Previously the libset would
   * warn on configuration, now provides more feedback
   * when library is not found.
   */
  public final void testStartsWithLib() {
    LibrarySet libset = new LibrarySet();
    Project p = new Project();
    MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder(
        "libmylib1");
    libset.setLibs(libs);
    assertEquals(0, listener.getMessageLoggedEvents().size());
  }

  /**
   * This test creates two "fake" libraries in the temporary directory and
   * check how many are visited.
   *
   * @param linker linker
   * @param expected expected number of visited files
   * @throws IOException
   *             if unable to write to temporary directory or delete temporary
   *             files
   */
  public final void testVisitFiles(final Linker linker,
                                   final int expected)
      throws IOException {
    LibrarySet libset = new LibrarySet();
    Project p = new Project();
    MockBuildListener listener = new MockBuildListener();
    p.addBuildListener(listener);
    libset.setProject(p);
    //
    //   create temporary files named cpptasksXXXXX.lib
    //
    File lib1 = File.createTempFile("cpptasks", ".lib");
    String lib1Name = lib1.getName();
    lib1Name = lib1Name.substring(0, lib1Name.indexOf(".lib"));
    File lib2 = File.createTempFile("cpptasks", ".lib");
    File baseDir = lib1.getParentFile();

    //   set the dir attribute to the temporary directory
    libset.setDir(baseDir);
    //   set libs to the file name without the suffix
    CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder(lib1Name);
    libset.setLibs(libs);

    //
    //   collect all files visited
    MockFileCollector collector = new MockFileCollector();
    libset.visitLibraries(p, linker, new File[0], collector);

    //
    //  get the canonical paths for the initial and visited libraries
    String expectedCanonicalPath = lib1.getCanonicalPath();
    String actualCanonicalPath = null;
    if (collector.size() == 1) {
      actualCanonicalPath = new File(collector.getBaseDir(0), collector
                                     .getFileName(0)).getCanonicalPath();
    }
    //
    //  delete the temporary files
    lib1.delete();
    lib2.delete();
    //   was there only one match
    assertEquals(expected, collector.size());
    if (expected == 1) {
      //   is its canonical path as expected
      assertEquals(expectedCanonicalPath, actualCanonicalPath);
    }
  }

  /**
   * Run testVisitFiles with the MSVC Linker
   * expect one matching file.
   *
   * @throws IOException if unable to create or delete temporary file
   */
  public final void testLinkerVisitFiles() throws IOException {
    Linker linker = MsvcLinker.getInstance();
    testVisitFiles(linker, 1);
  }

  /**
   * Run testVisitFiles with the MSVC Librarian
   * expect one matching file.
   *
   * @throws IOException if unable to create or delete temporary file
   */
  public final void testLibrarianVisitFiles() throws IOException {
    Linker linker = MsvcLibrarian.getInstance();
    testVisitFiles(linker, 0);
  }


    /**
     * This test specifies a library pattern that should
     * not match any available libraries and expects that
     * a build exception will be raised.
     *
     * See bug 1380366
     */
    public final void testBadLibname() {
      LibrarySet libset = new LibrarySet();
      Project p = new Project();
      MockBuildListener listener = new MockBuildListener();
      p.addBuildListener(listener);
      libset.setProject(p);
      //   set libs to the file name without the suffix
      CUtil.StringArrayBuilder libs = new CUtil.StringArrayBuilder("badlibname");
      libset.setLibs(libs);

      //
      //   collect all files visited
      MockFileCollector collector = new MockFileCollector();
      try {
        libset.visitLibraries(p, MsvcLinker.getInstance(), new File[0], collector);
      } catch(BuildException ex) {
          return;
      }
//
//      code around line 320 in LibrarySet that would throw BuildException
//         (and prevent reaching this line) is disabled since logic for identifying
//         missing libraries does not work reliably on non-Windows platforms
//       
//      fail("visitLibraries should throw exception due to unsatisifed libname");
    }

}
