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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.TargetHistoryTable;
import com.github.maven_nar.cpptasks.TargetInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;

import junit.framework.TestCase;

public class TestLinkerMapLifecycle extends TestCase {
  private static class RecordingLinker extends GccLinker {
    private String mapArgument;
    private int exitCode;
    private boolean writeMap = true;

    RecordingLinker() {
      super("gcc", new String[] {
          ".o"
      }, new String[0], "", "", false, null);
    }

    @Override
    protected int runCommand(final CCTask task, final File workingDir, final String[] command) {
      if (isDryRun()) {
        return super.runCommand(task, workingDir, command);
      }
      final List<String> args = Arrays.asList(command);
      try {
        write(new File(workingDir, args.get(args.indexOf("-o") + 1)), "binary");
        for (final String arg : command) {
          final int index = arg.indexOf("-Map=");
          if (index >= 0) {
            mapArgument = arg.substring(index + 5);
            if (writeMap) {
              write(new File(workingDir, mapArgument), "new map");
            }
          }
        }
        return exitCode;
      } catch (final IOException ex) {
        throw new BuildException(ex);
      }
    }
  }

  private static String read(final File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  private static void write(final File file, final String content) throws IOException {
    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
  }

  private File directory;

  private CCTask task;

  private CommandLineLinkerConfiguration configuration(final RecordingLinker linker, final boolean map) {
    return new CommandLineLinkerConfiguration(linker, "test", new String[][] {
        new String[0], new String[0]
    }, new ProcessorParam[0], false, map, false, new String[0], null);
  }

  @Override
  protected void setUp() throws Exception {
    directory = Files.createTempDirectory("nar-map-test-").toFile();
    task = new CCTask();
    final Project project = new Project();
    project.setProperty("nar.os", "Linux");
    task.setProject(project);
  }

  private TargetInfo target(final CommandLineLinkerConfiguration config, final File output) {
    return new TargetInfo(config, new File[0], new File[0], output, false);
  }

  @Override
  protected void tearDown() throws Exception {
    for (final File file : directory.listFiles()) {
      Files.delete(file.toPath());
    }
    Files.delete(directory.toPath());
  }

  public void testDisabledMapCreatesOnlyBinary() {
    final RecordingLinker linker = new RecordingLinker();
    linker.link(task, new File(directory, "probe"), new String[0], configuration(linker, false));
    assertNull(linker.mapArgument);
    assertEquals(1, directory.list().length);
  }

  public void testDryRunDoesNotCreateFiles() {
    final RecordingLinker linker = new RecordingLinker();
    linker.setDryRun(true);
    linker.setCommands(new ArrayList<String[]>());
    linker.link(task, new File(directory, "probe"), new String[0], configuration(linker, true));
    assertEquals(0, directory.list().length);
  }

  public void testFailedLinkPreservesPreviousMapAndCleansTemporaryFile() throws Exception {
    final RecordingLinker linker = new RecordingLinker();
    linker.exitCode = 1;
    final File output = new File(directory, "probe");
    final File map = new File(directory, "probe.map");
    write(map, "previous map");
    try {
      linker.link(task, output, new String[0], configuration(linker, true));
      fail("A failed link must be reported");
    } catch (final BuildException expected) {
      assertEquals("previous map", read(map));
      assertEquals(2, directory.list().length);
    }
  }

  public void testMissingMapTriggersRelinkWithoutChangingSources() throws Exception {
    final RecordingLinker linker = new RecordingLinker();
    final CommandLineLinkerConfiguration config = configuration(linker, true);
    final TargetHistoryTable history = new TargetHistoryTable(task, directory);
    final File output = new File(directory, "probe");
    final File map = new File(directory, "probe.map");
    write(output, "binary");
    write(map, "map");
    history.update(target(config, output));
    final TargetInfo current = target(config, output);
    history.markForRebuild(current);
    assertFalse("An existing binary and map are up to date", current.getRebuild());
    Files.delete(map.toPath());
    final TargetInfo missing = target(config, output);
    history.markForRebuild(missing);
    assertTrue("Removing only the map must trigger a relink", missing.getRebuild());
    final TargetInfo disabled = target(configuration(linker, false), output);
    history.markForRebuild(disabled);
    assertFalse("No map is required when disabled", disabled.getRebuild());
  }

  public void testPublishesMapWithLiteralSpecialFilename() throws Exception {
    final RecordingLinker linker = new RecordingLinker();
    final File output = new File(directory, "libprobe %,name.so");
    linker.link(task, output, new String[0], configuration(linker, true));
    assertTrue(output.isFile());
    assertEquals("new map", read(new File(directory, output.getName() + ".map")));
    // GNU ld interprets percent characters in the requested map path as
    // substitutions.
    assertFalse(linker.mapArgument.contains("%"));
    assertEquals(2, directory.list().length);
  }

  public void testSuccessfulCommandMustActuallyProduceRequestedMap() throws Exception {
    final RecordingLinker linker = new RecordingLinker();
    linker.writeMap = false;
    try {
      linker.link(task, new File(directory, "probe"), new String[0], configuration(linker, true));
      fail("A missing map must fail the link");
    } catch (final BuildException expected) {
      assertFalse(new File(directory, "probe.map").exists());
      assertEquals(1, directory.list().length);
    }
  }
}
