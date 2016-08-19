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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FlexInteger;

import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.gcc.GccLinker;
import com.github.maven_nar.cpptasks.types.FlexLong;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

/**
 * A linker definition. linker elements may be placed either as children of a
 * cc element or the project element. A linker element with an id attribute may
 * be referenced by linker elements with refid or extends attributes.
 *
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public class LinkerDef extends ProcessorDef {
  private long base;
  private String entry;
  private Boolean fixed;
  private Boolean incremental;
  private final Vector librarySets = new Vector();
  private Boolean map;
  private int stack;
  private final Vector sysLibrarySets = new Vector();
  private String toolPath;
  private String linkerPrefix;
  private Boolean skipDepLink;

  private final Set<File> libraryDirectories = new LinkedHashSet<>();

  /**
   * Default constructor
   * 
   * @see java.lang.Object#Object()
   */
  public LinkerDef() {
    this.base = -1;
    this.stack = -1;
  }

  private void addActiveLibrarySet(final Project project, final Vector libsets, final Vector srcSets) {
    final Enumeration srcenum = srcSets.elements();
    while (srcenum.hasMoreElements()) {
      final LibrarySet set = (LibrarySet) srcenum.nextElement();
      if (set.isActive(project)) {
        libsets.addElement(set);
      }
    }
  }

  private void addActiveSystemLibrarySets(final Project project, final Vector libsets) {
    addActiveLibrarySet(project, libsets, this.sysLibrarySets);
  }

  private void addActiveUserLibrarySets(final Project project, final Vector libsets) {
    addActiveLibrarySet(project, libsets, this.librarySets);
  }

  /**
   * Adds a linker command-line arg.
   */
  public void addConfiguredLinkerArg(final LinkerArgument arg) {
    addConfiguredProcessorArg(arg);
  }

  /**
   * Adds a compiler command-line arg.
   */
  public void addConfiguredLinkerParam(final LinkerParam param) {
    if (isReference()) {
      throw noChildrenAllowed();
    }
    addConfiguredProcessorParam(param);
  }

  public boolean addLibraryDirectory(final File directory) {
    if (directory == null || !directory.exists()) {
      return false;
    } else {
      return this.libraryDirectories.add(directory);
    }
  }

  public boolean addLibraryDirectory(final File parent, final String path) {
    if (parent == null) {
      return false;
    } else {
      final File directory = new File(parent, path);
      return addLibraryDirectory(directory);
    }
  }

  public void addLibraryDirectory(final String path) {
    final File directory = new File(path);
    addLibraryDirectory(directory);
  }

  /**
   * Adds a system library set.
   */
  public void addLibset(final LibrarySet libset) {
    if (isReference()) {
      throw super.noChildrenAllowed();
    }
    if (libset == null) {
      throw new NullPointerException("libset");
    }
    this.librarySets.addElement(libset);
  }

  /**
   * Adds a system library set.
   */
  public void addSyslibset(final SystemLibrarySet libset) {
    if (isReference()) {
      throw super.noChildrenAllowed();
    }
    if (libset == null) {
      throw new NullPointerException("libset");
    }
    this.sysLibrarySets.addElement(libset);
  }

  public void execute() throws org.apache.tools.ant.BuildException {
    throw new org.apache.tools.ant.BuildException("Not an actual task, but looks like one for documentation purposes");
  }

  /**
   * Returns an array of active library sets for this linker definition.
   */
  public LibrarySet[] getActiveLibrarySets(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef"))
          .getActiveUserLibrarySets(defaultProviders, index);
    }
    final Project p = getProject();
    final Vector libsets = new Vector();
    for (int i = index; i < defaultProviders.length; i++) {
      defaultProviders[i].addActiveUserLibrarySets(p, libsets);
    }
    addActiveUserLibrarySets(p, libsets);
    for (int i = index; i < defaultProviders.length; i++) {
      defaultProviders[i].addActiveSystemLibrarySets(p, libsets);
    }
    addActiveSystemLibrarySets(p, libsets);
    final LibrarySet[] sets = new LibrarySet[libsets.size()];
    libsets.copyInto(sets);
    return sets;
  }

  /**
   * Returns an array of active library sets for this linker definition.
   */
  public LibrarySet[] getActiveSystemLibrarySets(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef"))
          .getActiveUserLibrarySets(defaultProviders, index);
    }
    final Project p = getProject();
    final Vector libsets = new Vector();
    for (int i = index; i < defaultProviders.length; i++) {
      defaultProviders[i].addActiveSystemLibrarySets(p, libsets);
    }
    addActiveSystemLibrarySets(p, libsets);
    final LibrarySet[] sets = new LibrarySet[libsets.size()];
    libsets.copyInto(sets);
    return sets;
  }

  /**
   * Returns an array of active library sets for this linker definition.
   */
  public LibrarySet[] getActiveUserLibrarySets(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef"))
          .getActiveUserLibrarySets(defaultProviders, index);
    }
    final Project p = getProject();
    final Vector libsets = new Vector();
    for (int i = index; i < defaultProviders.length; i++) {
      defaultProviders[i].addActiveUserLibrarySets(p, libsets);
    }
    addActiveUserLibrarySets(p, libsets);
    final LibrarySet[] sets = new LibrarySet[libsets.size()];
    libsets.copyInto(sets);
    return sets;
  }

  public long getBase(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef")).getBase(defaultProviders, index);
    }
    if (this.base <= 0 && defaultProviders != null && index < defaultProviders.length) {
        return defaultProviders[index].getBase(defaultProviders, index + 1);
    }
    return this.base;
  }

  public String getEntry(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef")).getEntry(defaultProviders, index);
    }
    if (this.entry != null) {
      return this.entry;
    }
    if (defaultProviders != null && index < defaultProviders.length) {
      return defaultProviders[index].getEntry(defaultProviders, index + 1);
    }
    return null;
  }

  public Boolean getFixed(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef")).getFixed(defaultProviders, index);
    }
    if (this.fixed == null && defaultProviders != null && index < defaultProviders.length) {
        return defaultProviders[index].getFixed(defaultProviders, index + 1);
    }
    return this.fixed;
  }

  public boolean getIncremental(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef")).getIncremental(defaultProviders, index);
    }
    if (this.incremental != null) {
      return this.incremental.booleanValue();
    }
    if (defaultProviders != null && index < defaultProviders.length) {
      return defaultProviders[index].getIncremental(defaultProviders, index + 1);
    }
    return false;
  }

  public List<File> getLibraryDirectories() {
    return new ArrayList<>(this.libraryDirectories);
  }

  public boolean getMap(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef")).getMap(defaultProviders, index);
    }
    if (this.map != null) {
      return this.map.booleanValue();
    }
    if (defaultProviders != null && index < defaultProviders.length) {
      return defaultProviders[index].getMap(defaultProviders, index + 1);
    }
    return false;
  }

  @Override
  public Processor getProcessor() {
    Linker linker = (Linker) super.getProcessor();
    if (linker == null) {
      linker = GccLinker.getInstance();
    }
    if (getLibtool() && linker instanceof CommandLineLinker) {
      final CommandLineLinker cmdLineLinker = (CommandLineLinker) linker;
      linker = cmdLineLinker.getLibtoolLinker();
    }
    return linker;
  }

  @Override
  public Processor getProcessor(final LinkType linkType) {
    final Processor proc = getProcessor();
    return proc.getLinker(linkType);
  }

  public int getStack(final LinkerDef[] defaultProviders, final int index) {
    if (isReference()) {
      return ((LinkerDef) getCheckedRef(LinkerDef.class, "LinkerDef")).getStack(defaultProviders, index);
    }
    if (this.stack < 0 && defaultProviders != null && index < defaultProviders.length) {
        return defaultProviders[index].getStack(defaultProviders, index + 1);
    }
    return this.stack;
  }

  public String getToolPath() {
    return this.toolPath;
  }

  public String getLinkerPrefix() {
    return this.linkerPrefix;
  }
  
 public boolean isSkipDepLink() {
    return this.skipDepLink.booleanValue();
  }
  
  /**
   * Sets the base address. May be specified in either decimal or hex.
   * 
   * @param base
   *          base address
   * 
   */
  public void setBase(final FlexLong base) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.base = base.longValue();
  }

  /**
   * Sets the starting address.
   * 
   * @param entry
   *          function name
   */
  public void setEntry(final String entry) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.entry = entry;
  }

  /**
   * If true, marks the file to be loaded only at its preferred address.
   */
  public void setFixed(final boolean fixed) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.fixed = booleanValueOf(fixed);
  }

  /**
   * If true, allows incremental linking.
   * 
   */
  public void setIncremental(final boolean incremental) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.incremental = booleanValueOf(incremental);
  }

  /**
   * If set to true, a map file will be produced.
   */
  public void setMap(final boolean map) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.map = booleanValueOf(map);
  }

  /**
   * Sets linker type.
   * 
   * 
   * <table width="100%" border="1">
   * <thead>Supported linkers </thead>
   * <tr>
   * <td>gcc</td>
   * <td>Gcc Linker</td>
   * </tr>
   * <tr>
   * <td>g++</td>
   * <td>G++ Linker</td>
   * </tr>
   * <tr>
   * <td>ld</td>
   * <td>Ld Linker</td>
   * </tr>
   * <tr>
   * <td>ar</td>
   * <td>Gcc Librarian</td>
   * </tr>
   * <tr>
   * <td>msvc</td>
   * <td>Microsoft Linker</td>
   * </tr>
   * <tr>
   * <td>bcc</td>
   * <td>Borland Linker</td>
   * </tr>
   * <tr>
   * <td>df</td>
   * <td>Compaq Visual Fortran Linker</td>
   * </tr>
   * <tr>
   * <td>icl</td>
   * <td>Intel Linker for Windows (IA-32)</td>
   * </tr>
   * <tr>
   * <td>ecl</td>
   * <td>Intel Linker for Windows (IA-64)</td>
   * </tr>
   * <tr>
   * <td>icc</td>
   * <td>Intel Linker for Linux (IA-32)</td>
   * </tr>
   * <tr>
   * <td>ecc</td>
   * <td>Intel Linker for Linux (IA-64)</td>
   * </tr>
   * <tr>
   * <td>CC</td>
   * <td>Sun ONE Linker</td>
   * </tr>
   * <tr>
   * <td>aCC</td>
   * <td>HP aC++ Linker</td>
   * </tr>
   * <tr>
   * <td>os390</td>
   * <td>OS390 Linker</td>
   * </tr>
   * <tr>
   * <td>os390batch</td>
   * <td>OS390 Linker</td>
   * </tr>
   * <tr>
   * <td>os400</td>
   * <td>IccLinker</td>
   * </tr>
   * <tr>
   * <td>sunc89</td>
   * <td>C89 Linker</td>
   * </tr>
   * <tr>
   * <td>xlC</td>
   * <td>VisualAge Linker</td>
   * </tr>
   * <tr>
   * <td>wcl</td>
   * <td>OpenWatcom C/C++ linker</td>
   * </tr>
   * <tr>
   * <td>wfl</td>
   * <td>OpenWatcom FORTRAN linker</td>
   * </tr>
   * </table>
   * 
   */
  public void setName(final LinkerEnum name) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    final Linker linker = name.getLinker();
    super.setProcessor(linker);
  }

  @Override
  protected void setProcessor(final Processor proc) throws BuildException {
    Linker linker = null;
    if (proc instanceof Linker) {
      linker = (Linker) proc;
    } else {
      final LinkType linkType = new LinkType();
      linker = proc.getLinker(linkType);
    }
    super.setProcessor(linker);
  }

  /**
   * Sets stack size in bytes.
   */
  public void setStack(final FlexInteger stack) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.stack = stack.intValue();
  }

  public void setToolPath(final String path) {
    this.toolPath = path;
  }

  public void setLinkerPrefix(final String prefix) {
    this.linkerPrefix = prefix;
  }
  
  public void setSkipDepLink(final boolean skipDepLink) {
    this.skipDepLink = booleanValueOf(skipDepLink);
  }

  public void visitSystemLibraries(final Linker linker, final FileVisitor libraryVisitor) {
    final Project p = getProject();
    if (p == null) {
      throw new java.lang.IllegalStateException("project must be set");
    }
    if (isReference()) {
      final LinkerDef master = (LinkerDef) getCheckedRef(LinkerDef.class, "Linker");
      master.visitSystemLibraries(linker, libraryVisitor);
    } else {
      //
      // if this linker extends another,
      // visit its libraries first
      //
      final LinkerDef extendsDef = (LinkerDef) getExtends();
      if (extendsDef != null) {
        extendsDef.visitSystemLibraries(linker, libraryVisitor);
      }
      if (this.sysLibrarySets.size() > 0) {
        final File[] libpath = linker.getLibraryPath();
        for (int i = 0; i < this.sysLibrarySets.size(); i++) {
          final LibrarySet set = (LibrarySet) this.sysLibrarySets.elementAt(i);
          if (set.isActive(p)) {
            set.visitLibraries(p, linker, libpath, libraryVisitor);
          }
        }
      }
    }
  }

  public void visitUserLibraries(final Linker linker, final FileVisitor libraryVisitor) {
    final Project p = getProject();
    if (p == null) {
      throw new java.lang.IllegalStateException("project must be set");
    }
    if (isReference()) {
      final LinkerDef master = (LinkerDef) getCheckedRef(LinkerDef.class, "Linker");
      master.visitUserLibraries(linker, libraryVisitor);
    } else {
      //
      // if this linker extends another,
      // visit its libraries first
      //
      final LinkerDef extendsDef = (LinkerDef) getExtends();
      if (extendsDef != null) {
        extendsDef.visitUserLibraries(linker, libraryVisitor);
      }
      //
      // visit the user libraries
      //
      if (this.librarySets.size() > 0) {
        final File[] libpath = linker.getLibraryPath();
        for (int i = 0; i < this.librarySets.size(); i++) {
          final LibrarySet set = (LibrarySet) this.librarySets.elementAt(i);
          if (set.isActive(p)) {
            set.visitLibraries(p, linker, libpath, libraryVisitor);
          }
        }
      }
    }
  }
}
