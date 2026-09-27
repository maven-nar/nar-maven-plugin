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
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.github.maven_nar.NarPreparePackageMojo;
import com.github.maven_nar.NarUtil;
import com.github.maven_nar.ReplayCommandList;
import com.github.maven_nar.Script;
import com.github.maven_nar.Substitution;
import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;

import static org.junit.Assert.*;

/**
 * Real linking and loading catch RUNPATH errors that successful link commands
 * cannot detect.
 */
public class TestNativeLinkerReplay {
  private static Substitution substitution(final String type, final String from, final String to) {
    final Substitution substitution = new Substitution();
    substitution.setType(type);
    substitution.setReplace(from);
    substitution.setReplaceWith(to);
    return substitution;
  }

  private File directory;

  private void assertCommand(final File working, final String... command) throws Exception {
    final int status = run(working, command);
    assertEquals(Arrays.toString(command) + "\n" + readLog(), 0, status);
  }

  private void checkReplay(final String mode, final boolean map) throws Exception {
    final boolean origin = mode.startsWith("origin");
    final String originToken = "originBracedRegex".equals(mode) ? "${ORIGIN}" : "$ORIGIN";
    for (final String compiler : new String[] {
        "gcc", "g++"
    }) {
      for (final boolean dry : new boolean[] {
          false, true
      }) {
        final String id = mode + map + compiler + dry;
        final File root = new File(directory, "original " + id);
        final File output = new File(root, "nested/output");
        final File plugins = new File(root, "lib");
        final File relocated = new File(directory, "relocated " + id);
        assertTrue(output.mkdirs());
        assertTrue(plugins.mkdirs());
        for (final String source : new String[] {
            "provider.c", "consumer.c"
        }) {
          try (InputStream input = getClass().getResourceAsStream("/replay-runpath/" + source)) {
            Files.copy(input, new File(root, source).toPath());
          }
        }
        assertCommand(root, compiler, "-shared", "-fPIC", "provider.c", "-o",
            new File(plugins, "libreplayprobe.so").toString());
        assertCommand(root, compiler, "-c", "consumer.c", "-o", "main.o");
        final GccLinker linker = new GccLinker(compiler, new String[] {
            ".o"
        }, new String[0], "", "", false, null);
        final ReplayCommandList history = new ReplayCommandList();
        linker.setCommands(history);
        linker.setDryRun(dry);
        final CCTask task = new CCTask();
        final Project project = new Project();
        project.setProperty("nar.os", "Linux");
        task.setProject(project);
        task.setDecorateLinkerOptions(false);
        final String[] preargs = {
            "-g", "-Xlinker", "-rpath", "-Xlinker", plugins.toString()
        };
        // Keep the library search argument distinct so the string rule below changes
        // only the runtime path. NAR does not infer option semantics.
        final CommandLineLinkerConfiguration config = new CommandLineLinkerConfiguration(linker, "native-replay",
            new String[][] {
                preargs, {
                    "-L" + new File(root, "./lib"), "-lreplayprobe"
                }
            }, new ProcessorParam[0], false, map, false, new String[0], null);
        final File originalBinary = new File(output, "probe");
        linker.link(task, originalBinary, new String[] {
            new File(root, "main.o").toString()
        }, config);
        assertEquals(!dry, originalBinary.isFile());
        if (!dry)
          assertCommand(directory, originalBinary.toString());
        final File commands = new File(directory, "link-commands");
        NarUtil.writeCommandFile(commands, history);
        final List<Substitution> rules = new ArrayList<>();
        if (origin) {
          final boolean regex = "originBracedRegex".equals(mode);
          final String replacement = originToken + "/../../lib";
          rules.add(substitution(regex ? "regex" : "string",
              regex ? "^" + java.util.regex.Pattern.quote(plugins.toString()) + "$" : plugins.toString(),
              regex ? java.util.regex.Matcher.quoteReplacement(replacement) : replacement));
        }
        rules.add(substitution("absolutePath", root.toString(), relocated + File.separator));
        rules.add(substitution("regex", "^-g$", ""));
        Files.move(root.toPath(), relocated.toPath());
        final File binary = new File(relocated, "nested/output/probe");
        final File finalMap = new File(binary.getParentFile(), "probe.map");
        for (final String shell : new String[] {
            "sh", "bash"
        }) {
          Files.deleteIfExists(binary.toPath());
          Files.deleteIfExists(finalMap.toPath());
          final Script script = new Script();
          script.setScriptType(shell);
          script.setSubstitutions(rules);
          final File replay = new File(directory, "replay." + shell);
          try (PrintWriter writer = new PrintWriter(replay, "UTF-8")) {
            new NarPreparePackageMojo().processReplayFile(Files.readAllLines(commands.toPath(), StandardCharsets.UTF_8),
                script, writer);
          }
          assertFalse(new String(Files.readAllBytes(replay.toPath()), StandardCharsets.UTF_8).contains("'-g'"));
          // Both replay and execution run outside the project. The old absolute root
          // is gone, so retaining it cannot accidentally pass.
          assertCommand(directory, shell, replay.toString());
          assertCommand(directory, "readelf", "-d", binary.toString());
          final String expected = origin ? originToken + "/../../lib" : new File(relocated, "lib").toString();
          assertTrue(readLog(), readLog().contains("[" + expected + "]"));
          assertCommand(directory, binary.toString());
          assertEquals(map, finalMap.isFile());
          if (map)
            assertTrue(finalMap.length() > 0);
          for (final String name : binary.getParentFile().list())
            assertFalse(name, name.startsWith("nar-map-") && name.endsWith(".tmp"));
          if (origin) {
            final File moved = new File(directory, "finished " + id);
            Files.move(relocated.toPath(), moved.toPath());
            assertCommand(directory, new File(moved, "nested/output/probe").toString());
            Files.move(moved.toPath(), relocated.toPath());
          }
        }
      }
    }
  }

