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

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.Compiler;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.gcc.GccCCompiler;
import com.github.maven_nar.cpptasks.types.CompilerArgument;
import com.github.maven_nar.cpptasks.types.ConditionalPath;
import com.github.maven_nar.cpptasks.types.DefineSet;
import com.github.maven_nar.cpptasks.types.IncludePath;
import com.github.maven_nar.cpptasks.types.SystemIncludePath;
import com.github.maven_nar.cpptasks.types.UndefineArgument;
import java.io.File;

/**
 * A compiler definition. compiler elements may be placed either as children of
 * a cc element or the project element. A compiler element with an id attribute
 * may be referenced from compiler elements with refid or extends attributes.
 *
 * @author Adam Murdoch
 */
public final class CompilerDef extends ProcessorDef {
  /** The source file sets. */
  private final Vector defineSets = new Vector();
  private Boolean ccache = false;
  private Boolean exceptions;
  private Boolean rtti;
  private final Vector includePaths = new Vector();
  private Boolean multithreaded;
  private final Vector precompileDefs = new Vector();
  private final Vector sysIncludePaths = new Vector();
  private OptimizationEnum optimization;
  private int warnings = -1;
  private List<String> order;
  private String toolPath;
  private String compilerPrefix;
  private File workDir;
  private boolean gccFileAbsolutePath;
  private String fortifyID="";

  private boolean clearDefaultOptions;

  public CompilerDef() {
  }

  /**
   * Adds a compiler command-line arg.
   */
  public void addConfiguredCompilerArg(final CompilerArgument arg) {
    if (isReference()) {
      throw noChildrenAllowed();
    }
    addConfiguredProcessorArg(arg);
  }

  /**
   * Adds a compiler command-line arg.
   */
  public void addConfiguredCompilerParam(final CompilerParam param) {
    if (isReference()) {
      throw noChildrenAllowed();
    }
    addConfiguredProcessorParam(param);
  }

  /**
   * Adds a defineset.
   */
  public void addConfiguredDefineset(final DefineSet defs) {
    if (defs == null) {
      throw new NullPointerException("defs");
    }
    if (isReference()) {
      throw noChildrenAllowed();
    }
    this.defineSets.addElement(defs);
  }

  /**
   * Creates an include path.
   */
  public IncludePath createIncludePath() {
    final Project p = getProject();
    if (p == null) {
      throw new java.lang.IllegalStateException("project must be set");
    }
    if (isReference()) {
      throw noChildrenAllowed();
    }
    final IncludePath path = new IncludePath(p);
    this.includePaths.addElement(path);
    return path;
  }

  /**
   * Specifies precompilation prototype file and exclusions.
   * 
   */
  public PrecompileDef createPrecompile() throws BuildException {
    final Project p = getProject();
    if (isReference()) {
      throw noChildrenAllowed();
    }
    final PrecompileDef precomp = new PrecompileDef();
    precomp.setProject(p);
    this.precompileDefs.addElement(precomp);
    return precomp;
  }

  /**
   * Creates a system include path. Locations and timestamps of files located
   * using the system include paths are not used in dependency analysis.
   * 
   * 
   * Standard include locations should not be specified. The compiler
   * adapters should recognized the settings from the appropriate environment
   * variables or configuration files.
   */
  public SystemIncludePath createSysIncludePath() {
    final Project p = getProject();
    if (p == null) {
      throw new java.lang.IllegalStateException("project must be set");
    }
    if (isReference()) {
      throw noChildrenAllowed();
    }
    final SystemIncludePath path = new SystemIncludePath(p);
    this.sysIncludePaths.addElement(path);
    return path;
  }

  public void execute() throws org.apache.tools.ant.BuildException {
    throw new org.apache.tools.ant.BuildException("Not an actual task, but looks like one for documentation purposes");
  }

