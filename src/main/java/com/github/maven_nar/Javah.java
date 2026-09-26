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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Configures JNI header generation with javah or javac -h.
 *
 * @author Mark Donszelmann
 */
public class Javah {

  /**
   * Legacy javah command to run. A custom name is preserved in auto mode;
   * javac mode ignores this setting.
   */
  @Parameter(defaultValue = "javah")
  private String name = "javah";

  /**
   * Header generator: auto, javah, or javac. Auto uses javah when the selected
   * JDK provides it (normally JDK 8 and 9), otherwise javac -h (JDK 10+).
   * Explicit javac mode requires JDK 8 or newer. Selection uses the Maven JDK
   * toolchain, or javaHome when no toolchain is selected.
   */
  @Parameter(defaultValue = "auto")
  private String mode = "auto";

  /**
   * Skip JNI header generation with either tool.
   */
  @Parameter
  private boolean skip = false;

  /**
   * Add boot class paths. By default none.
   */
  @Parameter
  private List/* <File> */ bootClassPaths = new ArrayList();

  /**
   * Add class paths. By default the classDirectory directory is included and
   * all dependent classes.
   */
  @Parameter
  private List/* <File> */ classPaths = new ArrayList();

  /**
   * The target directory into which to generate the output.
   */
  @Parameter(defaultValue = "${project.build.directory}/nar/javah-include", required = true)
  private File jniDirectory;

  /**
   * The class directory to scan for class files with native interfaces.
   */
  @Parameter(defaultValue = "${project.build.directory}/classes", required = true)
  private File classDirectory;

  /**
   * The set of files/patterns to include Defaults to "**\/*.class"
   */
  @Parameter
  private Set includes = new HashSet();

  /**
   * A list of exclusion filters.
   */
  @Parameter
  private Set excludes = new HashSet();

  /**
   * Additional header targets specified as binary class names. The javac
   * backend supports dependency-only and constants-only targets, but rejects
   * JDK/platform classes. Legacy javah adds these targets only when local
   * native classes trigger generation.
   */
  @Parameter
  private Set extraClasses = new HashSet();

  /**
   * The granularity in milliseconds of the last modification date for testing
   * whether a source needs recompilation
   */
  @Parameter(defaultValue = "0", required = true)
  private int staleMillis = 0;

  /**
   * The directory to store the timestampfile for the processed aid files.
   * Defaults to jniDirectory.
   */
  @Parameter
  private File timestampDirectory;

  /**
   * The timestampfile for the processed class files. Defaults to name of javah.
   */
  @Parameter
  private File timestampFile;

  private AbstractNarMojo mojo;

  public Javah() {
  }

  public final void execute() throws MojoExecutionException, MojoFailureException {

    if (skip) {
      this.mojo.getLog().info("javah skipped");
      return;
    }

    getClassDirectory().mkdirs();
    if (!"auto".equals(mode) && !"javah".equals(mode) && !"javac".equals(mode)) {
      throw new MojoExecutionException("Unknown javah.mode '" + mode + "'; expected auto, javah or javac");
    }
    Toolchain toolchain = getToolchain();
    if ("javac".equals(mode) || ("auto".equals(mode) && "javah".equals(name) && findTool(toolchain, "javah") == null)) {
      try {
        if (!JavacHeaders.hasTargets(getClassDirectory(), getIncludes(), excludes, extraClasses)) {
          return;
        }
      } catch (IOException ex) {
        throw new MojoExecutionException("Cannot discover JNI header targets", ex);
      }
      String compiler = findTool(toolchain, "javac");
      if (compiler == null) {
        throw new MojoExecutionException("Cannot find javac in the selected JDK (toolchain or javaHome)");
      }
      try {
        new JavacHeaders(new File(compiler),
            new File(this.mojo.getMavenProject().getBuild().getDirectory(), "nar/javac-headers"),
            asFiles(getClassPaths()), asFiles(bootClassPaths), this.mojo.getLog())
            .generate(getClassDirectory(), getIncludes(), excludes, extraClasses, getJniDirectory());
      } catch (IOException ex) {
        throw new MojoExecutionException("JNI header generation failed: " + ex.getMessage(), ex);
      }
      return;
    }

    try {
      final SourceInclusionScanner scanner = new StaleSourceScanner(this.staleMillis, getIncludes(), this.excludes);
      if (getTimestampDirectory().exists()) {
        scanner.addSourceMapping(new SingleTargetSourceMapping(".class", getTimestampFile().getPath()));
      } else {
        scanner.addSourceMapping(new SuffixMapping(".class", ".dummy"));
      }

      final Set classes = scanner.getIncludedSources(getClassDirectory(), getTimestampDirectory());

      if (!classes.isEmpty()) {
        final Set files = new HashSet();
        for (final Object aClass : classes) {
          final String file = ((File) aClass).getPath();
          final JavaClass clazz = NarUtil.getBcelClass(file);
          final Method[] method = clazz.getMethods();
          for (final Method element : method) {
            if (element.isNative()) {
              files.add(clazz.getClassName());
            }
          }
        }

        if (!files.isEmpty()) {
          getJniDirectory().mkdirs();
          getTimestampDirectory().mkdirs();

          final String javah = getJavah();

          this.mojo.getLog().info("Running " + javah + " compiler on " + files.size() + " classes...");
          final int result = NarUtil.runCommand(javah, generateArgs(files), null, null, this.mojo.getLog());
          if (result != 0) {
            throw new MojoFailureException(
                javah + " failed with exit code " + result + " 0x" + Integer.toHexString(result));
          }
          FileUtils.fileWrite(getTimestampDirectory() + "/" + getTimestampFile(), "");
        }
      }
    } catch (final InclusionScanException e) {
      throw new MojoExecutionException("JAVAH: Class scanning failed", e);
    } catch (final IOException e) {
      throw new MojoExecutionException("JAVAH: IO Exception", e);
    } catch (final ClassFormatException e) {
      throw new MojoExecutionException("JAVAH: Class could not be inspected", e);
    }
  }

