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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;

import junit.framework.TestCase;

/**
 * Tests the actual linker arguments without requiring installed cross
 * compilers.
 */
public class TestGnuLinkerMap extends TestCase {
  private void assertDriverMap(final AbstractLdLinker linker, final String output) {
    for (final boolean decorate : new boolean[] {
        false, true
    }) {
      final List<String> args = prepare(linker, output, true, decorate);
      assertMapArguments(args, "-Map=" + output + ".map");
      assertEquals("-Xlinker", args.get(args.indexOf("-Map=" + output + ".map") - 1));
    }
  }

  private void assertMapArguments(final List<String> args, final String expected) {
    assertTrue(args.toString(), args.contains(expected));
    int count = 0;
    for (final String arg : args) {
      if (arg.contains("-Map")) {
        count++;
      }
      assertFalse(args.toString(), arg.equals("-M") || arg.equals("-Wl,-M") || arg.contains("-Wl,-Wl,"));
    }
    assertEquals(args.toString(), 1, count);
  }

  private CommandLineLinkerConfiguration configuration(final AbstractLdLinker linker, final CCTask task,
      final boolean map, final String[] libraries) {
    final Vector<String> args = new Vector<>();
    linker.addMap(task, map, args);
    return new CommandLineLinkerConfiguration(linker, "test", new String[][] {
        args.toArray(new String[args.size()]), new String[0]
    }, new ProcessorParam[0], false, map, false, libraries, null);
  }

  private List<String> prepare(final AbstractLdLinker linker, final String output, final boolean map,
      final boolean decorate) {
    final CCTask task = new CCTask();
    final org.apache.tools.ant.Project project = new org.apache.tools.ant.Project();
    project.setProperty("nar.os", "Linux");
    task.setProject(project);
    task.setOutfile(new File("output directory/undecorated-base"));
    task.setDecorateLinkerOptions(decorate);
    return Arrays.asList(linker.prepareArguments(task, "output directory", output, new String[] {
        "object.o"
    }, configuration(linker, task, map, new String[0])));
  }

  public void testCommaInOutputNameIsNotSplitByGcc() {
    // -Wl splits at commas, so a literal comma in the map filename requires
    // -Xlinker.
    for (final boolean decorate : new boolean[] {
        false, true
    }) {
      final List<String> args = prepare(GppLinker.getInstance(), "libcustom,name.so", true, decorate);
      assertMapArguments(args, "-Map=libcustom,name.so.map");
      assertEquals("-Xlinker", args.get(args.indexOf("-Map=libcustom,name.so.map") - 1));
    }
  }

  public void testConfiguredTargetOverridesHost() {
    final String host = System.getProperty("os.name");
    try {
      System.setProperty("os.name", "Mac OS X");
      // prepare() explicitly configures a Linux target.
      assertDriverMap(GccLinker.getInstance(), "libprobe.so");
    } finally {
      System.setProperty("os.name", host);
    }
  }

  public void testCrossCompilerMapFiles() {
    assertDriverMap(com.github.maven_nar.cpptasks.gcc.cross.GccLinker.getInstance(), "libprobe.so");
    assertDriverMap(com.github.maven_nar.cpptasks.gcc.cross.GppLinker.getInstance(), "libprobe.so");
    assertDriverMap(com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GccLinker.getInstance(), "libprobe.so");
    assertDriverMap(com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GppLinker.getInstance(), "libprobe.so");
  }

  public void testDarwinMapArgumentsForDriversAndDirectLd() {
    for (final AbstractLdLinker linker : new AbstractLdLinker[] {
        GccLinker.getInstance(), GppLinker.getInstance(), GccLinker.getCLangInstance(), GppLinker.getCLangInstance(),
        LdLinker.getInstance()
    }) {
      for (final boolean decorate : new boolean[] {
          false, true
      }) {
        final CCTask task = new CCTask();
        final org.apache.tools.ant.Project project = new org.apache.tools.ant.Project();
        project.setProperty("nar.os", "MacOSX");
        task.setProject(project);
        task.setDecorateLinkerOptions(decorate);
        final List<String> args = Arrays.asList(linker.prepareArguments(task, ".", "libprobe.dylib", new String[] {
            "object.o"
        }, configuration(linker, task, true, new String[0])));
        assertTrue(args.toString(), args.contains("-map"));
        final int index = args.indexOf("-map");
        if (linker == LdLinker.getInstance()) {
          assertEquals("libprobe.dylib.map", args.get(index + 1));
          assertFalse(args.contains("-Xlinker"));
        } else {
          assertEquals("-Xlinker", args.get(index - 1));
          assertEquals("-Xlinker", args.get(index + 1));
          assertEquals("libprobe.dylib.map", args.get(index + 2));
        }
      }
    }
  }

