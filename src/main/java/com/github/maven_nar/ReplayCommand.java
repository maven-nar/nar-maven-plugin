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
package com.github.maven_nar;

import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * A raw argument vector, kept separate from shell syntax until a replay script
 * is written.
 */
public final class ReplayCommand {
  private static final String LEGACY_HEADER = "# nar-replay-v1\t";
  private static final String HEADER = "# nar-replay-v2\t";

  private static String batchPath(final String path) {
    return "\"" + path.replace("%", "%%") + "\"";
  }

  private static String command(final String[] args, final boolean batch) {
    final StringBuilder result = new StringBuilder();
    for (final String argument : args) {
      if (result.length() != 0)
        result.append(' ');
      result.append(batch && result.length() == 0 ? batchPath(argument) : quote(argument, batch));
    }
    return result.toString();
  }

  public static ReplayCommand decode(final String line) throws MojoExecutionException {
    try {
      final boolean legacy = line.startsWith(LEGACY_HEADER);
      final int offset = legacy ? 1 : 3;
      final String[] fields = line.substring((legacy ? LEGACY_HEADER : HEADER).length()).split("\t", -1);
      if (fields.length <= offset)
        throw new IllegalArgumentException("Missing command arguments");
      final String[] args = new String[fields.length - offset];
      for (int i = offset; i < fields.length; i++)
        args[i - offset] = URLDecoder.decode(fields[i], "UTF-8");
      return new ReplayCommand(URLDecoder.decode(fields[0], "UTF-8"), args,
          legacy ? "" : URLDecoder.decode(fields[1], "UTF-8"), legacy ? "" : URLDecoder.decode(fields[2], "UTF-8"));
    } catch (final IllegalArgumentException | UnsupportedEncodingException ex) {
      throw new MojoExecutionException("Invalid structured replay command", ex);
    }
  }

  private static String escapeBatch(final String line) {
    // Keep cmd outside quote mode; the quoted argv reaches the Windows runtime
    // unchanged.
    return line.replace("^", "^^").replace("\"", "^\"").replace("&", "^&").replace("|", "^|").replace("<", "^<")
        .replace(">", "^>").replace("(", "^(").replace(")", "^)");
  }

  public static boolean isRecord(final String line) {
    return line.startsWith(HEADER) || line.startsWith(LEGACY_HEADER);
  }

  private static String quote(final String argument, final boolean batch) {
    if (!batch)
      return "'" + argument.replace("'", "'\"'\"'") + "'";
    // Windows programs parse backslashes before quotes and at the end of a quoted
    // argument.
    final StringBuilder result = new StringBuilder("\"");
    int slashes = 0;
    for (int i = 0; i < argument.length(); i++) {
      final char c = argument.charAt(i);
      if (c == '\\') {
        slashes++;
      } else {
        for (int j = 0; j < slashes * (c == '"' ? 2 : 1); j++)
          result.append('\\');
        slashes = 0;
        if (c == '"')
          result.append('\\');
        result.append(c == '%' ? "%%" : Character.toString(c));
      }
    }
    for (int j = 0; j < slashes * 2; j++)
      result.append('\\');
    return escapeBatch(result.append('"').toString());
  }

  private static String substitute(final String value, final Script script) {
    String result = value;
    if (script.getSubstitutions() != null) {
      for (final Substitution substitution : script.getSubstitutions())
        result = substitution.substitute(result);
    }
    return result;
  }

  private static void writeBatch(final Script script, final PrintWriter writer, final String cwd, final String line,
      final String temporary, final String destination) {
    writer.println("setlocal DisableDelayedExpansion");
    writer.println("pushd " + batchPath(cwd));
    writer.println("if errorlevel 1 exit /b 1");
    final String temp = temporary == null ? null : batchPath(".\\" + temporary);
    if (temp != null) {
      writer.println("if exist " + temp + " del /f /q " + temp);
      writer.println("if exist " + temp + " (popd & exit /b 1)");
    }
    if (script.isEchoLines())
      writer.println("echo " + escapeBatch(line));
    writer.println(line);
    writer.println("set \"_nar_replay_status=%errorlevel%\"");
    if (temp != null) {
      writer.println("if \"%_nar_replay_status%\"==\"0\" if not exist " + temp + " set \"_nar_replay_status=1\"");
      writer.println(
          "if \"%_nar_replay_status%\"==\"0\" for %%F in (" + temp + ") do if %%~zF==0 set \"_nar_replay_status=1\"");
      writer.println("set \"_nar_replay_attributes=\"");
      writer.println("if \"%_nar_replay_status%\"==\"0\" for %%F in (" + batchPath(".\\" + destination)
          + ") do set \"_nar_replay_attributes=%%~aF\"");
      writer.println("if \"%_nar_replay_attributes:~0,1%\"==\"d\" set \"_nar_replay_status=1\"");
      writer.println(
          "if \"%_nar_replay_status%\"==\"0\" move /y " + temp + " " + batchPath(".\\" + destination) + " >nul");
      writer.println("if \"%_nar_replay_status%\"==\"0\" set \"_nar_replay_status=%errorlevel%\"");
      writer.println("if exist " + temp + " del /f /q " + temp);
    }
    writer.println("popd");
    writer.println("if not \"%_nar_replay_status%\"==\"0\" exit /b %_nar_replay_status%");
    writer.println("endlocal");
  }

