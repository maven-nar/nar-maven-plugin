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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.CompilerEnum;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.types.CompilerArgument;
import com.github.maven_nar.cpptasks.types.ConditionalFileSet;
import com.github.maven_nar.cpptasks.types.DefineArgument;
import com.github.maven_nar.cpptasks.types.DefineSet;

/**
 * Abstract Compiler class
 *
 * @author Mark Donszelmann
 */
public abstract class Compiler {

  public static final String MAIN = "main";

  public static final String TEST = "test";

  /**
   * The name of the compiler. Some choices are: "msvc", "g++", "gcc", "CC",
   * "cc", "icc", "icpc", ... Default is
   * Architecture-OS-Linker specific: FIXME: table missing
   */
  @Parameter
  private String name;

  /**
   * The prefix for the compiler.
   */
  @Parameter
  private String prefix;

  /**
   * Path location of the compile tool
   */
  @Parameter
  private String toolPath;

  /**
   * Source directory for native files
   */
  @Parameter(defaultValue = "${basedir}/src/main", required = true)
  private File sourceDirectory;

  /**
   * Source directory for native test files
   */
  @Parameter(defaultValue = "${basedir}/src/test", required = true)
  private File testSourceDirectory;

  /**
   * To use full path for the filenames.
   * false to have "relative" path
   * true to have "absolute" path
   * absolute: will give path from filesystem root "/"
   * relative: will give relative path from "workdir" which is usually after "${basedir}/src/main"
   */
  @Parameter(required = true)
  private boolean gccFileAbsolutePath = false;

  /**
   * Include patterns for sources
   */
  @Parameter(required = true)
  private Set<String> includes = new HashSet<>();

  /**
   * Exclude patterns for sources
   */
  @Parameter(required = true)
  private Set<String> excludes = new HashSet<>();

  /**
   * Include patterns for test sources
   */
  @Parameter(required = true)
  private Set<String> testIncludes = new HashSet<>();

  /**
   * Exclude patterns for test sources
   */
  @Parameter(required = true)
  private Set<String> testExcludes = new HashSet<>();

  @Parameter(defaultValue = "false", required = false)
  private boolean ccache = false;

  /**
   * Compile with debug information.
   */
  @Parameter(required = true)
  private boolean debug = false;

  /**
   * Enables generation of exception handling code.
   */
  @Parameter(defaultValue = "true", required = true)
  private boolean exceptions = true;

  /**
   * Enables run-time type information.
   */
  @Parameter(defaultValue = "true", required = true)
  private boolean rtti = true;

  /**
   * Sets optimization. Possible choices are: "none", "size", "minimal",
   * "speed", "full", "aggressive", "extreme",
   * "unsafe".
   */
  @Parameter(defaultValue = "none", required = true)
  private String optimize = "none";

  /**
   * Enables or disables generation of multi-threaded code. Default value:
   * false, except on Windows.
   */
  @Parameter(required = true)
  private boolean multiThreaded = false;

  /**
   * Defines
   */
  @Parameter
  private List<String> defines;

  /**
   * Defines for the compiler as a comma separated list of name[=value] pairs,
   * where the value is optional. Will work
   * in combination with &lt;defines&gt;.
   */
  @Parameter
  private String defineSet;

  /**
   * Clears default defines
   */
  @Parameter(required = true)
  private boolean clearDefaultDefines;

  /**
   * Undefines
   */
  @Parameter
  private List<String> undefines;

  /**
   * Undefines for the compiler as a comma separated list of name[=value] pairs
   * where the value is optional. Will work
   * in combination with &lt;undefines&gt;.
   */
  @Parameter
  private String undefineSet;

  /**
   * Clears default undefines
   */
  @Parameter
  private boolean clearDefaultUndefines;

  /**
   * Include Paths. Defaults to "${sourceDirectory}/include"
   */
  @Parameter
  private List<IncludePath> includePaths;

  /**
   * Test Include Paths. Defaults to "${testSourceDirectory}/include"
   */
  @Parameter
  private List<IncludePath> testIncludePaths;

  /**
   * System Include Paths, which are added at the end of all include paths
   */
  @Parameter
  private List<String> systemIncludePaths;

  /**
   * Additional options for the C++ compiler Defaults to Architecture-OS-Linker
   * specific values. FIXME table missing
   */
  @Parameter
  private List<String> options;

  /**
   * Additional options for the compiler when running in the nar-testCompile
   * phase.
   */
  @Parameter
  private List<String> testOptions;

  /**
   * Options for the compiler as a whitespace separated list. Will work in
   * combination with &lt;options&gt;.
   */
  @Parameter
  private String optionSet;

