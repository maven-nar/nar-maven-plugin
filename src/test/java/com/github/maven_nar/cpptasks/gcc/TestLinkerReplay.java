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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.github.maven_nar.NarCompileMojo;
import com.github.maven_nar.NarPreparePackageMojo;
import com.github.maven_nar.NarUtil;
import com.github.maven_nar.Script;
import com.github.maven_nar.Substitution;
import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;

import static org.junit.Assert.*;

/**
 * Execute the generated scripts, including the persisted command-log round
 * trip.
 */
public class TestLinkerReplay {
  public static class PathProbe {
    public static void main(final String[] args) throws Exception {
      final List<String> values = Arrays.asList(args);
      for (int i = 0; i < args.length; i++) {
        final String value = args[i];
        if (value.startsWith("-L") || value.startsWith("-F")) {
          final String directory = value.length() == 2 ? args[++i] : value.substring(2);
          if (!new File(directory).isDirectory())
            throw new IllegalArgumentException("Missing search directory: " + directory);
        } else if (value.startsWith("--input=")) {
          if (!new File(value.substring(8)).isFile())
            throw new IllegalArgumentException("Missing explicit path: " + value);
        }
      }
      if (!new File(args[args.length - 2]).isFile())
        throw new IllegalArgumentException("Missing input: " + args[args.length - 2]);
      if (!new File(args[args.length - 1]).isFile())
        throw new IllegalArgumentException("Missing relative input");
      Files.write(new File(args[values.indexOf("-o") + 1]).toPath(), "linked".getBytes(StandardCharsets.UTF_8));
      for (final String arg : args) {
        if (arg.startsWith("-Map="))
          Files.write(new File(arg.substring(5)).toPath(), "map".getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  public static class Probe {
    public static void main(final String[] args) throws Exception {
      final int index = Arrays.asList(args).indexOf("-o");
      if (!"object name.o".equals(args[args.length - 1]))
        throw new IllegalArgumentException("Quoted input argument");
      Files.write(new File(args[index + 1]).toPath(), args[0].getBytes(StandardCharsets.UTF_8));
      if (!"MISSING".equals(args[0])) {
        for (final String arg : args) {
          if (arg.startsWith("-Map="))
            Files.write(new File(arg.substring(5)).toPath(), args[0].getBytes(StandardCharsets.UTF_8));
        }
      }
      if ("FAIL".equals(args[0]))
        System.exit(7);
    }
  }

  private static class RecordingMojo extends NarCompileMojo {
    List<String[]> history() {
      return linkCommands;
    }
  }

  private static String encodedArguments(final List<String> values) throws Exception {
    return ReplayArgumentProbe.encodedArguments(values);
  }

  private static String read(final File file) throws Exception {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  private static Substitution substitution(final String type, final String match, final String replacement) {
    final Substitution sub = new Substitution();
    sub.setType(type);
    sub.setReplace(match);
    sub.setReplaceWith(replacement);
    return sub;
  }

  private File directory;

  private File argumentScript(final List<String[]> history, final String shell, final File working,
      final List<Substitution> rules)
      throws Exception {
    // Exercise the payload rules without also rewriting the JVM or the test's
    // working directory.
    final String[] paths = {
        stageArgumentProbe().getAbsolutePath(), history.get(0)[0], working.getAbsolutePath()
    };
    final List<Substitution> substitutions = new ArrayList<>();
    for (int i = 0; i < paths.length; i++) {
      substitutions.add(substitution("string", paths[i], "REPLAY_LAUNCH_PATH_" + i));
    }
    substitutions.addAll(rules);
    for (int i = paths.length - 1; i >= 0; i--) {
      substitutions.add(substitution("string", "REPLAY_LAUNCH_PATH_" + i, paths[i]));
    }
    return scriptWithSubstitutions(history, shell, substitutions);
  }

  private void assertNoTemporaryMaps(final File directory) {
    for (final String name : directory.list()) {
      assertFalse("Temporary map remains: " + name, name.startsWith("nar-map-") && name.endsWith(".tmp"));
    }
  }

  private void assertReplaySucceeds(final File replay, final String shell, final File invocation) throws Exception {
    final String[] command = "bat".equals(shell) ? new String[] {
        "cmd.exe", "/d", "/c", replay.getAbsolutePath()
    } : new String[] {
        "/bin/" + shell, replay.getAbsolutePath()
    };
    final File log = new File(directory, "run.log");
    final int status = new ProcessBuilder(command).directory(invocation).redirectErrorStream(true).redirectOutput(log)
        .start().waitFor();
    assertEquals(read(log), 0, status);
  }

  private void checkExplicitRelativePaths(final String shell) throws Exception {
    for (final boolean map : new boolean[] {
        false, true
    }) {
      for (final boolean dry : new boolean[] {
          false, true
      }) {
        final File root = new File(directory, "relative " + shell + map + dry);
        final File working = new File(root, "target/bin");
        final File objects = new File(root, "target/object dir %!&");
        assertTrue(working.mkdirs());
        assertTrue(objects.mkdirs());
        final File input = new File(objects, "input.o");
        Files.write(input.toPath(), "object".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(working, "local.o").toPath(), "local".getBytes(StandardCharsets.UTF_8));
        // Include both replacement targets in the launcher path so broad rules fail
        // regardless of the checkout directory. Keep batch commands short, too.
        final List<String> pre = Arrays.asList("-cp",
            stageProbe(PathProbe.class, "nar-map-result-launcher").getAbsolutePath(), PathProbe.class.getName(),
            "-L" + objects, "-F", objects.toString(), "--input=" + input);
        final List<String[]> history = new RecordingMojo().history();
        recordProbe(working, "result", pre, new String[] {
            input.toString(), "local.o"
        }, dry, map, history);
        Files.deleteIfExists(new File(working, "result").toPath());
        Files.deleteIfExists(new File(working, "result.map").toPath());
        // This relative replacement is supplied explicitly, from the recorded cwd.
        // Applying the root rule afterwards also makes the working directory relative
        // to the script's invocation directory.
        final File replay = scriptWithSubstitutions(history, shell,
            Arrays.asList(substitution("string", objects.toString(), "../object dir %!&"),
                substitution("absolutePath", root.toString(), ""),
                substitution("regex", "^result(\\.map)?$", "renamed %!&$1"),
                // Match map arguments and metadata without rewriting launcher paths.
                substitution("regex", "^(-Map=)?nar-map-", "$1rewritten-map-")));
        if (map)
          assertTrue(read(replay).contains("-Map=rewritten-map-"));
        assertReplaySucceeds(replay, shell, root);
        assertEquals("linked", read(new File(working, "renamed %!&")));
        assertEquals(map, new File(working, "renamed %!&.map").isFile());
        if (map)
          assertEquals("map", read(new File(working, "renamed %!&.map")));
        assertFalse(new File(root, "renamed %!&").exists());
        assertFalse(new File(working, "result").exists());
        for (final String name : working.list())
          assertFalse("Temporary map remains: " + name, name.endsWith(".tmp"));
      }
    }
  }

  private void checkLiteralSubstitutions(final String shell) throws Exception {
    for (final String type : new String[] {
        "string", "regex", "absolutePath", "relativePath"
    }) {
      for (final boolean map : new boolean[] {
          false, true
      }) {
        for (final boolean dry : new boolean[] {
            false, true
        }) {
          final File working = new File(directory, "literal " + shell + type + map + dry + "/nested/output");
          assertTrue(working.mkdirs());
          final String root = new File(directory, "source").toString();
          final String prefix = root + File.separator;
          final List<String> payload = Arrays.asList(prefix + "main.o", "-L" + prefix + "lib", "-F",
              prefix + "frameworks", "-Xlinker", "--rpath=" + prefix + "missing,cache:" + prefix + "lib,cache",
              prefix + "zero", prefix + "capture", "--input=" + prefix + "input.o", "ORIGIN_VALUE", "SHELL_VALUE");
          final List<String[]> history = recordArguments(working, payload, dry, map);
          assertEquals(!dry, new File(working, "result").isFile());
          Files.deleteIfExists(new File(working, "result").toPath());
          Files.deleteIfExists(new File(working, "result.map").toPath());
          final String match = "regex".equals(type) ? java.util.regex.Pattern.quote(prefix)
              : type.endsWith("Path") ? root : prefix;
          final String special = "two words 'single' \"double\" %PATH% !bang! & | < > ^ ; $HOME \\\\";
          final File replay = argumentScript(history, shell, working,
              Arrays.asList(substitution(type, match, "relative/"),
                  substitution("regex", "^(?=relative/zero$)", "/usr/lib:"),
                  substitution("regex", "^relative/(?<first>cap)(ture)$", "$2/${first}/$2"),
                  substitution("string", "ORIGIN_VALUE", "$ORIGIN/../lib:${ORIGIN}/plugins"),
                  substitution("string", "SHELL_VALUE", special)));
          assertReplaySucceeds(replay, shell, directory);
          assertEquals(
              encodedArguments(Arrays.asList("relative/main.o", "-Lrelative/lib", "-F", "relative/frameworks",
                  "-Xlinker", "--rpath=relative/missing,cache:relative/lib,cache", "/usr/lib:relative/zero",
                  "ture/cap/ture", "--input=relative/input.o", "$ORIGIN/../lib:${ORIGIN}/plugins", special)),
              read(new File(working, "result")));
          assertEquals(map, new File(working, "result.map").isFile());
          if (map)
            assertEquals("map", read(new File(working, "result.map")));
          assertNoTemporaryMaps(working);
          assertFalse(read(replay).contains("_nar_replay_base"));
        }
      }
    }
  }

  private void checkRelocatedPaths(final String shell, final String type, final boolean map, final boolean dry)
      throws Exception {
    final String id = shell + type + map + dry;
    final File original = new File(directory, "original " + id);
    final File working = new File(original, "target/bin");
    final File objects = new File(original,
        "target/object dir %PATH%! & " + ("bat".equals(shell) ? "' ^" : map ? "' $" : "\" $"));
    assertTrue(working.mkdirs());
    assertTrue(objects.mkdirs());
    final File input = new File(objects, "input.o");
    Files.write(input.toPath(), "object".getBytes(StandardCharsets.UTF_8));
    Files.write(new File(working, "local.o").toPath(), "local".getBytes(StandardCharsets.UTF_8));
    final List<String> pre = new ArrayList<>(Arrays.asList("-cp", System.getProperty("java.class.path"),
        PathProbe.class.getName(), "-L" + objects, "-L", objects.toString(), "-F" + objects, "-F", objects.toString()));
    if (type.endsWith("Path"))
      pre.add("--input=" + input);
    final List<String[]> history = new RecordingMojo().history();
    final String name = "output %!&.bin";
    recordProbe(working, name, pre, new String[] {
        input.toString(), "local.o"
    }, dry, map, history);
    assertEquals(!dry, new File(working, name).isFile());
    Files.deleteIfExists(new File(working, name).toPath());
    Files.deleteIfExists(new File(working, name + ".map").toPath());
    assertNoTemporaryMaps(working);
    final Substitution sub = new Substitution();
    sub.setType(type);
    sub.setReplace(type.endsWith("Path") ? original.toString() : original + File.separator);
    if ("regex".equals(type))
      sub.setReplace(java.util.regex.Pattern.quote(sub.getReplace()));
    final String special = "bat".equals(shell) ? " %PATH%! & ^" : " %! & ' \" $";
    final File relocated = new File(directory, "relocated " + id + special);
    final String replacement = relocated + File.separator;
    sub.setReplaceWith("regex".equals(type) ? java.util.regex.Matcher.quoteReplacement(replacement) : replacement);
    final File replay = scriptWithSubstitutions(history, shell,
        Arrays.asList(sub, substitution("regex", "^$", "restored argument")));
    // The original tree must be unavailable: otherwise an accidentally retained
    // absolute path could pass.
    Files.move(original.toPath(), relocated.toPath());
    assertReplaySucceeds(replay, shell, directory);
    final File result = new File(relocated, "target/bin/" + name);
    assertEquals("linked", read(result));
    assertEquals(map, new File(result.getParentFile(), name + ".map").isFile());
    if (map)
      assertEquals("map", read(new File(result.getParentFile(), name + ".map")));
    assertFalse("Output belongs in the recorded directory", new File(relocated, name).exists());
    assertNoTemporaryMaps(result.getParentFile());
  }

  private void checkRemovedArguments(final String shell) throws Exception {
    for (final String type : new String[] {
        "string", "regex"
    }) {
      for (final boolean map : new boolean[] {
          false, true
      }) {
        for (final boolean dry : new boolean[] {
            false, true
        }) {
          final File working = new File(directory, shell + type + map + dry + "/nested/output");
          assertTrue(working.mkdirs());
          final List<String> payload = Arrays.asList("-g", "", " \t ", "REPLAY_VALUE", "-remove", "-g");
          final List<String[]> history = recordArguments(working, payload, dry, map);
          assertEquals(!dry, new File(working, "result").isFile());
          Files.deleteIfExists(new File(working, "result").toPath());
          Files.deleteIfExists(new File(working, "result.map").toPath());
          final String replacement = "two words 'quoted' \"&quoted\" %PATH% !bang! $HOME ; \\\\";
          final Substitution value = substitution("string", "REPLAY_VALUE", replacement);
          final File replay = argumentScript(history, shell, working,
              Arrays.asList(substitution(type, "regex".equals(type) ? "^-g$" : "-g", ""),
                  substitution(type, "regex".equals(type) ? "^-remove$" : "-remove", ""), value));
          assertReplaySucceeds(replay, shell, directory);
          // Exact argv comparison catches unwanted empty tokens, trimming, splitting and
          // shell expansion.
          assertEquals(encodedArguments(Arrays.asList("", " \t ", replacement)), read(new File(working, "result")));
          assertEquals(map, new File(working, "result.map").isFile());
          if (map)
            assertEquals("map", read(new File(working, "result.map")));
          assertNoTemporaryMaps(working);
        }
      }
    }
    // A value temporarily emptied and then restored must survive; only the final
    // value decides removal.
    final List<String[]> history = recordArguments(directory, Arrays.asList("-g", ""), true, false);
    final File replay = argumentScript(history, shell, directory,
        Arrays.asList(substitution("string", "-g", ""), substitution("regex", "^$", "restored value")));
    assertReplaySucceeds(replay, shell, directory);
    assertEquals(encodedArguments(Arrays.asList("restored value", "restored value")),
        read(new File(directory, "result")));
  }

  private void checkReplayWorkingDirectory(final String shell, final String name, final boolean relative,
      final boolean missing)
      throws Exception {
    for (final boolean map : new boolean[] {
        false, true
    }) {
      for (final boolean dry : new boolean[] {
          false, true
      }) {
        final File root = new File(directory, "invocation " + shell + relative + missing + map + dry);
        final File working = new File(root, name);
        final File cdpath = new File(directory, "CDPATH decoy " + shell + relative + missing + map + dry);
        final File decoy = new File(cdpath, name);
        assertTrue(working.mkdirs());
        assertTrue(decoy.mkdirs());
        final List<String[]> history = recordArguments(working, Arrays.asList("directory probe"), dry, map);
        assertEquals(!dry, new File(working, "result").isFile());
        Files.deleteIfExists(new File(working, "result").toPath());
        Files.deleteIfExists(new File(working, "result.map").toPath());
        if (missing)
          delete(working);
        final List<Substitution> rules = new ArrayList<>();
        if (relative)
          rules.add(substitution("absolutePath", root.toString(), ""));
        final File replay = scriptWithSubstitutions(history, shell, rules);
        assertFalse(read(replay).contains("_nar_replay_base="));
        final File decoyMap = new File(decoy, "result.map");
        Files.write(decoyMap.toPath(), "decoy map".getBytes(StandardCharsets.UTF_8));
        String temporary = null;
        for (final String argument : history.get(0)) {
          if (argument.startsWith("-Map=")) {
            temporary = argument.substring(5);
            Files.write(new File(decoy, temporary).toPath(), "decoy temporary".getBytes(StandardCharsets.UTF_8));
            if (!missing)
              Files.write(new File(working, temporary).toPath(), "stale map".getBytes(StandardCharsets.UTF_8));
          }
        }
        final File log = new File(directory, "run.log");
        final String[] command = "bat".equals(shell) ? new String[] {
            "cmd.exe", "/d", "/c", replay.getAbsolutePath()
        } : new String[] {
            "/bin/" + shell, replay.getAbsolutePath()
        };
        final ProcessBuilder builder = new ProcessBuilder(command).directory(root).redirectErrorStream(true)
            .redirectOutput(log);
        builder.environment().put("CDPATH", cdpath.toString());
        builder.environment().put("OLDPWD", decoy.toString());
        final int status = builder.start().waitFor();
        if (missing) {
          assertTrue("Missing directory must fail before linking: " + read(log), status != 0);
          assertFalse(working.exists());
        } else {
          assertEquals(read(log), 0, status);
          assertTrue("Replay wrote outside the recorded directory: " + read(log), new File(working, "result").isFile());
          assertEquals(encodedArguments(Arrays.asList("directory probe")), read(new File(working, "result")));
          assertEquals(map, new File(working, "result.map").isFile());
          if (map)
            assertEquals("map", read(new File(working, "result.map")));
          assertNoTemporaryMaps(working);
        }
        assertFalse(new File(root, "result").exists());
        assertFalse(new File(decoy, "result").exists());
        assertEquals("decoy map", read(decoyMap));
        if (temporary != null)
          assertEquals("decoy temporary", read(new File(decoy, temporary)));
        delete(root);
        delete(cdpath);
      }
    }
  }

  private void checkRewrittenExecutable(final String shell) throws Exception {
    final boolean batch = "bat".equals(shell);
    final File original = new File(directory, "executable original " + shell);
    final File working = new File(original, "nested/output");
    assertTrue(working.mkdirs());
    final File executable = new File(original, batch ? "tool.exe" : "tool");
    Files.copy((batch ? new File(System.getenv("ComSpec")) : new File("/bin/true")).toPath(), executable.toPath());
    if (!batch)
      assertTrue(executable.setExecutable(true));
    final String[] args = batch ? new String[] {
        executable.toString(), "/d", "/c", "exit", "0"
    } : new String[] {
        executable.toString()
    };
    final StringBuilder v1 = new StringBuilder("# nar-replay-v1\t")
        .append(URLEncoder.encode(working.toString(), "UTF-8"));
    for (final String arg : args)
      v1.append('\t').append(URLEncoder.encode(arg, "UTF-8"));
    final Substitution sub = new Substitution();
    sub.setType("absolutePath");
    sub.setReplace(original.toString());
    final File relocated = new File(directory, "executable relocated " + shell + " %PATH%! &");
    sub.setReplaceWith(relocated + File.separator);
    final Script config = new Script();
    config.setScriptType(shell);
    config.setSubstitutions(Arrays.asList(sub));
    final File replay = new File(directory, "executable." + config.getExtension());
    try (PrintWriter writer = new PrintWriter(replay, "UTF-8")) {
      new NarPreparePackageMojo().processReplayFile(Arrays.asList(v1.toString(), "readable command"), config, writer);
    }
    Files.move(original.toPath(), relocated.toPath());
    assertReplaySucceeds(replay, shell, directory);
  }

  private void delete(final File file) throws Exception {
    if (file.isDirectory()) {
      for (final File child : file.listFiles())
        delete(child);
    }
    Files.deleteIfExists(file.toPath());
  }

  private int execute(final File script, final String shell) throws Exception {
    return new ProcessBuilder("/bin/" + shell, script.getAbsolutePath()).directory(directory).redirectErrorStream(true)
        .redirectOutput(new File(directory, "run.log")).start().waitFor();
  }

  private List<String[]> record(final File working, final String output, final String value, final boolean dry)
      throws Exception {
    return record(working, output, value, dry, false, new RecordingMojo().history());
  }

  private List<String[]> record(final File working, final String output, final String value, final boolean dry,
      final boolean map, final List<String[]> history)
      throws Exception {
    final GccLinker linker = new GccLinker(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(),
        new String[] {
            ".o"
        }, new String[0], "", "", false, null);
    linker.setCommands(history);
    linker.setDryRun(dry);
    final CCTask task = new CCTask();
    final Project project = new Project();
    project.setProperty("nar.os", "Linux");
    task.setProject(project);
    task.setDecorateLinkerOptions(false);
    final String[] pre = {
        "-cp", System.getProperty("java.class.path"), Probe.class.getName(), value
    };
    final CommandLineLinkerConfiguration config = new CommandLineLinkerConfiguration(linker, "replay-test",
        new String[][] {
            pre, new String[0]
        }, new ProcessorParam[0], false, map, false, new String[0], null);
    linker.link(task, new File(working, output), new String[] {
        "object name.o"
    }, config);
    return history;
  }

  private List<String[]> recordArguments(final File working, final List<String> payload, final boolean dry,
      final boolean map)
      throws Exception {
    final List<String> pre = new ArrayList<>(
        Arrays.asList("-cp", stageArgumentProbe().getAbsolutePath(), ReplayArgumentProbe.class.getName()));
    pre.addAll(payload);
    pre.add("END_ARGUMENTS");
    final List<String[]> history = new RecordingMojo().history();
    recordProbe(working, "result", pre, new String[0], dry, map, history);
    return history;
  }

  private void recordProbe(final File working, final String output, final List<String> pre, final String[] inputs,
      final boolean dry, final boolean map, final List<String[]> history)
      throws Exception {
    final GccLinker linker = new GccLinker(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(),
        new String[] {
            ".o"
        }, new String[0], "", "", false, null);
    linker.setCommands(history);
    linker.setDryRun(dry);
    final CCTask task = new CCTask();
    final Project project = new Project();
    project.setProperty("nar.os", "Linux");
    task.setProject(project);
    task.setDecorateLinkerOptions(false);
    final CommandLineLinkerConfiguration config = new CommandLineLinkerConfiguration(linker, "replay-test",
        new String[][] {
            pre.toArray(new String[pre.size()]), new String[0]
        }, new ProcessorParam[0], false, map, false, new String[0], null);
    linker.link(task, new File(working, output), inputs, config);
  }

  private File script(final List<String[]> commands, final String shell, final Substitution sub) throws Exception {
    return scriptWithSubstitutions(commands, shell, sub == null ? null : Arrays.asList(sub));
  }

  private File scriptWithSubstitutions(final List<String[]> commands, final String shell,
      final List<Substitution> substitutions)
      throws Exception {
    final File log = new File(directory, "link-commands");
    NarUtil.writeCommandFile(log, commands);
    final Script script = new Script();
    script.setScriptType(shell);
    script.setSubstitutions(substitutions);
    final File output = new File(directory, "replay." + script.getExtension());
    try (PrintWriter writer = new PrintWriter(output, "UTF-8")) {
      new NarPreparePackageMojo().processReplayFile(Files.readAllLines(log.toPath(), StandardCharsets.UTF_8), script,
          writer);
    }
    if ("bat".equals(shell)) {
      for (final String line : Files.readAllLines(output.toPath(), StandardCharsets.UTF_8))
        assertTrue("Batch test line exceeds cmd.exe's limit: " + line.length(), line.length() < 8191);
    }
    return output;
  }

  @Before
  public void setUp() throws Exception {
    directory = Files.createTempDirectory("nar-replay-test-").toFile();
  }

  private File stageArgumentProbe() throws Exception {
    // String substitutions must not accidentally rewrite the JVM's own launch
    // paths.
    return stageProbe(ReplayArgumentProbe.class, "launcher-g-remove");
  }

  private File stageProbe(final Class<?> probe, final String name) throws Exception {
    final File classes = new File(directory, name + "/classes");
    final String resource = probe.getName().replace('.', '/') + ".class";
    final File target = new File(classes, resource);
    if (!target.isFile()) {
      assertTrue(target.getParentFile().mkdirs());
      try (InputStream input = probe.getResourceAsStream("/" + resource)) {
        Files.copy(input, target.toPath());
      }
    }
    return classes;
  }

  @After
  public void tearDown() throws Exception {
    delete(directory);
  }

  @Test
  public void testBatchReplayAppliesLiteralSubstitutions() throws Exception {
    Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    checkLiteralSubstitutions("bat");
  }

  @Test
  public void testBatchReplayPreservesLiteralArguments() throws Exception {
    Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    final File working = new File(directory, "batch work %!&");
    assertTrue(working.mkdir());
    final String value = "value \"&quoted\" %PATH% !bang! \\";
    final List<String[]> commands = record(working, "result %!&.txt", value, true);
    final File scriptFile = script(commands, "bat", null);
    final Process process = new ProcessBuilder("cmd.exe", "/d", "/c", scriptFile.getAbsolutePath()).directory(directory)
        .redirectErrorStream(true).redirectOutput(new File(directory, "run.log")).start();
    final int status = process.waitFor();
    assertEquals(read(new File(directory, "run.log")), 0, status);
    assertEquals(value, read(new File(working, "result %!&.txt")));
  }

  @Test
  public void testBatchReplayPublishesMapAndPreservesFailureStatus() throws Exception {
    Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    final File map = new File(directory, "probe %!&.map");
    for (final String value : new String[] {
        "contents", "FAIL", "MISSING"
    }) {
      Files.write(map.toPath(), "previous map".getBytes(StandardCharsets.UTF_8));
      final List<String[]> commands = record(directory, "probe %!&", value, true, true, new RecordingMojo().history());
      final File scriptFile = script(commands, "bat", null);
      final Process process = new ProcessBuilder("cmd.exe", "/d", "/c", scriptFile.getAbsolutePath())
          .directory(directory).redirectErrorStream(true).redirectOutput(new File(directory, "run.log")).start();
      final int status = process.waitFor();
      assertEquals(read(new File(directory, "run.log")), "contents".equals(value) ? 0 : "FAIL".equals(value) ? 7 : 1,
          status);
      assertEquals("contents".equals(value) ? value : "previous map", read(map));
      assertNoTemporaryMaps(directory);
    }
  }

  @Test
  public void testBatchReplayRemovesOnlyExplicitlyEmptiedArguments() throws Exception {
    Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    checkRemovedArguments("bat");
  }

  @Test
  public void testBatchReplayResolvesRewrittenExecutableAndV1Record() throws Exception {
    Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    checkRewrittenExecutable("bat");
  }

  @Test
  public void testBatchReplayUsesExplicitAbsoluteRootReplacements() throws Exception {
    Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    for (final String type : new String[] {
        "absolutePath", "relativePath", "string", "regex"
    }) {
      for (final boolean map : new boolean[] {
          false, true
      }) {
        for (final boolean dry : new boolean[] {
            false, true
        }) {
          checkRelocatedPaths("bat", type, map, dry);
        }
      }
    }
  }

  @Test
  public void testBatchReplayUsesExplicitRelativePathsAndMapNames() throws Exception {
    Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    checkExplicitRelativePaths("bat");
  }

  @Test
  public void testBatchReplayUsesRecordedWorkingDirectory() throws Exception {
    Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
    checkReplayWorkingDirectory("bat", "nested/output", true, false);
    checkReplayWorkingDirectory("bat", "nested/output", false, false);
    checkReplayWorkingDirectory("bat", "nested/output", true, true);
  }

  @Test
  public void testLegacyQuotedLogStillWorks() throws Exception {
    Assume.assumeTrue(new File("/bin/sh").isFile());
    final File scriptFile = new File(directory, "legacy.sh");
    try (PrintWriter writer = new PrintWriter(scriptFile, "UTF-8")) {
      new NarPreparePackageMojo().processReplayFile(Arrays.asList("printf '%s' 'legacy value' > 'legacy output'"),
          new Script(), writer);
    }
    final Process process = new ProcessBuilder("/bin/sh", scriptFile.getAbsolutePath()).directory(directory).start();
    assertEquals(0, process.waitFor());
    assertEquals("legacy value", read(new File(directory, "legacy output")));
  }

  @Test
  public void testPosixReplayAppliesLiteralSubstitutions() throws Exception {
    Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
    for (final String shell : new String[] {
        "sh", "bash"
    })
      checkLiteralSubstitutions(shell);
  }

  @Test
  public void testPosixReplayIgnoresCdpathForWorkingDirectory() throws Exception {
    Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
    for (final String shell : new String[] {
        "sh", "bash"
    }) {
      checkReplayWorkingDirectory(shell, "nested/output", true, false);
      checkReplayWorkingDirectory(shell, "nested/output", false, false);
    }
  }

  @Test
  public void testPosixReplayPreservesLiteralArgumentsAndWorkingDirectory() throws Exception {
    for (final String shell : new String[] {
        "sh", "bash"
    }) {
      Assume.assumeTrue(new File("/bin/" + shell).isFile());
      final File working = new File(directory, shell + " work %,'$");
      assertTrue(working.mkdir());
      final String name = "output name %,'$&.so";
      final List<String[]> commands = record(working, name, "first value '$HOME' \"quoted\" & ;", true);
      final File scriptFile = script(commands, shell, null);
      final Process process = new ProcessBuilder("/bin/" + shell, scriptFile.getAbsolutePath()).directory(directory)
          .redirectErrorStream(true).redirectOutput(new File(directory, "run.log")).start();
      final int status = process.waitFor();
      assertEquals(read(new File(directory, "run.log")), 0, status);
      assertEquals("first value '$HOME' \"quoted\" & ;", read(new File(working, name)));
      assertFalse(new File(directory, "output").exists());
    }
  }

  @Test
  public void testPosixReplayRejectsMissingWorkingDirectoryWithCdpath() throws Exception {
    Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
    for (final String shell : new String[] {
        "sh", "bash"
    })
      checkReplayWorkingDirectory(shell, "nested/output", true, true);
  }

  @Test
  public void testPosixReplayRemovesOnlyExplicitlyEmptiedArguments() throws Exception {
    Assume.assumeTrue(new File("/bin/sh").isFile());
    for (final String shell : new String[] {
        "sh", "bash"
    })
      checkRemovedArguments(shell);
  }

  @Test
  public void testPosixReplayResolvesRewrittenExecutableAndV1Record() throws Exception {
    Assume.assumeTrue(new File("/bin/true").isFile());
    for (final String shell : new String[] {
        "sh", "bash"
    })
      checkRewrittenExecutable(shell);
  }

  @Test
  public void testPosixReplayTreatsWorkingDirectoryAsLiteralPath() throws Exception {
    Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
    for (final String shell : new String[] {
        "sh", "bash"
    }) {
      for (final String name : new String[] {
          "-output", "-", "output %!& ' \" $"
      })
        checkReplayWorkingDirectory(shell, name, true, false);
    }
  }

  @Test
  public void testPosixReplayUsesExplicitAbsoluteRootReplacements() throws Exception {
    Assume.assumeTrue(new File("/bin/sh").isFile());
    for (final String shell : new String[] {
        "sh", "bash"
    }) {
      for (final String type : new String[] {
          "absolutePath", "relativePath", "string", "regex"
      }) {
        for (final boolean map : new boolean[] {
            false, true
        }) {
          for (final boolean dry : new boolean[] {
              false, true
          }) {
            checkRelocatedPaths(shell, type, map, dry);
          }
        }
      }
    }
  }

  @Test
  public void testPosixReplayUsesExplicitRelativePathsAndMapNames() throws Exception {
    Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
    for (final String shell : new String[] {
        "sh", "bash"
    })
      checkExplicitRelativePaths(shell);
  }

  @Test
  public void testReplayFailurePreservesOldMapAndRemovesTemporaryMap() throws Exception {
    Assume.assumeTrue(new File("/bin/sh").isFile());
    final File map = new File(directory, "probe.map");
    Files.write(map.toPath(), "previous map".getBytes(StandardCharsets.UTF_8));
    final List<String[]> commands = record(directory, "probe", "FAIL", true, true, new RecordingMojo().history());
    assertEquals(7, execute(script(commands, "sh", null), "sh"));
    assertEquals("previous map", read(map));
    assertNoTemporaryMaps(directory);
  }

  @Test
  public void testReplayHandlesMultipleOutputs() throws Exception {
    Assume.assumeTrue(new File("/bin/sh").isFile());
    final List<String[]> commands = new RecordingMojo().history();
    for (final String name : new String[] {
        "one", "two"
    }) {
      record(directory, name, name, true, true, commands);
    }
    assertEquals(0, execute(script(commands, "sh", null), "sh"));
    for (final String name : new String[] {
        "one", "two"
    }) {
      assertTrue(new File(directory, name + ".map").isFile());
      assertEquals(name, read(new File(directory, name + ".map")));
    }
    assertNoTemporaryMaps(directory);
  }

  @Test
  public void testReplayPublishesMapsFromNormalAndDryRuns() throws Exception {
    for (final String shell : new String[] {
        "sh", "bash"
    }) {
      Assume.assumeTrue(new File("/bin/" + shell).isFile());
      for (final boolean dry : new boolean[] {
          false, true
      }) {
        final File working = new File(directory, shell + "-" + dry);
        assertTrue(working.mkdir());
        final String name = "output %,'$&.so";
        final File output = new File(working, name);
        final File map = new File(working, name + ".map");
        final List<String[]> commands = record(working, name, "map contents", dry, true, new RecordingMojo().history());
        if (dry)
          assertEquals("Dry-run must only record commands", 0, working.list().length);
        Files.deleteIfExists(output.toPath());
        Files.deleteIfExists(map.toPath());
        final int status = execute(script(commands, shell, null), shell);
        assertEquals(read(new File(directory, "run.log")), 0, status);
        assertEquals("map contents", read(output));
        assertTrue("Replay must publish the map under the final name", map.isFile());
        assertEquals("map contents", read(map));
        assertEquals("Temporary maps must be cleaned up", 2, working.list().length);
      }
    }
  }

  @Test
  public void testReplayRejectsMissingMapAfterSuccessfulLink() throws Exception {
    Assume.assumeTrue(new File("/bin/sh").isFile());
    final File map = new File(directory, "probe.map");
    Files.write(map.toPath(), "previous map".getBytes(StandardCharsets.UTF_8));
    final List<String[]> commands = record(directory, "probe", "MISSING", true, true, new RecordingMojo().history());
    assertTrue(execute(script(commands, "sh", null), "sh") != 0);
    assertEquals("previous map", read(map));
    assertNoTemporaryMaps(directory);
  }

  @Test
  public void testReplayRejectsRemovalOfExecutable() throws Exception {
    final List<String[]> history = recordArguments(directory, Arrays.asList("value"), true, false);
    for (final String shell : new String[] {
        "sh", "bash", "bat"
    }) {
      for (final String type : new String[] {
          "string", "regex"
      }) {
        final String executable = history.get(0)[0];
        final String match = "regex".equals(type) ? "^" + java.util.regex.Pattern.quote(executable) + "$" : executable;
        try {
          scriptWithSubstitutions(history, shell, Arrays.asList(substitution(type, match, "")));
          fail("Removing the executable must fail during script generation");
        } catch (final MojoExecutionException expected) {
          assertTrue(expected.getMessage(), expected.getMessage().contains("executable"));
        }
      }
    }
  }

  @Test
  public void testReplayRejectsStaleMap() throws Exception {
    Assume.assumeTrue(new File("/bin/sh").isFile());
    final List<String[]> commands = record(directory, "probe", "MISSING", true, true, new RecordingMojo().history());
    for (final String argument : commands.get(0)) {
      if (argument.startsWith("-Map=")) {
        Files.write(new File(directory, argument.substring(5)).toPath(), "stale map".getBytes(StandardCharsets.UTF_8));
      }
    }
    assertTrue(execute(script(commands, "sh", null), "sh") != 0);
    assertFalse(new File(directory, "probe.map").exists());
    assertNoTemporaryMaps(directory);
  }

  @Test
  public void testSubstitutionCanIntroduceSpacesAndQuotes() throws Exception {
    Assume.assumeTrue(new File("/bin/sh").isFile());
    final List<String[]> commands = record(directory, "result", "REPLAY_VALUE", true);
    final Substitution sub = new Substitution();
    sub.setReplace("REPLAY_VALUE");
    sub.setReplaceWith("a value with 'quotes' and $HOME");
    final File scriptFile = script(commands, "sh", sub);
    final Process process = new ProcessBuilder("/bin/sh", scriptFile.getAbsolutePath()).directory(directory)
        .redirectErrorStream(true).redirectOutput(new File(directory, "run.log")).start();
    final int status = process.waitFor();
    assertEquals(read(new File(directory, "run.log")), 0, status);
    assertEquals(sub.getReplaceWith(), read(new File(directory, "result")));
  }
}