  public void testDirectLdMapFiles() {
    final AbstractLdLinker[] linkers = {
        LdLinker.getInstance(), com.github.maven_nar.cpptasks.gcc.cross.LdLinker.getInstance(),
        com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.LdLinker.getInstance()
    };
    for (final AbstractLdLinker linker : linkers) {
      for (final boolean decorate : new boolean[] {
          false, true
      }) {
        assertMapArguments(prepare(linker, "libprobe.so", true, decorate), "-Map=libprobe.so.map");
      }
    }
  }

  public void testFinalOutputNameOverridesTaskBaseName() {
    // The task still contains the undecorated base name; the link target has the
    // final native filename.
    assertDriverMap(GppLinker.getInstance(), "libcustom.so.2");
    assertDriverMap(GccLinker.getInstance(), "custom-executable");
  }

  public void testGccMapFile() {
    assertDriverMap(GccLinker.getInstance(), "libprobe-1.2.so");
    assertDriverMap(GccLinker.getCLangInstance(), "libprobe-1.2.so");
  }

  public void testGppMapFile() {
    assertDriverMap(GppLinker.getInstance(), "libprobe-1.2.so");
    assertDriverMap(GppLinker.getCLangInstance(), "libprobe-1.2.so");
  }

  public void testLibraryFilteringPreservesMapArgument() {
    final CCTask task = new CCTask();
    final org.apache.tools.ant.Project project = new org.apache.tools.ant.Project();
    project.setProperty("nar.os", "Linux");
    task.setProject(project);
    task.setDecorateLinkerOptions(false);
    final AbstractLdLinker linker = GppLinker.getInstance();
    final CommandLineLinkerConfiguration config = configuration(linker, task, true, new String[] {
        "dependency"
    });
    final List<String> args = Arrays.asList(linker.prepareArguments(task, "output", "libprobe.so", new String[] {
        "object.o", "/deps/libdependency.so"
    }, config));
    assertFalse(args.contains("/deps/libdependency.so"));
    assertMapArguments(args, "-Map=libprobe.so.map");
  }

  public void testMapDisabled() {
    for (final AbstractLdLinker linker : new AbstractLdLinker[] {
        GccLinker.getInstance(), GppLinker.getInstance(), LdLinker.getInstance()
    }) {
      for (final boolean decorate : new boolean[] {
          false, true
      }) {
        final List<String> args = prepare(linker, "probe", false, decorate);
        for (final String arg : args) {
          assertFalse(args.toString(), arg.contains("-Map") || arg.equals("-M") || arg.equals("-Wl,-M"));
        }
      }
    }
  }

  public void testMapFlagChangesBuildHistoryIdentity() {
    final GccLinker linker = new GccLinker("gcc", new String[] {
        ".o"
    }, new String[0], "lib", ".so", false, null) {
      @Override
      public String getIdentifier() {
        return "test-gcc";
      }
    };
    final CCTask task = new CCTask();
    final LinkerDef defaults = new LinkerDef();
    final LinkerDef enabled = new LinkerDef();
    enabled.setMap(true);
    final LinkerDef disabled = new LinkerDef();
    disabled.setMap(false);
    final org.apache.tools.ant.Project project = new org.apache.tools.ant.Project();
    project.setProperty("nar.os", "Linux");
    task.setProject(project);
    defaults.setProject(project);
    enabled.setProject(project);
    disabled.setProject(project);
    final String enabledId = linker.createConfiguration(task, new LinkType(), new ProcessorDef[] {
        defaults
    }, (ProcessorDef) enabled, null, null).getIdentifier();
    final String disabledId = linker.createConfiguration(task, new LinkType(), new ProcessorDef[] {
        defaults
    }, (ProcessorDef) disabled, null, null).getIdentifier();
    // A changed map setting must trigger a relink even when source files have not
    // changed.
    assertFalse(enabledId.equals(disabledId));
    project.setProperty("nar.os", "MacOSX");
    final String darwinId = linker.createConfiguration(task, new LinkType(), new ProcessorDef[] {
        defaults
    }, (ProcessorDef) enabled, null, null).getIdentifier();
    assertFalse("Changing map syntax must invalidate link history", enabledId.equals(darwinId));
  }

  public void testOtherVendorAdaptersRetainTheirMapArguments() {
    for (final AbstractLdLinker linker : new AbstractLdLinker[] {
        com.github.maven_nar.cpptasks.sun.ForteCCLinker.getInstance(),
        com.github.maven_nar.cpptasks.ibm.xlC_rLinker.getInstance()
    }) {
      final List<String> args = prepare(linker, "probe", true, false);
      assertTrue(args.toString(), args.contains("-M"));
      assertFalse(args.toString(), args.contains("-Map=probe.map"));
    }
  }

  public void testOutputFilenameIsPassedWithoutShellQuotes() {
    final List<String> args = prepare(GccLinker.getInstance(), "libprobe name.so", true, false);
    assertEquals("libprobe name.so", args.get(args.indexOf("-o") + 1));
  }

  public void testSpacesRemainInOneMapArgument() {
    assertDriverMap(GppLinker.getInstance(), "libcustom name.so");
  }
}