  private String[] generateArgs(final Set/* <String> */ classes) throws MojoExecutionException {

    final List args = new ArrayList();

    if (!this.bootClassPaths.isEmpty()) {
      args.add("-bootclasspath");
      args.add(StringUtils.join(this.bootClassPaths.iterator(), File.pathSeparator));
    }

    args.add("-classpath");
    args.add(StringUtils.join(getClassPaths().iterator(), File.pathSeparator));

    args.add("-d");
    args.add(getJniDirectory().getPath());

    if (this.mojo.getLog().isDebugEnabled()) {
      args.add("-verbose");
    }

    if (classes != null) {
      for (final Object aClass : classes) {
        args.add(aClass);
      }
    }

    if (this.extraClasses != null) {
      for (final Object extraClass : this.extraClasses) {
        args.add(extraClass);
      }
    }

    return (String[]) args.toArray(new String[args.size()]);
  }

  protected final File getClassDirectory() {
    if (this.classDirectory == null) {
      this.classDirectory = new File(this.mojo.getMavenProject().getBuild().getDirectory(), "classes");
    }
    return this.classDirectory;
  }

  protected final List getClassPaths() throws MojoExecutionException {
    if (this.classPaths.isEmpty()) {
      try {
        this.classPaths.addAll(this.mojo.getMavenProject().getCompileClasspathElements());
      } catch (final DependencyResolutionRequiredException e) {
        throw new MojoExecutionException("JAVAH, cannot get classpath", e);
      }
    }
    return this.classPaths;
  }

  protected final Set getIncludes() {
    NarUtil.removeNulls(this.includes);
    if (this.includes.isEmpty()) {
      this.includes.add("**/*.class");
    }
    return this.includes;
  }

  private static List<File> asFiles(List values) {
    List<File> files = new ArrayList<File>();
    for (Object value : values) {
      files.add(new File(value.toString()));
    }
    return files;
  }

  private String findTool(Toolchain toolchain, String tool) throws MojoExecutionException, MojoFailureException {
    if (toolchain != null) {
      return toolchain.findTool(tool);
    }
    File executable = javaHomeTool(tool);
    return executable.isFile() ? executable.getAbsolutePath() : null;
  }

  private File javaHomeTool(String tool) throws MojoExecutionException, MojoFailureException {
    File executable = new File(tool);
    if (executable.isAbsolute()) {
      return executable;
    }
    File bin = new File(this.mojo.getJavaHome(this.mojo.getAOL()), "bin");
    executable = new File(bin, tool);
    if (!executable.isFile() && File.separatorChar == '\\' && !tool.endsWith(".exe")) {
      executable = new File(bin, tool + ".exe");
    }
    return executable;
  }

  private String getJavah() throws MojoExecutionException, MojoFailureException {
    // An explicit custom command is always legacy and is never retried with javac.
    if (!"javah".equals(name)) {
      return javaHomeTool(name).getAbsolutePath();
    }
    String command = findTool(getToolchain(), "javah");
    if (command == null) {
      throw new MojoExecutionException("Cannot find javah in the selected JDK");
    }
    return command;
  }

  protected final File getJniDirectory() {
    if (this.jniDirectory == null) {
      this.jniDirectory = new File(this.mojo.getMavenProject().getBuild().getDirectory(), "nar/javah-include");
    }
    return this.jniDirectory;
  }

  protected final File getTimestampDirectory() {
    if (this.timestampDirectory == null) {
      this.timestampDirectory = getJniDirectory();
    }
    return this.timestampDirectory;
  }

  protected final File getTimestampFile() {
    if (this.timestampFile == null) {
      this.timestampFile = new File(new File(this.name).getName());
    }
    return this.timestampFile;
  }

  // TODO remove the part with ToolchainManager lookup once we depend on
  // 2.0.9 (have it as prerequisite). Define as regular component field then.
  private Toolchain getToolchain() {
    Toolchain toolChain = null;
    final ToolchainManager toolchainManager = ((NarJavahMojo) this.mojo).getToolchainManager();

    if (toolchainManager != null) {
      toolChain = toolchainManager.getToolchainFromBuildContext("jdk", ((NarJavahMojo) this.mojo).getSession());
    }
    return toolChain;
  }

  public final void setAbstractCompileMojo(final AbstractNarMojo abstractNarMojo) {
    this.mojo = abstractNarMojo;
  }
}