  /**
   * Clears default options
   */
  @Parameter(required = true)
  private boolean clearDefaultOptions;

  /**
   * Comma separated list of filenames to compile in order
   */
  @Parameter
  private String compileOrder;
  private AbstractCompileMojo mojo;

  protected Compiler() {
  }

  /**
   * Filter elements such as cr\lf that are problematic when used inside a `define`
   *  
   * @param value  define value to be cleaned
   * @return
   */
  private String cleanDefineValue(final String value) {
    return value.replaceAll("\r", "").replaceAll("\n", ""); // ?maybe replace with chars \\n
  }
  
  public final void copyIncludeFiles(final MavenProject mavenProject, final File targetDirectory) throws IOException {
    for (final IncludePath includePath : getIncludePaths("dummy")) {
      if (includePath.exists()) {
        NarUtil.copyDirectoryStructure(includePath.getFile(), targetDirectory, includePath.getIncludes(),
            NarUtil.DEFAULT_EXCLUDES);
      }
    }
  }

  /**
   * Generates a new {@link CompilerDef} and populates it give the parameters
   * provided.
   * 
   * @param type
   *          - main or test library - used to determine include and exclude
   *          paths.
   * @param output
   *          - TODO Not sure..
   * @return {@link CompilerDef} which contains the configuration for this
   *         compiler given the type and output.
   * @throws MojoFailureException
   *           TODO
   * @throws MojoExecutionException
   *           TODO
   */
  public final CompilerDef getCompiler(final String type, final String output)
      throws MojoFailureException, MojoExecutionException {
    final String name = getName();
    if (name == null) {
      return null;
    }

    final CompilerDef compilerDef = new CompilerDef();
    compilerDef.setProject(this.mojo.getAntProject());
    final CompilerEnum compilerName = new CompilerEnum();
    compilerName.setValue(name);
    compilerDef.setName(compilerName);

    // tool path
    if (this.toolPath != null) {
      compilerDef.setToolPath(this.toolPath);
    } else if (Msvc.isMSVC(mojo)) {
      mojo.getMsvc().setToolPath(compilerDef,getLanguage());
    }

    // debug, exceptions, rtti, multiThreaded
    compilerDef.setCompilerPrefix(this.prefix);
    compilerDef.setCcache(this.ccache);
    compilerDef.setDebug(this.debug);
    compilerDef.setExceptions(this.exceptions);
    compilerDef.setRtti(this.rtti);
    compilerDef.setMultithreaded(this.mojo.getOS().equals("Windows") || this.multiThreaded);

    // optimize
    final OptimizationEnum optimization = new OptimizationEnum();
    optimization.setValue(this.optimize);
    compilerDef.setOptimize(optimization);

    // add options
    if (this.options != null) {
      for (final String string : this.options) {
        final CompilerArgument arg = new CompilerArgument();
        arg.setValue(string);
        compilerDef.addConfiguredCompilerArg(arg);
      }
    }

    if (this.optionSet != null) {

      final String[] opts = this.optionSet.split("\\s");

      for (final String opt : opts) {

        final CompilerArgument arg = new CompilerArgument();

        arg.setValue(opt);
        compilerDef.addConfiguredCompilerArg(arg);
      }
    }

    compilerDef.setClearDefaultOptions(this.clearDefaultOptions);
    if (!this.clearDefaultOptions) {
      final String optionsProperty = NarProperties.getInstance(this.mojo.getMavenProject()).getProperty(
          getPrefix() + "options");
      if (optionsProperty != null) {
        final String[] option = optionsProperty.split(" ");
        for (final String element : option) {
          final CompilerArgument arg = new CompilerArgument();
          arg.setValue(element);
          compilerDef.addConfiguredCompilerArg(arg);
        }
      }
    }

    // add defines
    if (this.defines != null) {
      final DefineSet ds = new DefineSet();
      for (final String string : this.defines) {
        final DefineArgument define = new DefineArgument();
        final String[] pair = string.split("=", 2);
        define.setName(pair[0]);
        define.setValue(pair.length > 1 ? cleanDefineValue(pair[1]) : null);
        ds.addDefine(define);
      }
      compilerDef.addConfiguredDefineset(ds);
    }

    if (this.defineSet != null) {

      final String[] defList = this.defineSet.split(",");
      final DefineSet defSet = new DefineSet();

      for (final String element : defList) {

        final String[] pair = element.trim().split("=", 2);
        final DefineArgument def = new DefineArgument();

        def.setName(pair[0]);
        def.setValue(pair.length > 1 ? cleanDefineValue(pair[1]) : null);

        defSet.addDefine(def);
      }

      compilerDef.addConfiguredDefineset(defSet);
    }

    if (!this.clearDefaultDefines) {
      final DefineSet ds = new DefineSet();
      final String defaultDefines = NarProperties.getInstance(this.mojo.getMavenProject()).getProperty(
          getPrefix() + "defines");
      if (defaultDefines != null) {
        ds.setDefine(new CUtil.StringArrayBuilder(defaultDefines));
      }
      compilerDef.addConfiguredDefineset(ds);
    }

    // add undefines
    if (this.undefines != null) {
      final DefineSet us = new DefineSet();
      for (final String string : this.undefines) {
        final DefineArgument undefine = new DefineArgument();
        final String[] pair = string.split("=", 2);
        undefine.setName(pair[0]);
        undefine.setValue(pair.length > 1 ? pair[1] : null);
        us.addUndefine(undefine);
      }
      compilerDef.addConfiguredDefineset(us);
    }

    if (this.undefineSet != null) {

      final String[] undefList = this.undefineSet.split(",");
      final DefineSet undefSet = new DefineSet();

      for (final String element : undefList) {

        final String[] pair = element.trim().split("=", 2);
        final DefineArgument undef = new DefineArgument();

        undef.setName(pair[0]);
        undef.setValue(pair.length > 1 ? pair[1] : null);

        undefSet.addUndefine(undef);
      }

      compilerDef.addConfiguredDefineset(undefSet);
    }

    if (!this.clearDefaultUndefines) {
      final DefineSet us = new DefineSet();
      final String defaultUndefines = NarProperties.getInstance(this.mojo.getMavenProject()).getProperty(
          getPrefix() + "undefines");
      if (defaultUndefines != null) {
        us.setUndefine(new CUtil.StringArrayBuilder(defaultUndefines));
      }
      compilerDef.addConfiguredDefineset(us);
    }

    // add include path
    for (final IncludePath includePath : getIncludePaths(type)) {
      // Darren Sargent, 30Jan2008 - fail build if invalid include path(s)
      // specified.
      if (!includePath.exists()) {
        throw new MojoFailureException("NAR: Include path not found: " + includePath);
      }
      compilerDef.createIncludePath().setPath(includePath.getPath());
    }

    // add system include path (at the end)
    if (this.systemIncludePaths != null) {
      for (final String path : this.systemIncludePaths) {
        compilerDef.createSysIncludePath().setPath(path);
      }
    }

    // Add default fileset (if exists)
    final List<File> srcDirs = getSourceDirectories(type);
    final Set<String> includeSet = getIncludes(type);
    final Set<String> excludeSet = getExcludes(type);

    // now add all but the current test to the excludes
    for (final Object o : this.mojo.getTests()) {
      final Test test = (Test) o;
      if (!test.getName().equals(output)) {
        excludeSet.add("**/" + test.getName() + ".*");
      }
    }

    for (final File srcDir : srcDirs) {
      this.mojo.getLog().debug("Checking for existence of " + getLanguage() + " source directory: " + srcDir);
      if (srcDir.exists()) {
        if (this.compileOrder != null) {
          compilerDef.setOrder(Arrays.asList(StringUtils.split(this.compileOrder, ", ")));
        }

        final ConditionalFileSet fileSet = new ConditionalFileSet();
        fileSet.setProject(this.mojo.getAntProject());
        fileSet.setIncludes(StringUtils.join(includeSet.iterator(), ","));
        fileSet.setExcludes(StringUtils.join(excludeSet.iterator(), ","));
        fileSet.setDir(srcDir);
        compilerDef.addFileset(fileSet);
      }
    }
    
    if (type.equals(TEST)) {
      if (this.testSourceDirectory.exists()) {
        compilerDef.setWorkDir(this.testSourceDirectory);
      }
    } else {
      if (this.sourceDirectory.exists()) {
        compilerDef.setWorkDir(this.sourceDirectory);
      }
    }

    compilerDef.setGccFileAbsolutePath(this.gccFileAbsolutePath);

    return compilerDef;
  }

