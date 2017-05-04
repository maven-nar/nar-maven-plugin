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
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.util.StringUtils;

/**
 * Some utilities used by the CC and Link tasks.
 *
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public class CUtil {
  /**
   * A class that splits a white-space, comma-separated list into a String
   * array. Used for task attributes.
   */
  public static final class StringArrayBuilder {
    private final String[] _value;

    public StringArrayBuilder(final String value) {
      // Split the defines up
      final StringTokenizer tokens = new StringTokenizer(value, ", ");
      final Vector vallist = new Vector();
      while (tokens.hasMoreTokens()) {
        final String val = tokens.nextToken().trim();
        if (val.length() == 0) {
          continue;
        }
        vallist.addElement(val);
      }
      this._value = new String[vallist.size()];
      vallist.copyInto(this._value);
    }

    public String[] getValue() {
      return this._value;
    }
  }

  public final static int FILETIME_EPSILON = 500;

  /**
   * Adds the elements of the array to the given vector
   */
  public static void addAll(final Vector dest, final Object[] src) {
    if (src == null) {
      return;
    }
    for (final Object element : src) {
      dest.addElement(element);
    }
  }

  /**
   * Checks a array of names for non existent or non directory entries and
   * nulls them out.
   *
   * @return Count of non-null elements
   */
  public static int checkDirectoryArray(final String[] names) {
    int count = 0;
    for (int i = 0; i < names.length; i++) {
      if (names[i] != null) {
        final File dir = new File(names[i]);
        if (dir.exists() && dir.isDirectory()) {
          count++;
        } else {
          names[i] = null;
        }
      }
    }
    return count;
  }

  /**
   * Extracts the basename of a file, removing the extension, if present
   */
  public static String getBasename(final File file) {
    final String path = file.getPath();
    // Remove the extension
    String basename = file.getName();
    final int pos = basename.lastIndexOf('.');
    if (pos != -1) {
      basename = basename.substring(0, pos);
    }
    return basename;
  }

  /**
   * Gets the parent directory for the executable file name using the current
   * directory and system executable path
   *
   * @param exeName
   *          Name of executable such as "cl.exe"
   * @return parent directory or null if not located
   */
  public static File getExecutableLocation(final String exeName) {
    //
    // must add current working directory to the
    // from of the path from the "path" environment variable
    final File currentDir = new File(System.getProperty("user.dir"));
    if (new File(currentDir, exeName).exists()) {
      return currentDir;
    }
    final File[] envPath = CUtil.getPathFromEnvironment("PATH", File.pathSeparator);
    for (final File element : envPath) {
      if (new File(element, exeName).exists()) {
        return element;
      }
    }
    return null;
  }

  /**
   * Extracts the parent of a file
   */
  public static String getParentPath(final String path) {
    final int pos = path.lastIndexOf(File.separator);
    if (pos <= 0) {
      return null;
    }
    return path.substring(0, pos);
  }

  /**
   * Returns an array of File for each existing directory in the specified
   * environment variable
   *
   * @param envVariable
   *          environment variable name such as "LIB" or "INCLUDE"
   * @param delim
   *          delimitor used to separate parts of the path, typically ";"
   *          or ":"
   * @return array of File's for each part that is an existing directory
   */
  public static File[] getPathFromEnvironment(final String envVariable, final String delim) {
    // OS/4000 does not support the env command.
    if (System.getProperty("os.name").equals("OS/400")) {
      return new File[] {};
    }
    final Vector osEnv = Execute.getProcEnvironment();
    final String match = envVariable.concat("=");
    for (final Enumeration e = osEnv.elements(); e.hasMoreElements();) {
      final String entry = ((String) e.nextElement()).trim();
      if (entry.length() > match.length()) {
        final String entryFrag = entry.substring(0, match.length());
        if (entryFrag.equalsIgnoreCase(match)) {
          final String path = entry.substring(match.length());
          return parsePath(path, delim);
        }
      }
    }
    final File[] noPath = new File[0];
    return noPath;
  }

  /**
   * Returns a relative path for the targetFile relative to the base
   * directory.
   *
   * @param base
   *          base directory as returned by File.getCanonicalPath()
   * @param targetFile
   *          target file
   * @return relative path of target file. Returns targetFile if there were
   *         no commonalities between the base and the target
   *
   */
  public static String getRelativePath(final String base, final File targetFile) {
    try {
      //
      // remove trailing file separator
      //
      String canonicalBase = base;
      if (base.charAt(base.length() - 1) != File.separatorChar) {
        canonicalBase = base + File.separatorChar;
      }
      //
      // get canonical name of target
      //
      String canonicalTarget;
      if (System.getProperty("os.name").equals("OS/400")) {
        canonicalTarget = targetFile.getPath();
      } else {
        canonicalTarget = targetFile.getCanonicalPath();
      }
      if (canonicalBase.startsWith(canonicalTarget + File.separatorChar)) {
        canonicalTarget = canonicalTarget + File.separator;
      }
      if (canonicalTarget.equals(canonicalBase)) {
        return ".";
      }
      //
      // see if the prefixes are the same
      //
      if (substringMatch(canonicalBase, 0, 2, "\\\\")) {
        //
        // UNC file name, if target file doesn't also start with same
        // server name, don't go there
        final int endPrefix = canonicalBase.indexOf('\\', 2);
        final String prefix1 = canonicalBase.substring(0, endPrefix);
        final String prefix2 = canonicalTarget.substring(0, endPrefix);
        if (!prefix1.equals(prefix2)) {
          return canonicalTarget;
        }
      } else {
        if (substringMatch(canonicalBase, 1, 3, ":\\")) {
          final int endPrefix = 2;
          final String prefix1 = canonicalBase.substring(0, endPrefix);
          final String prefix2 = canonicalTarget.substring(0, endPrefix);
          if (!prefix1.equals(prefix2)) {
            return canonicalTarget;
          }
        } else {
        	if (canonicalBase.charAt(0) == '/' && canonicalTarget.charAt(0) != '/') {
        		return canonicalTarget;
        	}
        }
      }
      final char separator = File.separatorChar;
      int lastCommonSeparator = -1;
      int minLength = canonicalBase.length();
      if (canonicalTarget.length() < minLength) {
        minLength = canonicalTarget.length();
      }
      //
      // walk to the shorter of the two paths
      // finding the last separator they have in common
      for (int i = 0; i < minLength; i++) {
        if (canonicalTarget.charAt(i) == canonicalBase.charAt(i)) {
          if (canonicalTarget.charAt(i) == separator) {
            lastCommonSeparator = i;
          }
        } else {
          break;
        }
      }
      final StringBuffer relativePath = new StringBuffer(50);
      //
      // walk from the first difference to the end of the base
      // adding "../" for each separator encountered
      //
      for (int i = lastCommonSeparator + 1; i < canonicalBase.length(); i++) {
        if (canonicalBase.charAt(i) == separator) {
          if (relativePath.length() > 0) {
            relativePath.append(separator);
          }
          relativePath.append("..");
        }
      }
      if (canonicalTarget.length() > lastCommonSeparator + 1) {
        if (relativePath.length() > 0) {
          relativePath.append(separator);
        }
        relativePath.append(canonicalTarget.substring(lastCommonSeparator + 1));
      }
      return relativePath.toString();
    } catch (final IOException ex) {
    }
    return targetFile.toString();
  }

  public static boolean isActive(final Project p, final String ifCond, final String unlessCond) throws BuildException {
    if (ifCond != null) {
      final String ifValue = p.getProperty(ifCond);
      if (ifValue == null) {
        return false;
      } else {
        if (ifValue.equals("false") || ifValue.equals("no")) {
          throw new BuildException("if condition \"" + ifCond + "\" has suspicious value \"" + ifValue);
        }
      }
    }
    if (unlessCond != null) {
      final String unlessValue = p.getProperty(unlessCond);
      if (unlessValue != null) {
        if (unlessValue.equals("false") || unlessValue.equals("no")) {
          throw new BuildException("unless condition \"" + unlessCond + "\" has suspicious value \"" + unlessValue);
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Determines whether time1 is later than time2
   * to a degree that file system time truncation is not significant.
   *
   * @param time1
   *          long first time value
   * @param time2
   *          long second time value
   * @return boolean if first time value is later than second time value.
   *         If the values are within the rounding error of the file system
   *         return false.
   */
  public static boolean isSignificantlyAfter(final long time1, final long time2) {
    return time1 > time2 + FILETIME_EPSILON;
  }

  /**
   * Determines whether time1 is earlier than time2
   * to a degree that file system time truncation is not significant.
   *
   * @param time1
   *          long first time value
   * @param time2
   *          long second time value
   * @return boolean if first time value is earlier than second time value.
   *         If the values are within the rounding error of the file system
   *         return false.
   */
  public static boolean isSignificantlyBefore(final long time1, final long time2) {
    return time1 + FILETIME_EPSILON < time2;
  }

  /**
   * Determines if source file has a system path,
   * that is part of the compiler or platform.
   * 
   * @param source
   *          source, may not be null.
   * @return true is source file appears to be system library
   *         and its path should be discarded.
   */
  public static boolean isSystemPath(final File source) {
    final String lcPath = source.getAbsolutePath().toLowerCase(java.util.Locale.US);
    return lcPath.contains("platformsdk") || lcPath.contains("windows kits") || lcPath.contains("microsoft")
        || Objects.equals(lcPath, "/usr/include")
        || Objects.equals(lcPath, "/usr/lib") || Objects.equals(lcPath, "/usr/local/include")
        || Objects.equals(lcPath, "/usr/local/lib");
  }

  /**
   * Parse a string containing directories into an File[]
   *
   * @param path
   *          path string, for example ".;c:\something\include"
   * @param delim
   *          delimiter, typically ; or :
   */
  public static File[] parsePath(final String path, final String delim) {
    final Vector libpaths = new Vector();
    int delimPos = 0;
    for (int startPos = 0; startPos < path.length(); startPos = delimPos + delim.length()) {
      delimPos = path.indexOf(delim, startPos);
      if (delimPos < 0) {
        delimPos = path.length();
      }
      //
      // don't add an entry for zero-length paths
      //
      if (delimPos > startPos) {
        final String dirName = path.substring(startPos, delimPos);
        final File dir = new File(dirName);
        if (dir.exists() && dir.isDirectory()) {
          libpaths.addElement(dir);
        }
      }
    }
    final File[] paths = new File[libpaths.size()];
    libpaths.copyInto(paths);
    return paths;
  }

  /**
   * This method is exposed so test classes can overload and test the
   * arguments without actually spawning the compiler
   */
  public static int runCommand(final CCTask task, final File workingDir, final String[] cmdline,
      final boolean newEnvironment, final Environment env) throws BuildException {
    try {
      task.log(Commandline.toString(cmdline), task.getCommandLogLevel());

     /* final Execute exe = new Execute(new LogStreamHandler(task, Project.MSG_INFO, Project.MSG_ERR));
      if (System.getProperty("os.name").equals("OS/390")) {
        exe.setVMLauncher(false);
      }
      exe.setAntRun(task.getProject());
      exe.setCommandline(cmdline);
      exe.setWorkingDirectory(workingDir);
      if (env != null) {
        final String[] environment = env.getVariables();
        if (environment != null) {
          for (final String element : environment) {
            task.log("Setting environment variable: " + element, Project.MSG_VERBOSE);
          }
        }
        exe.setEnvironment(environment);
      }
      exe.setNewenvironment(newEnvironment);
      return exe.execute();
            */
	  return CommandExecution.runCommand(cmdline,workingDir,task);
    } catch (final java.io.IOException exc) {
      throw new BuildException("Could not launch " + cmdline[0] + ": " + exc, task.getLocation());
    }
  }

  /**
   * Compares the contents of 2 arrays for equaliy.
   */
  public static boolean sameList(final Object[] a, final Object[] b) {
    if (a == null || b == null || a.length != b.length) {
      return false;
    }
    for (int i = 0; i < a.length; i++) {
      if (!a[i].equals(b[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compares the contents of an array and a Vector for equality.
   */
  public static boolean sameList(final Vector v, final Object[] a) {
    if (v == null || a == null || v.size() != a.length) {
      return false;
    }
    for (int i = 0; i < a.length; i++) {
      final Object o = a[i];
      if (!o.equals(v.elementAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compares the contents of an array and a Vector for set equality. Assumes
   * input array and vector are sets (i.e. no duplicate entries)
   */
  public static boolean sameSet(final Object[] a, final Vector b) {
    if (a == null || b == null || a.length != b.size()) {
      return false;
    }
    if (a.length == 0) {
      return true;
    }
    // Convert the array into a set
    final Hashtable t = new Hashtable();
    for (final Object element : a) {
      t.put(element, element);
    }
    for (int i = 0; i < b.size(); i++) {
      final Object o = b.elementAt(i);
      if (t.remove(o) == null) {
        return false;
      }
    }
    return t.size() == 0;
  }

  private static boolean
      substringMatch(final String src, final int beginIndex, final int endIndex, final String target) {
    if (src.length() < endIndex) {
      return false;
    }
    return src.substring(beginIndex, endIndex).equals(target);
  }

  /**
   * Converts a vector to a string array.
   */
  public static String[] toArray(final Vector src) {
    final String[] retval = new String[src.size()];
    src.copyInto(retval);
    return retval;
  }

  public static String toUnixPath(final String path) {
    if (File.separatorChar != '/' && path.indexOf(File.separatorChar) != -1) {
      return StringUtils.replace(path, File.separator, "/");
    }
    return path;
  }

  public static String toWindowsPath(final String path) {
    if (File.separatorChar != '\\' && path.indexOf(File.separatorChar) != -1) {
      return StringUtils.replace(path, File.separator, "\\");
    }
    return path;
  }

  /**
   * Replaces any embedded quotes in the string so that the value can be
   * placed in an attribute in an XML file
   *
   * @param attrValue
   *          value to be expressed
   * @return equivalent attribute literal
   *
   */
  public static String xmlAttribEncode(final String attrValue) {
    final StringBuffer buf = new StringBuffer(attrValue);
    int quotePos;

    for (quotePos = -1; (quotePos = buf.indexOf("\"", quotePos + 1)) >= 0;) {
      buf.deleteCharAt(quotePos);
      buf.insert(quotePos, "&quot;");
      quotePos += 5;
    }

    for (quotePos = -1; (quotePos = buf.indexOf("<", quotePos + 1)) >= 0;) {
      buf.deleteCharAt(quotePos);
      buf.insert(quotePos, "&lt;");
      quotePos += 3;
    }

    for (quotePos = -1; (quotePos = buf.indexOf(">", quotePos + 1)) >= 0;) {
      buf.deleteCharAt(quotePos);
      buf.insert(quotePos, "&gt;");
      quotePos += 3;
    }

    return buf.toString();
  }

}
