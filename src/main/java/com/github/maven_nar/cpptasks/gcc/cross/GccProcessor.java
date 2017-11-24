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
package com.github.maven_nar.cpptasks.gcc.cross;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.compiler.CaptureStreamHandler;

/**
 * A add-in class for Gcc processors
 *
 * 
 */
public class GccProcessor {
  // the results from gcc -dumpmachine
  private static String machine;
  private static String[] specs;
  // the results from gcc -dumpversion
  private static String version;

  private static int addLibraryPatterns(final String[] libnames, final StringBuffer buf, final String prefix,
      final String extension, final String[] patterns, final int offset) {
    for (int i = 0; i < libnames.length; i++) {
      buf.setLength(0);
      buf.append(prefix);
      buf.append(libnames[i]);
      buf.append(extension);
      patterns[offset + i] = buf.toString();
    }
    return offset + libnames.length;
  }

  /**
   * Converts absolute Cygwin file or directory names to the corresponding
   * Win32 name.
   * 
   * @param names
   *          array of names, some elements may be null, will be changed in
   *          place.
   */
  public static void convertCygwinFilenames(final String[] names) {
    if (names == null) {
      throw new NullPointerException("names");
    }
    final File gccDir = CUtil.getExecutableLocation("gcc.exe");
    if (gccDir != null) {
      final String prefix = gccDir.getAbsolutePath() + "/..";
      final StringBuffer buf = new StringBuffer();
      for (int i = 0; i < names.length; i++) {
        final String name = names[i];
        if (name != null && name.length() > 1 && name.charAt(0) == '/') {
          buf.setLength(0);
          buf.append(prefix);
          buf.append(name);
          names[i] = buf.toString();
        }
      }
    }
  }

  public static String getMachine() {
    if (machine == null) {
      final String[] args = new String[] {
          "gcc", "-dumpmachine"
      };
      final String[] cmdout = CaptureStreamHandler.run(args);
      if (cmdout.length == 0) {
        machine = "nomachine";
      } else {
        machine = cmdout[0];
      }
    }
    return machine;
  }

  public static String[] getOutputFileSwitch(final String letter, final String outputFile) {
    final StringBuffer buf = new StringBuffer();
    if (outputFile.indexOf(' ') >= 0) {
      buf.append('"');
      buf.append(outputFile.replace('\\', '/'));
      buf.append('"');
    } else {
      buf.append(outputFile.replace('\\', '/'));
    }
    final String[] retval = new String[] {
        letter, buf.toString()
    };
    return retval;
  }

  /**
   * Returns the contents of the gcc specs file.
   * 
   * The implementation locates gcc.exe in the executable path and then
   * builds a relative path name from the results of -dumpmachine and
   * -dumpversion. Attempts to use gcc -dumpspecs to provide this information
   * resulted in stalling on the Execute.run
   * 
   * @return contents of the specs file
   */
  public static String[] getSpecs() {
    if (specs == null) {
      final File gccParent = CUtil.getExecutableLocation("gcc.exe");
      if (gccParent != null) {
        //
        // build a relative path like
        // ../lib/gcc-lib/i686-pc-cygwin/2.95.3-5/specs
        //
        //
        // resolve it relative to the location of gcc.exe
        //
        final String relativePath = "../lib/gcc-lib/" + getMachine() +
            '/' +
            getVersion() +
            "/specs";
        final File specsFile = new File(gccParent, relativePath);
        //
        // found the specs file
        //
        try {
          //
          // read the lines in the file
          //
          final BufferedReader reader = new BufferedReader(new FileReader(specsFile));
          final Vector<String> lines = new Vector<>(100);
          String line = reader.readLine();
          while (line != null) {
            lines.addElement(line);
            line = reader.readLine();
          }
          specs = new String[lines.size()];
          lines.copyInto(specs);
        } catch (final IOException ex) {
        }
      }
    }
    if (specs == null) {
      specs = new String[0];
    }
    return specs;
  }

  public static String getVersion() {
    if (version == null) {
      final String[] args = new String[] {
          "gcc", "-dumpversion"
      };
      final String[] cmdout = CaptureStreamHandler.run(args);
      if (cmdout.length == 0) {
        version = "noversion";
      } else {
        version = cmdout[0];
      }
    }
    return version;
  }

  public static boolean isCaseSensitive() {
    return true;
  }

  /**
   * Determines if task is running with cygwin
   * 
   * @return true if cygwin was detected
   */
  public static boolean isCygwin() {
    return getMachine().indexOf("cygwin") > 0;
  }

  private static boolean isHPUX() {
    final String osname = System.getProperty("os.name").toLowerCase();
    if (osname.contains("hp") && osname.contains("ux")) {
      return true;
    }
    return false;
  }

  /**
   * 
   * Parses the results of the specs file for a specific processor and
   * options
   * 
   * @param specsContent
   *          Contents of specs file as returned from getSpecs
   * @param specSectionStart
   *          start of spec section, for example "*cpp:"
   * @param options
   *          command line switches such as "-istart"
   */
  public static String[][]
      parseSpecs(final String[] specsContent, final String specSectionStart, final String[] options) {
    if (specsContent == null) {
      throw new NullPointerException("specsContent");
    }
    if (specSectionStart == null) {
      throw new NullPointerException("specSectionStart");
    }
    if (options == null) {
      throw new NullPointerException("option");
    }
    final String[][] optionValues = new String[options.length][];
    final StringBuffer optionValue = new StringBuffer(40);
    for (int i = 0; i < specsContent.length; i++) {
      String specLine = specsContent[i];
      //
      // if start of section then start paying attention
      //
      if (specLine.startsWith(specSectionStart)) {
        final Vector<String>[] optionVectors = new Vector[options.length];
        for (int j = 0; j < options.length; j++) {
          optionVectors[j] = new Vector<>(10);
        }
        //
        // go to next line and examine contents
        // and repeat until end of file
        //
        for (i++; i < specsContent.length; i++) {
          specLine = specsContent[i];
          for (int j = 0; j < options.length; j++) {
            int optionStart = specLine.indexOf(options[j]);
            while (optionStart >= 0) {
              optionValue.setLength(0);
              //
              // walk rest of line looking for first non
              // whitespace
              // and then next space
              boolean hasNonBlank = false;
              int k = optionStart + options[j].length();
              for (; k < specLine.length(); k++) {
                //
                // either a blank or a "}" (close of
                // conditional)
                // section will end the path
                //
                if (specLine.charAt(k) == ' ' || specLine.charAt(k) == '}') {
                  if (hasNonBlank) {
                    break;
                  }
                } else {
                  hasNonBlank = true;
                  optionValue.append(specLine.charAt(k));
                }
              }
              //
              // transition back to whitespace
              // value is over, add it to vector
              if (hasNonBlank) {
                optionVectors[j].addElement(optionValue.toString());
              }
              //
              // find next occurance on line
              //
              optionStart = specLine.indexOf(options[j], k);
            }
          }
        }
        //
        // copy vectors over to option arrays
        //
        for (int j = 0; j < options.length; j++) {
          optionValues[j] = new String[optionVectors[j].size()];
          optionVectors[j].copyInto(optionValues[j]);
        }
      }
    }
    //
    // fill in any missing option values with
    // a zero-length string array
    for (int i = 0; i < optionValues.length; i++) {
      final String[] zeroLenArray = new String[0];
      if (optionValues[i] == null) {
        optionValues[i] = zeroLenArray;
      }
    }
    return optionValues;
  }

  private GccProcessor() {
  }
}