  private void delete(final File file) throws Exception {
    if (file.isDirectory())
      for (final File child : file.listFiles())
        delete(child);
    Files.deleteIfExists(file.toPath());
  }

  private String readLog() throws Exception {
    return new String(Files.readAllBytes(new File(directory, "run.log").toPath()), StandardCharsets.UTF_8);
  }

  private int run(final File working, final String... command) throws Exception {
    final ProcessBuilder process = new ProcessBuilder(command).directory(working).redirectErrorStream(true)
        .redirectOutput(new File(directory, "run.log"));
    process.environment().remove("LD_LIBRARY_PATH");
    process.environment().remove("LD_PRELOAD");
    process.environment().put("LC_ALL", "C");
    return process.start().waitFor();
  }

  @Before
  public void setUp() throws Exception {
    Assume.assumeTrue("ELF RUNPATH test", System.getProperty("os.name").equals("Linux"));
    directory = Files.createTempDirectory("nar-native-replay-").toFile();
    for (final String command : new String[] {
        "gcc", "g++", "readelf", "sh", "bash"
    }) {
      try {
        final int status = "sh".equals(command) || "bash".equals(command) ? run(directory, command, "-c", "exit 0")
            : run(directory, command, "--version");
        Assume.assumeTrue(command + " unavailable", status == 0);
      } catch (final java.io.IOException unavailable) {
        Assume.assumeNoException(unavailable);
      }
    }
  }

  @After
  public void tearDown() throws Exception {
    if (directory != null)
      delete(directory);
  }

  @Test
  public void testExplicitAbsoluteRootRelocation() throws Exception {
    for (final boolean map : new boolean[] {
        false, true
    })
      checkReplay("absolute", map);
  }

  @Test
  public void testLiteralOriginRuntimePathsSurviveRelocation() throws Exception {
    for (final String mode : new String[] {
        "originString", "originBracedRegex"
    }) {
      for (final boolean map : new boolean[] {
          false, true
      })
        checkReplay(mode, map);
    }
  }
}
