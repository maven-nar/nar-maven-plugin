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
package com.github.maven_nar.cpptasks.borland;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * A add-in class for Borland(r) processor adapters
 *
 * 
 */
public final class BorlandProcessor {
  public static void addWarningSwitch(final Vector<String> args, final int level) {
    switch (level) {
      case 0:
        args.addElement("-w-");
        break;
      case 5:
        args.addElement("-w!");
        break;
      default:
        args.addElement("-w");
        break;
    }
  }

  public static void getDefineSwitch(final StringBuffer buffer, final String define, final String value) {
    buffer.append("-D");
    buffer.append(define);
    if (value != null && value.length() > 0) {
      buffer.append('=');
      buffer.append(value);
    }
  }

  /**
   * This method extracts path information from the appropriate .cfg file in
   * the install directory.
   * 
   * @param toolName
   *          Tool name, for example, "bcc32", "brc32", "ilink32"
   * @param switchChar
   *          Command line switch character, for example "L" for libraries
   * @param defaultRelativePath
   *          default path relative to executable directory
   * @return path
   */
  public static File[] getEnvironmentPath(final String toolName, final char switchChar,
      final String[] defaultRelativePath) {
    if (toolName == null) {
      throw new NullPointerException("toolName");
    }
    if (defaultRelativePath == null) {
      throw new NullPointerException("defaultRelativePath");
    }
    String[] path = defaultRelativePath;
    File exeDir = CUtil.getExecutableLocation(toolName + ".exe");
    if (exeDir != null) {
      final File cfgFile = new File(exeDir, toolName + ".cfg");
      if (cfgFile.exists()) {
        try {
          final Reader reader = new BufferedReader(new FileReader(cfgFile));
          final BorlandCfgParser cfgParser = new BorlandCfgParser(switchChar);
          path = cfgParser.parsePath(reader);
          reader.close();
        } catch (final IOException ex) {
          //
          // could be logged
          //
        }
      }
    } else {
      //
      // if can't find the executable,
      // assume current directory to resolve relative paths
      //
      exeDir = new File(System.getProperty("user.dir"));
    }
    int nonExistant = 0;
    File[] resourcePath = new File[path.length];
    for (int i = 0; i < path.length; i++) {
      resourcePath[i] = new File(path[i]);
      if (!resourcePath[i].isAbsolute()) {
        resourcePath[i] = new File(exeDir, path[i]);
      }
      //
      // if any of the entries do not exist or are
      // not directories, null them out
      if (!(resourcePath[i].exists() && resourcePath[i].isDirectory())) {
        resourcePath[i] = null;
        nonExistant++;
      }
    }
    //
    // if there were some non-existant or non-directory
    // entries in the configuration file then
    // create a shorter array
    if (nonExistant > 0) {
      final File[] culled = new File[resourcePath.length - nonExistant];
      int index = 0;
      for (final File element : resourcePath) {
        if (element != null) {
          culled[index++] = element;
        }
      }
      resourcePath = culled;
    }
    return resourcePath;
  }

  public static String getIncludeDirSwitch(final String includeOption, final String includeDir) {
    final StringBuffer buf = new StringBuffer(includeOption);
    quoteFile(buf, includeDir);
    return buf.toString();
  }

  public static String[] getLibraryPatterns(final String[] libnames, final LibraryTypeEnum libType) {
    final StringBuffer buf = new StringBuffer();
    final String[] patterns = new String[libnames.length];
    for (int i = 0; i < libnames.length; i++) {
      buf.setLength(0);
      buf.append(libnames[i]);
      buf.append(".lib");
      patterns[i] = buf.toString();
    }
    return patterns;
  }

  public static String[] getOutputFileSwitch(final String outFile) {
    return new String[0];
  }

  public static void getUndefineSwitch(final StringBuffer buffer, final String define) {
    buffer.append("-U");
    buffer.append(define);
  }

  public static boolean isCaseSensitive() {
    return false;
  }

  /**
   * Prepares argument list to execute the linker using a response file.
   * 
   * @param outputFile
   *          linker output file
   * @param args
   *          output of prepareArguments
   * @return arguments for runTask
   */
  public static String[] prepareResponseFile(final File outputFile, final String[] args, final String continuation)
      throws IOException {
    final String baseName = outputFile.getName();
    final File commandFile = new File(outputFile.getParent(), baseName + ".rsp");
    final FileWriter writer = new FileWriter(commandFile);
    for (int i = 1; i < args.length - 1; i++) {
      writer.write(args[i]);
      //
      // if either the current argument ends with
      // or next argument starts with a comma then
      // don't split the line
      if (args[i].endsWith(",") || args[i + 1].startsWith(",")) {
        writer.write(' ');
      } else {
        //
        // split the line to make it more readable
        //
        writer.write(continuation);
      }
    }
    //
    // write the last argument
    //
    if (args.length > 1) {
      writer.write(args[args.length - 1]);
    }
    writer.close();
    final String[] execArgs = new String[2];
    execArgs[0] = args[0];
    //
    // left for the caller to decorate
    execArgs[1] = commandFile.toString();
    return execArgs;
  }

  public static void quoteFile(final StringBuffer buf, final String outPath) {
    if (outPath.charAt(0) != '\"'
        && (outPath.indexOf(' ') >= 0 || outPath.indexOf('-') >= 0 || outPath.indexOf('/') >= 0)) {
      buf.append('\"');
      buf.append(outPath);
      buf.append('\"');
    } else {
      buf.append(outPath);
    }
  }

  private BorlandProcessor() {
  }
}