  private static void writePosix(final Script script, final PrintWriter writer, final String cwd, final String line,
      final String temporary, final String destination) {
    writer.println("(");
    // A dot prefix bypasses CDPATH and makes leading '-' a literal directory name.
    final String target = cwd.isEmpty() || cwd.startsWith("/") ? cwd : "./" + cwd;
    writer.println("cd " + quote(target, false) + " || exit $?");
    final String temp = temporary == null ? null : quote("./" + temporary, false);
    if (temp != null) {
      // Clear an earlier failed replay's map so a linker that produces nothing cannot
      // appear successful.
      writer.println("trap " + quote("rm -f " + temp, false) + " 0");
      writer.println("rm -f " + temp + " || exit $?");
    }
    if (script.isEchoLines())
      writer.println("printf '%s\\n' " + quote(line, false));
    writer.println(line);
    if (temp != null) {
      writer.println("_nar_replay_status=$?");
      writer.println("if [ \"$_nar_replay_status\" -ne 0 ]; then exit \"$_nar_replay_status\"; fi");
      writer.println("test -s " + temp + " || exit 1");
      final String output = quote("./" + destination, false);
      writer.println("test ! -d " + output + " || exit 1");
      writer.println("mv -f " + temp + " " + output + " || exit $?");
    }
    writer.println(") || exit $?");
  }

  private final String directory;

  private final String[] arguments;

  private final String temporaryMap;

  private final String finalMap;

  public ReplayCommand(final File directory, final String[] arguments) {
    this(directory.getAbsolutePath(), arguments, "", "");
  }

  public ReplayCommand(final File directory, final String[] arguments, final String temporaryMap,
      final String finalMap) {
    this(directory.getAbsolutePath(), arguments, temporaryMap == null ? "" : temporaryMap,
        finalMap == null ? "" : finalMap);
  }

  private ReplayCommand(final String directory, final String[] arguments, final String temporaryMap,
      final String finalMap) {
    this.directory = directory;
    this.arguments = arguments.clone();
    this.temporaryMap = temporaryMap;
    this.finalMap = finalMap;
  }

  public String display() {
    return ("(cd " + quote(this.directory, false) + " && " + command(this.arguments, false) + ")").replace("\r", "\\r")
        .replace("\n", "\\n");
  }

  /** Preserve literal arguments in metadata alongside a readable command. */
  public String encode() throws UnsupportedEncodingException {
    final StringBuilder result = new StringBuilder(HEADER).append(URLEncoder.encode(this.directory, "UTF-8"));
    result.append('\t').append(URLEncoder.encode(this.temporaryMap, "UTF-8"));
    result.append('\t').append(URLEncoder.encode(this.finalMap, "UTF-8"));
    for (final String argument : this.arguments)
      result.append('\t').append(URLEncoder.encode(argument, "UTF-8"));
    return result.toString();
  }

  public void write(final Script script, final PrintWriter writer) throws MojoExecutionException {
    final boolean batch = "bat".equals(script.getScriptType());
    if (!batch && !"sh".equals(script.getScriptType()) && !"bash".equals(script.getScriptType())) {
      throw new MojoExecutionException("Unknown replay script type: " + script.getScriptType());
    }
    final String cwd = substitute(this.directory, script);
    final List<String> args = new ArrayList<>();
    for (int i = 0; i < this.arguments.length; i++) {
      final String value = substitute(this.arguments[i], script);
      // Only an explicitly emptied argument is removed, after every substitution has
      // run.
      if (!this.arguments[i].isEmpty() && value.isEmpty()) {
        if (i == 0)
          throw new MojoExecutionException("Replay substitutions removed the executable: " + this.arguments[i]);
        continue;
      }
      args.add(value);
    }
    final String line = command(args.toArray(new String[args.size()]), batch);
    final String temporary = this.temporaryMap.isEmpty() ? null : substitute(this.temporaryMap, script);
    final String destination = this.finalMap.isEmpty() ? null : substitute(this.finalMap, script);
    if (batch) {
      writeBatch(script, writer, cwd, line, temporary, destination);
    } else {
      writePosix(script, writer, cwd, line, temporary, destination);
    }
  }
}