  public UndefineArgument[] getActiveDefines() {
    final Project p = getProject();
    if (p == null) {
      throw new java.lang.IllegalStateException("project must be set before this call");
    }
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getActiveDefines();
    }
    final Vector actives = new Vector();
    for (int i = 0; i < this.defineSets.size(); i++) {
      final DefineSet currentSet = (DefineSet) this.defineSets.elementAt(i);
      final UndefineArgument[] defines = currentSet.getDefines();
      for (final UndefineArgument define : defines) {
        if (define.isActive(p)) {
          actives.addElement(define);
        }
      }
    }
    final UndefineArgument[] retval = new UndefineArgument[actives.size()];
    actives.copyInto(retval);
    return retval;
  }

  /**
   * Returns the compiler-specific include path.
   */
  public String[] getActiveIncludePaths() {
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getActiveIncludePaths();
    }
    return getActivePaths(this.includePaths);
  }

  private String[] getActivePaths(final Vector paths) {
    final Project p = getProject();
    if (p == null) {
      throw new java.lang.IllegalStateException("project not set");
    }
    final Vector activePaths = new Vector(paths.size());
    for (int i = 0; i < paths.size(); i++) {
      final ConditionalPath path = (ConditionalPath) paths.elementAt(i);
      if (path.isActive(p)) {
        final String[] pathEntries = path.list();
        for (final String pathEntrie : pathEntries) {
          activePaths.addElement(pathEntrie);
        }
      }
    }
    final String[] pathNames = new String[activePaths.size()];
    activePaths.copyInto(pathNames);
    return pathNames;
  }

  public PrecompileDef getActivePrecompile(final CompilerDef ccElement) {
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getActivePrecompile(ccElement);
    }
    PrecompileDef current = null;
    final Enumeration iter = this.precompileDefs.elements();
    while (iter.hasMoreElements()) {
      current = (PrecompileDef) iter.nextElement();
      if (current.isActive()) {
        return current;
      }
    }
    final CompilerDef extendedDef = (CompilerDef) getExtends();
    if (extendedDef != null) {
      current = extendedDef.getActivePrecompile(null);
      if (current != null) {
        return current;
      }
    }
    if (ccElement != null && getInherit()) {
      return ccElement.getActivePrecompile(null);
    }
    return null;
  }

  public String[] getActiveSysIncludePaths() {
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getActiveSysIncludePaths();
    }
    return getActivePaths(this.sysIncludePaths);
  }

  public Boolean getCcache() {
    return this.ccache;
  }

  public final boolean getExceptions(final CompilerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getExceptions(defaultProviders, index);
    }
    if (this.exceptions != null) {
      return this.exceptions.booleanValue();
    } else {
      if (defaultProviders != null && index < defaultProviders.length) {
        return defaultProviders[index].getExceptions(defaultProviders, index + 1);
      }
    }
    return false;
  }

  public boolean getMultithreaded(final CompilerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getMultithreaded(defaultProviders, index);
    }
    if (this.multithreaded != null) {
      return this.multithreaded.booleanValue();
    } else {
      if (defaultProviders != null && index < defaultProviders.length) {
        return defaultProviders[index].getMultithreaded(defaultProviders, index + 1);
      }
    }
    return true;
  }

  public final OptimizationEnum getOptimization(final CompilerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getOptimization(defaultProviders, index);
    }
    if (this.optimization != null) {
      return this.optimization;
    } else {
      if (defaultProviders != null && index < defaultProviders.length) {
        return defaultProviders[index].getOptimization(defaultProviders, index + 1);
      }
    }
    return null;
  }

  public List<String> getOrder() {
    return this.order;
  }

  @Override
  public Processor getProcessor() {
    Processor processor = super.getProcessor();
    if (processor == null) {
      processor = GccCCompiler.getInstance();
    }
    if (getLibtool() && processor instanceof CommandLineCompiler) {
      final CommandLineCompiler compiler = (CommandLineCompiler) processor;
      processor = compiler.getLibtoolCompiler();
    }
    return processor;
  }

  public final Boolean getRtti(final CompilerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getRtti(defaultProviders, index);
    }
    if (this.rtti != null) {
      return this.rtti;
    } else {
      if (defaultProviders != null && index < defaultProviders.length) {
        return defaultProviders[index].getRtti(defaultProviders, index + 1);
      }
    }
    return null;
  }

  public String getToolPath() {
    return this.toolPath;
  }

  public String getCompilerPrefix() {
    return this.compilerPrefix;
  }

  public File getWorkDir() {
      return this.workDir;
  }
  
  public int getWarnings(final CompilerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((CompilerDef) getCheckedRef(CompilerDef.class, "CompilerDef")).getWarnings(defaultProviders, index);
    }
    if (this.warnings == -1 && defaultProviders != null && index < defaultProviders.length) {
      return defaultProviders[index].getWarnings(defaultProviders, index + 1);
    }
    return this.warnings;
  }

  public boolean isClearDefaultOptions() {
    return this.clearDefaultOptions;
  }

  public void setCcache(final Boolean ccache) {
    this.ccache = ccache;
  }

  /**
   * Sets the default compiler adapter. Use the "name" attribute when the
   * compiler is a supported compiler.
   * 
   * @param classname
   *          fully qualified classname which implements CompilerAdapter
   */
  @Override
  public void setClassname(final String classname) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    super.setClassname(classname);
    final Processor proc = getProcessor();
    if (!(proc instanceof Compiler)) {
      throw new BuildException(classname + " does not implement Compiler");
    }
  }

  public void setClearDefaultOptions(final boolean clearDefaultOptions) {
    this.clearDefaultOptions = clearDefaultOptions;
  }

  /**
   * Enables or disables exception support.
   * 
   * @param exceptions
   *          if true, exceptions are supported.
   * 
   */
  public void setExceptions(final boolean exceptions) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.exceptions = booleanValueOf(exceptions);
  }

  /**
   * Enables or disables generation of multithreaded code. Unless specified,
   * multithreaded code generation is enabled.
   * 
   * @param multithreaded
   *          If true, generated code may be multithreaded.
   */
  public void setMultithreaded(final boolean multithreaded) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.multithreaded = booleanValueOf(multithreaded);
  }

  /**
   * Sets compiler type.
   * 
   * 
   * <table width="100%" border="1">
   * <thead>Supported compilers </thead>
   * <tr>
   * <td>gcc (default)</td>
   * <td>GCC C++ compiler</td>
   * </tr>
   * <tr>
   * <td>g++</td>
   * <td>GCC C++ compiler</td>
   * </tr>
   * <tr>
   * <td>c++</td>
   * <td>GCC C++ compiler</td>
   * </tr>
   * <tr>
   * <td>g77</td>
   * <td>GNU Fortran compiler</td>
   * </tr>
   * <tr>
   * <td>msvc</td>
   * <td>Microsoft Visual C++</td>
   * </tr>
   * <tr>
   * <td>bcc</td>
   * <td>Borland C++ Compiler</td>
   * </tr>
   * <tr>
   * <td>msrc</td>
   * <td>Microsoft Resource Compiler</td>
   * </tr>
   * <tr>
   * <td>brc</td>
   * <td>Borland Resource Compiler</td>
   * </tr>
   * <tr>
   * <td>df</td>
   * <td>Compaq Visual Fortran Compiler</td>
   * </tr>
   * <tr>
   * <td>midl</td>
   * <td>Microsoft MIDL Compiler</td>
   * </tr>
   * <tr>
   * <td>icl</td>
   * <td>Intel C++ compiler for Windows (IA-32)</td>
   * </tr>
   * <tr>
   * <td>ecl</td>
   * <td>Intel C++ compiler for Windows (IA-64)</td>
   * </tr>
   * <tr>
   * <td>icc</td>
   * <td>Intel C++ compiler for Linux (IA-32)</td>
   * </tr>
   * <tr>
   * <td>ecc</td>
   * <td>Intel C++ compiler for Linux (IA-64)</td>
   * </tr>
   * <tr>
   * <td>CC</td>
   * <td>Sun ONE C++ compiler</td>
   * </tr>
   * <tr>
   * <td>aCC</td>
   * <td>HP aC++ C++ Compiler</td>
   * </tr>
   * <tr>
   * <td>os390</td>
   * <td>OS390 C Compiler</td>
   * </tr>
   * <tr>
   * <td>os400</td>
   * <td>Icc Compiler</td>
   * </tr>
   * <tr>
   * <td>sunc89</td>
   * <td>Sun C89 C Compiler</td>
   * </tr>
   * <tr>
   * <td>xlC</td>
   * <td>VisualAge C Compiler</td>
   * </tr>
   * <tr>
   * <td>uic</td>
   * <td>Qt user interface compiler</td>
   * </tr>
   * <tr>
   * <td>moc</td>
   * <td>Qt meta-object compiler</td>
   * </tr>
   * <tr>
   * <td>wcl</td>
   * <td>OpenWatcom C/C++ compiler</td>
   * </tr>
   * <tr>
   * <td>wfl</td>
   * <td>OpenWatcom FORTRAN compiler</td>
   * </tr>
   * </table>
   * 
   */
  public void setName(final CompilerEnum name) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    final Compiler compiler = name.getCompiler();
    setProcessor(compiler);
  }

  /**
   * Sets optimization level.
   * 
   * @param value
   *          optimization level
   */
  public void setOptimize(final OptimizationEnum value) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.optimization = value;
  }

  // FREEHEP
  /**
   * List of source filenames without extensions
   * 
   * @param asList
   */
  public void setOrder(final List<String> order) {
    this.order = order;
  }

  @Override
  protected void setProcessor(final Processor proc) throws BuildException {
    try {
      super.setProcessor(proc);
    } catch (final ClassCastException ex) {
      throw new BuildException(ex);
    }
  }

  /**
   * Enables or disables run-time type information.
   *
   * @param rtti
   *          if true, run-time type information is supported.
   * 
   */
  public void setRtti(final boolean rtti) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.rtti = booleanValueOf(rtti);
  }

  public void setToolPath(final String path) {
    this.toolPath = path;
  }

  public void setCompilerPrefix(final String prefix) {
    this.compilerPrefix = prefix;
  }

  public void setWorkDir(final File workDir) {
      this.workDir = workDir;
  }

  public void setFortifyID(final String fortifyID) {
    this.fortifyID = fortifyID;
  }

  public String getFortifyID() {
    return this.fortifyID;
  }
  /**
   * Enumerated attribute with the values "none", "severe", "default",
   * "production", "diagnostic", and "aserror".
   */
  public void setWarnings(final WarningLevelEnum level) {
    this.warnings = level.getIndex();
  }

  public void setGccFileAbsolutePath(final boolean sourceFileAbsPath) {
    this.gccFileAbsolutePath = sourceFileAbsPath;
    return;
  }

  public boolean getGccFileAbsolutePath() {
    return this.gccFileAbsolutePath;
  }
}
