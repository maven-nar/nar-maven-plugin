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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @author Mark Donszelmann
 */
public final class NarUtil {
  private static final class StreamGobbler extends Thread {
    private final InputStream is;

    private final TextStream ts;

    private StreamGobbler(final InputStream is, final TextStream ts) {
      this.is = is;
      this.ts = ts;
    }

    @Override
    public void run() {
      try {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(this.is));
        String line = null;
        while ((line = reader.readLine()) != null) {
          this.ts.println(line);
        }
        reader.close();
      } catch (final IOException e) {
        // e.printStackTrace()
        final StackTraceElement[] stackTrace = e.getStackTrace();
        for (final StackTraceElement element : stackTrace) {
          this.ts.println(element.toString());
        }
      }
    }
  }

  public static final String DEFAULT_EXCLUDES = "**/*~,**/#*#,**/.#*,**/%*%,**/._*,"
      + "**/CVS,**/CVS/**,**/.cvsignore," + "**/SCCS,**/SCCS/**,**/vssver.scc," + "**/.svn,**/.svn/**,**/.DS_Store";

  public static String addLibraryPathToEnv(final String path, final Map environment, final String os) {
    String pathName = null;
    char separator = ' ';
    switch (os) {
      case OS.WINDOWS:
        pathName = "PATH";
        separator = ';';
        break;
      case OS.MACOSX:
        pathName = "DYLD_LIBRARY_PATH";
        separator = ':';
        break;
      case OS.AIX:
        pathName = "LIBPATH";
        separator = ':';
        break;
      default:
        pathName = "LD_LIBRARY_PATH";
        separator = ':';
        break;
    }

    String value = environment != null ? (String) environment.get(pathName) : null;
    if (value == null) {
      value = NarUtil.getEnv(pathName, pathName, null);
    }

    String libPath = path;
    libPath = libPath.replace(File.pathSeparatorChar, separator);
    if (value != null) {
      value += separator + libPath;
    } else {
      value = libPath;
    }
    if (environment != null) {
      environment.put(pathName, value);
    }
    return pathName + "=" + value;
  }

  /**
   * (Darren) this code lifted from mvn help:active-profiles plugin Recurses
   * into the project's parent poms to find the active profiles of the
   * specified project and all its parents.
   * 
   * @param project
   *          The project to start with
   * @return A list of active profiles
   */
  static List collectActiveProfiles(final MavenProject project) {
    final List profiles = project.getActiveProfiles();

    if (project.hasParent()) {
      profiles.addAll(collectActiveProfiles(project.getParent()));
    }

    return profiles;
  }

  public static int copyDirectoryStructure(final File sourceDirectory, final File destinationDirectory,
      final String includes, final String excludes) throws IOException {
    if (!sourceDirectory.exists()) {
      throw new IOException("Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ").");
    }

    final List files = FileUtils.getFiles(sourceDirectory, includes, excludes);
    final String sourcePath = sourceDirectory.getAbsolutePath();

    int copied = 0;
    for (final Object file1 : files) {
      final File file = (File) file1;
      String dest = file.getAbsolutePath();
      dest = dest.substring(sourcePath.length() + 1);
      final File destination = new File(destinationDirectory, dest);
      if (file.isFile()) {
        // destination = destination.getParentFile();
        // use FileUtils from commons-io, because it preserves timestamps
        org.apache.commons.io.FileUtils.copyFile(file, destination);
        copied++;

        // copy executable bit
        try {
          // 1.6 only so coded using introspection
          // destination.setExecutable( file.canExecute(), false );
          final Method canExecute = file.getClass().getDeclaredMethod("canExecute");
          final Method setExecutable = destination.getClass()
              .getDeclaredMethod("setExecutable", boolean.class, boolean.class);
          setExecutable
              .invoke(destination, canExecute.invoke(file), Boolean.FALSE);
        } catch (final SecurityException | InvocationTargetException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException e) {
          // ignored
        }
      } else if (file.isDirectory()) {
        if (!destination.exists() && !destination.mkdirs()) {
          throw new IOException("Could not create destination directory '" + destination.getAbsolutePath() + "'.");
        }
        copied += copyDirectoryStructure(file, destination, includes, excludes);
      } else {
        throw new IOException("Unknown file type: " + file.getAbsolutePath());
      }
    }
    return copied;
  }

  public static void deleteDirectory(final File dir) throws MojoExecutionException {
    int retries = OS.WINDOWS.equalsIgnoreCase(System.getProperty("os.name")) ? 3 : 1;  // Windows file locking (such as due to virus scanners) and sometimes deleting slowly, or slow to report completion.
    while (retries > 0) {
      retries--;
      try {
        FileUtils.deleteDirectory(dir);
        retries = 0;
      } catch (final IOException e) {
        if (retries > 0) {
          Thread.yield();
        } else {
          throw new MojoExecutionException("Could not delete directory: " + dir, e);
        }
      }
      if (retries > 0) {
//      getLog().info("Could not delete directory: " + dir + " : Retrying");
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
        }
      //TODO: if( windows and interactive ) prompt for retry?
      //@Component(role=org.codehaus.plexus.components.interactivity.Prompter.class, hint="archetype")
      //public class ArchetypePrompter
      }
    }
  }
  
  static Set findInstallNameToolCandidates(final File[] files, final Log log)
      throws MojoExecutionException, MojoFailureException {
    final HashSet candidates = new HashSet();

    for (final File file2 : files) {
      final File file = file2;

      if (!file.exists()) {
        continue;
      }

      if (file.isDirectory()) {
        candidates.addAll(findInstallNameToolCandidates(file.listFiles(), log));
      }

      final String fileName = file.getName();
      if (file.isFile() && file.canWrite()
          && (fileName.endsWith(".so") || fileName.endsWith(".dylib") || fileName.endsWith(".jnilib"))) {
        candidates.add(file);
      }
    }

    return candidates;
  }

  // FIXME, should go to AOL.
  /*
   * NOT USED ?
   * public static String getAOLKey( String architecture, String os, Linker
   * linker )
   * throws MojoFailureException, MojoExecutionException
   * {
   * // construct AOL key prefix
   * return getArchitecture( architecture ) + "." + getOS( os ) + "." +
   * getLinkerName( architecture, os, linker )
   * + ".";
   * }
   */

  public static AOL getAOL(final MavenProject project, final String architecture, final String os, final Linker linker,
      final String aol, final Log log) throws MojoFailureException, MojoExecutionException {

    /*
    To support a linker that is not the default linker specified in the aol.properties
    * */
    String aol_linker;

    if(linker != null & linker.getName() != null)
    {
      log.debug("linker original name: " + linker.getName());
      aol_linker = linker.getName();
    }
    else
    {
      log.debug("linker original name not exist ");
      aol_linker = getLinkerName(project, architecture, os,linker, log);
    }
    log.debug("aol_linker: " + aol_linker);

    return aol == null ? new AOL(getArchitecture(architecture), getOS(os), aol_linker) : new AOL(aol);
  }

  public static String getAOLKey(final String aol) {
    // FIXME, this may not always work correctly
    return replace("-", ".", aol);
  }

  public static String getArchitecture(final String architecture) {
    if (architecture == null) {
      return System.getProperty("os.arch");
    }
    return architecture;
  }

  /**
   * Returns the Bcel Class corresponding to the given class filename
   * 
   * @param filename
   *          the absolute file name of the class
   * @return the Bcel Class.
   * @throws IOException
   */
  public static JavaClass getBcelClass(final String filename) throws IOException {
    final ClassParser parser = new ClassParser(filename);
    return parser.parse();
  }

  public static String getEnv(final String envKey, final String alternateSystemProperty, final String defaultValue) {
    String envValue = null;
    try {
      envValue = System.getenv(envKey);
      if (envValue == null && alternateSystemProperty != null) {
        envValue = System.getProperty(alternateSystemProperty);
      }
    } catch (final Error e) {
      // JDK 1.4?
      if (alternateSystemProperty != null) {
        envValue = System.getProperty(alternateSystemProperty);
      }
    }

    if (envValue == null) {
      envValue = defaultValue;
    }

    return envValue;
  }

  /**
   * Returns the header file name (javah) corresponding to the given class file
   * name
   * 
   * @param filename
   *          the absolute file name of the class
   * @return the header file name.
   */
  public static String getHeaderName(final String basename, final String filename) {
    final String base = basename.replaceAll("\\\\", "/");
    final String file = filename.replaceAll("\\\\", "/");
    if (!file.startsWith(base)) {
      throw new IllegalArgumentException("Error " + file + " does not start with " + base);
    }
    String header = file.substring(base.length() + 1);
    header = header.replaceAll("/", "_");
    header = header.replaceAll("\\.class", ".h");
    return header;
  }

  public static File getJavaHome(final File javaHome, final String os) {
    File home = javaHome;
    // adjust JavaHome
    if (home == null) {
      home = new File(System.getProperty("java.home"));
      if (home.getName().equals("jre")) {
        // we want the JDK base directory, not the JRE subfolder
        home = home.getParentFile();
      }
    }
    return home;
  }

  public static Linker getLinker(final Linker linker, final Log log) {
    Linker link = linker;
    if (link == null) {
      link = new Linker(log);
    }
    return link;
  }

  public static String getLinkerName(final MavenProject project, final String architecture, final String os,
      final Linker linker, final Log log) throws MojoFailureException, MojoExecutionException {
    return getLinker(linker, log).getName(NarProperties.getInstance(project),
        getArchitecture(architecture) + "." + getOS(os) + ".");
  }

  public static String getOS(final String defaultOs) {
    String os = defaultOs;
    // adjust OS if not given
    if (os == null) {
      os = System.getProperty("os.name");
      final String name = os.toLowerCase();
      if (name.startsWith("windows")) {
        os = OS.WINDOWS;
      }
      if (name.startsWith("linux")) {
        os = OS.LINUX;
      }
      if (name.startsWith("freebsd")) {
        os = OS.FREEBSD;
      }
      if (name.equals("mac os x")) {
        os = OS.MACOSX;
      }
	 if ( name.equals( "AIX" ) )
	{
		os = OS.AIX;
	}
	if ( name.equals( "SunOS" ) )
	{
		os = OS.SUNOS;
	}
    }
    return os;
  }

  public static boolean isWindows() {
    return Objects.equals(getOS(null), OS.WINDOWS);
  }

  public static void makeExecutable(final File file, final Log log) throws MojoExecutionException, MojoFailureException {
    if (!file.exists()) {
      return;
    }

    if (file.isDirectory()) {
      final File[] files = file.listFiles();
      for (final File file2 : files) {
        makeExecutable(file2, log);
      }
    }
    if (file.isFile() && file.canRead() && file.canWrite() && !file.isHidden()) {
      // chmod +x file
      final int result = runCommand("chmod", new String[] {
          "+x", file.getPath()
      }, null, null, log);
      if (result != 0) {
        throw new MojoExecutionException("Failed to execute 'chmod +x " + file.getPath() + "'" + " return code: \'"
            + result + "\'.");
      }
    }
  }

  public static void makeLink(final File file, final Log log) throws MojoExecutionException, MojoFailureException {
    if (!file.exists()) {
      return;
    }

    if (file.isDirectory()) {
      final File[] files = file.listFiles();
      for (final File file2 : files) {
        makeLink(file2, log);
      }
    }
    if (file.isFile() && file.canRead() && file.canWrite() && !file.isHidden()
        && file.getName().matches(".*\\.so(\\.\\d+)+$")) {
      final File sofile = new File(file.getParent(), file.getName().substring(0, file.getName().indexOf(".so") + 3));
      if (!sofile.exists()) {
        // ln -s lib.so.xx lib.so
        final int result = runCommand("ln", new String[] {
            "-s", file.getName(), sofile.getPath()
        }, null, null, log);
        if (result != 0) {
          throw new MojoExecutionException("Failed to execute 'ln -s " + file.getName() + " " + sofile.getPath() + "'"
              + " return code: \'" + result + "\'.");
        }
      }
    }
  }

  /* for jdk 1.4 */
  private static String quote(final String s) {
    final String escQ = "\\Q";
    final String escE = "\\E";

    int slashEIndex = s.indexOf(escE);
    if (slashEIndex == -1) {
      return escQ + s + escE;
    }

    final StringBuffer sb = new StringBuffer(s.length() * 2);
    sb.append(escQ);
    slashEIndex = 0;
    int current = 0;
    while ((slashEIndex = s.indexOf(escE, current)) != -1) {
      sb.append(s.substring(current, slashEIndex));
      current = slashEIndex + 2;
      sb.append(escE);
      sb.append("\\");
      sb.append(escE);
      sb.append(escQ);
    }
    sb.append(s.substring(current, s.length()));
    sb.append(escE);
    return sb.toString();
  }

  /* for jdk 1.4 */
  private static String quoteReplacement(final String s) {
    if (s.indexOf('\\') == -1 && s.indexOf('$') == -1) {
      return s;
    }
    final StringBuffer sb = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c == '\\') {
        sb.append('\\');
        sb.append('\\');
      } else if (c == '$') {
        sb.append('\\');
        sb.append('$');
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  static void removeNulls(final Collection<?> collection) {
    for (final Iterator<?> iter = collection.iterator(); iter.hasNext();) {
      if (iter.next() == null) {
        iter.remove();
      }
    }
  }

  /**
   * Replaces target with replacement in string. For jdk 1.4 compatiblity.
   * 
   * @param target
   * @param replacement
   * @param string
   * @return
   */
  public static String replace(final CharSequence target, final CharSequence replacement, final String string) {
    return Pattern.compile(quote(target.toString())/*
                                                    * , Pattern.LITERAL jdk 1.4
                                                    */).matcher(string).replaceAll(
    /* Matcher. jdk 1.4 */quoteReplacement(replacement.toString()));
  }

  public static int runCommand(final String cmd, final String[] args, final File workingDirectory, final String[] env,
      final Log log) throws MojoExecutionException, MojoFailureException {
    if (log.isInfoEnabled()) {
      final StringBuilder argLine = new StringBuilder();
      if (args != null) {
        for (final String arg : args) {
          argLine.append(" ").append(arg);
        }
      }
      if (workingDirectory != null) {
        log.info("+ cd " + workingDirectory.getAbsolutePath());
      }
      log.info("+ " + cmd + argLine);
    }
    return runCommand(cmd, args, workingDirectory, env, new TextStream() {
      @Override
      public void println(final String text) {
        log.info(text);
      }
    }, new TextStream() {
      @Override
      public void println(final String text) {
        log.error(text);
      }

    }, new TextStream() {
      @Override
      public void println(final String text) {
        log.debug(text);
      }
    }, log);
  }

  public static int runCommand(final String cmd, final String[] args, final File workingDirectory, final String[] env,
      final TextStream out, final TextStream err, final TextStream dbg, final Log log)
      throws MojoExecutionException, MojoFailureException {
    return runCommand(cmd, args, workingDirectory, env, out, err, dbg, log, false);
  }

  public static int runCommand(final String cmd, final String[] args, final File workingDirectory, final String[] env,
      final TextStream out, final TextStream err, final TextStream dbg, final Log log, final boolean expectFailure)
      throws MojoExecutionException, MojoFailureException {
    final Commandline cmdLine = new Commandline();

    try {
      dbg.println("RunCommand: " + cmd);
      cmdLine.setExecutable(cmd);
      if (args != null) {
        for (final String arg : args) {
          dbg.println("  '" + arg + "'");
        }
        cmdLine.addArguments(args);
      }
      if (workingDirectory != null) {
        dbg.println("in: " + workingDirectory.getPath());
        cmdLine.setWorkingDirectory(workingDirectory);
      }

      if (env != null) {
        dbg.println("with Env:");
        for (final String element : env) {
          final String[] nameValue = element.split("=", 2);
          if (nameValue.length < 2) {
            throw new MojoFailureException("   Misformed env: '" + element + "'");
          }
          dbg.println("   '" + nameValue[0] + "=" + nameValue[1] + "'");
          cmdLine.addEnvironment(nameValue[0], nameValue[1]);
        }
      }

      final Process process = cmdLine.execute();
      final StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), err);
      final StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), out);

      errorGobbler.start();
      outputGobbler.start();
      process.waitFor();
      final int exitValue = process.exitValue();
      dbg.println("ExitValue: " + exitValue);
      final int timeout = 5000;
      errorGobbler.join(timeout);
      outputGobbler.join(timeout);
      if (exitValue != 0 ^ expectFailure) {
        if (log == null) {
          System.err.println(err.toString());
          System.err.println(out.toString());
          System.err.println(dbg.toString());
        } else {
          log.warn(err.toString());
          log.warn(out.toString());
          log.warn(dbg.toString());
        }
        throw new MojoExecutionException("exit code: " + exitValue);
      }
      return exitValue;
    } catch (final MojoExecutionException e) {
      throw e;
    } catch (final Exception e) {
      throw new MojoExecutionException("Could not launch " + cmdLine, e);
    }
  }

  static void runInstallNameTool(final File[] files, final Log log) throws MojoExecutionException, MojoFailureException {
    final Set libs = findInstallNameToolCandidates(files, log);

    for (final Object lib1 : libs) {
      final File subjectFile = (File) lib1;
      final String subjectName = subjectFile.getName();
      final String subjectPath = subjectFile.getPath();

      final int idResult = runCommand("install_name_tool", new String[] { "-id", subjectPath, subjectPath
      }, null, null, log);

      if (idResult != 0) {
        throw new MojoExecutionException(
            "Failed to execute 'install_name_tool -id " + subjectPath + " " + subjectPath + "'" + " return code: \'"
                + idResult + "\'.");
      }

      for (final Object lib : libs) {
        final File dependentFile = (File) lib;
        final String dependentPath = dependentFile.getPath();

        if (Objects.equals(dependentPath, subjectPath)) {
          continue;
        }

        final int changeResult = runCommand("install_name_tool",
            new String[] { "-change", subjectName, subjectPath, dependentPath
            }, null, null, log);

        if (changeResult != 0) {
          throw new MojoExecutionException(
              "Failed to execute 'install_name_tool -change " + subjectName + " " + subjectPath + " " + dependentPath
                  + "'" + " return code: \'" + changeResult + "\'.");
        }
      }
    }
  }

  public static void runRanlib(final File file, final Log log) throws MojoExecutionException, MojoFailureException {
    if (!file.exists()) {
      return;
    }

    if (file.isDirectory()) {
      final File[] files = file.listFiles();
      for (final File file2 : files) {
        runRanlib(file2, log);
      }
    }
    if (file.isFile() && file.canWrite() && !file.isHidden() && file.getName().endsWith(".a")) {
      // ranlib file
      final int result = runCommand("ranlib", new String[] {
        file.getPath()
      }, null, null, log);
      if (result != 0) {
        throw new MojoExecutionException("Failed to execute 'ranlib " + file.getPath() + "'" + " return code: \'"
            + result + "\'.");
      }
    }
  }

  /**
   * Produces a human-readable string of the given object which has fields
   * annotated with the Maven {@link Parameter} annotation.
   * 
   * @param o The object for which a human-readable string is desired.
   * @return A human-readable string, with each {@code @Parameter} field on a
   *         separate line rendered as a key/value pair.
   */
  public static String prettyMavenString(final Object o) {
    final StringBuilder sb = new StringBuilder();
    sb.append(o.getClass().getName()).append(":\n");
    for (final Field f : o.getClass().getDeclaredFields()) {
      if (f.getAnnotation(Parameter.class) == null) continue;
      sb.append("\t").append(f.getName()).append("=").append(fieldValue(f, o)).append("\n");
    }
    return sb.toString();
  }

  private static Object fieldValue(final Field f, final Object o) {
    try {
      return f.get(o);
    }
    catch (final IllegalArgumentException | IllegalAccessException exc) {
      return "<ERROR>";
    }
  }

  private NarUtil() {
    // never instantiate
  }
}