  public final Set<String> getExcludes() throws MojoFailureException, MojoExecutionException {
    return getExcludes("main");
  }

  protected final Set<String> getExcludes(final String type) throws MojoFailureException, MojoExecutionException {
    final Set<String> result = new HashSet<>();
    if (type.equals(TEST) && !this.testExcludes.isEmpty()) {
      result.addAll(this.testExcludes);
    } else if (!this.excludes.isEmpty()) {
      result.addAll(this.excludes);
    } else {
      final String defaultExcludes = NarProperties.getInstance(this.mojo.getMavenProject()).getProperty(
          getPrefix() + "excludes");
      if (defaultExcludes != null) {
        final String[] exclude = defaultExcludes.split(" ");
        for (final String element : exclude) {
          result.add(element.trim());
        }
      }
    }

    return result;
  }

  protected final List<IncludePath> getIncludePaths(final String type) {
    List<IncludePath> includeList = type.equals(TEST) ? this.testIncludePaths : this.includePaths;

    if (includeList != null && includeList.size() != 0) {
      return includeList;
    }

    includeList = new ArrayList<>();
    for (final File file2 : getSourceDirectories(type)) {
      // VR 20100318 only add include directories that exist - we now fail the
      // build fast if an include directory does not exist
      final File file = new File(file2, "include");
      if (file.isDirectory()) {
        final IncludePath includePath = new IncludePath();
        includePath.setPath(file.getPath());
        includeList.add(includePath);
      }
    }
    return includeList;
  }

