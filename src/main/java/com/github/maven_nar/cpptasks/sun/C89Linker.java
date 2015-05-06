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
package com.github.maven_nar.cpptasks.sun;

import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * Adapter for the Sun C89 Linker
 *
 * @author Hiram Chirino (cojonudo14@hotmail.com)
 */
public final class C89Linker extends CommandLineLinker {
  private static final C89Linker dllLinker = new C89Linker("lib", ".so");
  private static final C89Linker instance = new C89Linker("", "");

  public static C89Linker getInstance() {
    return instance;
  }

  private final String outputPrefix;

  private C89Linker(final String outputPrefix, final String outputSuffix) {
    super("ld", "/bogus", new String[] {
        ".o", ".a", ".lib", ".x"
    }, new String[] {}, outputSuffix, false, null);
    this.outputPrefix = outputPrefix;
  }

  protected void addBase(final long base, final Vector<String> args) {
  }

  protected void addEntry(final String entry, final Vector<String> args) {
  }

  protected void addFixed(final Boolean fixed, final Vector<String> args) {
  }

  protected void addImpliedArgs(final boolean debug, final LinkType linkType, final Vector<String> args) {
    if (linkType.isSharedLibrary()) {
      args.addElement("-G");
    }
  }

  protected void addIncremental(final boolean incremental, final Vector<String> args) {
  }

  @Override
  public String[] addLibrarySets(final CCTask task, final LibrarySet[] libsets, final Vector<String> preargs,
      final Vector<String> midargs, final Vector<String> endargs) {
    super.addLibrarySets(task, libsets, preargs, midargs, endargs);
    final StringBuffer buf = new StringBuffer("-l");
    for (final LibrarySet set : libsets) {
      final File libdir = set.getDir(null);
      final String[] libs = set.getLibs();
      if (libdir != null) {
        endargs.addElement("-L");
        endargs.addElement(libdir.getAbsolutePath());
      }
      for (final String lib : libs) {
        //
        // reset the buffer to just "-l"
        //
        buf.setLength(2);
        //
        // add the library name
        buf.append(lib);
        //
        // add the argument to the list
        endargs.addElement(buf.toString());
      }
    }
    return null;
  }

  protected void addMap(final boolean map, final Vector<String> args) {
  }

  protected void addStack(final int stack, final Vector<String> args) {
  }

  @Override
  public String getCommandFileSwitch(final String commandFile) {
    return "@" + commandFile;
  }

  @Override
  public File[] getLibraryPath() {
    return CUtil.getPathFromEnvironment("LIB", ";");
  }

  @Override
  public String[] getLibraryPatterns(final String[] libnames, final LibraryTypeEnum libType) {
    return C89Processor.getLibraryPatterns(libnames, libType);
  }

  @Override
  public Linker getLinker(final LinkType linkType) {
    if (linkType.isSharedLibrary()) {
      return dllLinker;
    }
    /*
     * if(linkType.isStaticLibrary()) { return
     * OS390Librarian.getInstance(); }
     */
    return instance;
  }

  @Override
  public int getMaximumCommandLength() {
    return Integer.MAX_VALUE;
  }

  @Override
  public String[] getOutputFileNames(final String baseName, final VersionInfo versionInfo) {
    final String[] baseNames = super.getOutputFileNames(baseName, versionInfo);
    if (this.outputPrefix.length() > 0) {
      for (int i = 0; i < baseNames.length; i++) {
        baseNames[i] = this.outputPrefix + baseNames[i];
      }
    }
    return baseNames;
  }

  @Override
  public String[] getOutputFileSwitch(final String outputFile) {
    return new String[] {
        "-o", outputFile
    };
  }

  @Override
  public boolean isCaseSensitive() {
    return C89Processor.isCaseSensitive();
  }
}