  public final Set<String> getIncludes() throws MojoFailureException, MojoExecutionException {
    return getIncludes("main");
  }

  protected final Set<String> getIncludes(final String type) throws MojoFailureException, MojoExecutionException {
    final Set<String> result = new HashSet<>();
    if (!type.equals(TEST) && !this.includes.isEmpty()) {
      result.addAll(this.includes);
    } else if (type.equals(TEST) && !this.testIncludes.isEmpty()) {
      result.addAll(this.testIncludes);
    } else {
      final String defaultIncludes = NarProperties.getInstance(this.mojo.getMavenProject()).getProperty(
          getPrefix() + "includes");
      if (defaultIncludes != null) {
        final String[] include = defaultIncludes.split(" ");
        for (final String element : include) {
          result.add(element.trim());
        }
      }
    }
    return result;
  }

  protected abstract String getLanguage();

  public String getName() throws MojoFailureException, MojoExecutionException {
    // adjust default values
    if (this.name == null) {
      this.name = NarProperties.getInstance(this.mojo.getMavenProject()).getProperty(getPrefix() + "compiler");
    }
    if (this.prefix == null) {
      this.prefix = NarProperties.getInstance(this.mojo.getMavenProject()).getProperty(getPrefix() + "prefix");
    }
    return this.name;
  }

  protected final String getPrefix() throws MojoFailureException, MojoExecutionException {
    return this.mojo.getAOL().getKey() + "." + getLanguage() + ".";
  }

  public final List<File> getSourceDirectories() {
    return getSourceDirectories("dummy");
  }

  private List<File> getSourceDirectories(final String type) {
    final List<File> sourceDirectories = new ArrayList<>();
    final File baseDir = this.mojo.getMavenProject().getBasedir();

    if (type.equals(TEST)) {
      if (this.testSourceDirectory == null) {
        this.testSourceDirectory = new File(baseDir, "/src/test");
      }
      if (this.testSourceDirectory.exists()) {
        sourceDirectories.add(this.testSourceDirectory);
      }

      for (final Object element : this.mojo.getMavenProject().getTestCompileSourceRoots()) {
        final File extraTestSourceDirectory = new File((String) element);
        if (extraTestSourceDirectory.exists()) {
          sourceDirectories.add(extraTestSourceDirectory);
        }
      }
    } else {
      if (this.sourceDirectory == null) {
        this.sourceDirectory = new File(baseDir, "src/main");
      }
      if (this.sourceDirectory.exists()) {
        sourceDirectories.add(this.sourceDirectory);
      }

      for (final Object element : this.mojo.getMavenProject().getCompileSourceRoots()) {
        final File extraSourceDirectory = new File((String) element);
        if (extraSourceDirectory.exists()) {
          sourceDirectories.add(extraSourceDirectory);
        }
      }
    }

    if (this.mojo.getLog().isDebugEnabled()) {
      for (final File file : sourceDirectories) {
        this.mojo.getLog().debug("Added to sourceDirectory: " + file.getPath());
      }
    }
    return sourceDirectories;
  }

  /**
   * @return The standard Compiler configuration with 'testOptions' added to the
   *         argument list.
   */
  public final CompilerDef getTestCompiler(final String type, final String output)
      throws MojoFailureException, MojoExecutionException {
    final CompilerDef compiler = getCompiler(type, output);
    if (compiler != null && this.testOptions != null) {
      for (final String string : this.testOptions) {
        final CompilerArgument arg = new CompilerArgument();
        arg.setValue(string);
        compiler.addConfiguredCompilerArg(arg);
      }
    }
    return compiler;
  }

  public final void setAbstractCompileMojo(final AbstractCompileMojo mojo) {
    this.mojo = mojo;
  }

  @Override
  public String toString() {
    return NarUtil.prettyMavenString(this);
  }
}
