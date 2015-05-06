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
package com.github.maven_nar.cpptasks.ti;

import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * Adapter for TI DSP linkers
 * *
 * 
 * @author CurtA
 * 
 */
public class ClxxLinker extends CommandLineLinker {
  private static final ClxxLinker cl55DllInstance = new ClxxLinker("lnk55", ".dll");
  private static final ClxxLinker cl55Instance = new ClxxLinker("lnk55", ".exe");
  private static final ClxxLinker cl6xDllInstance = new ClxxLinker("lnk6x", ".dll");
  private static final ClxxLinker cl6xInstance = new ClxxLinker("lnk6x", ".exe");

  public static ClxxLinker getCl55DllInstance() {
    return cl55DllInstance;
  }

  public static ClxxLinker getCl55Instance() {
    return cl55Instance;
  }

  public static ClxxLinker getCl6xDllInstance() {
    return cl6xDllInstance;
  }

  public static ClxxLinker getCl6xInstance() {
    return cl6xInstance;
  }

  private ClxxLinker(final String command, final String outputSuffix) {
    super(command, "-h", new String[] {
        ".o", ".lib", ".res"
    }, new String[] {
        ".map", ".pdb", ".lnk"
    }, outputSuffix, false, null);
  }

  protected void addBase(final long base, final Vector<String> args) {
  }

  protected void addEntry(final String entry, final Vector<String> args) {
  }

  protected void addFixed(final Boolean fixed, final Vector<String> args) {
  }

  protected void addImpliedArgs(final boolean debug, final LinkType linkType, final Vector<String> args) {
    if (linkType.isSharedLibrary()) {
      args.addElement("-abs");
    }
  }

  protected void addIncremental(final boolean incremental, final Vector<String> args) {
  }

  protected void addMap(final boolean map, final Vector<String> args) {
    if (map) {
      args.addElement("-m");
    }
  }

  protected void addStack(final int stack, final Vector<String> args) {
  }

  @Override
  protected String getCommandFileSwitch(final String commandFile) {
    return "@" + commandFile;
  }

  @Override
  public File[] getLibraryPath() {
    return new File[0];
  }

  @Override
  public String[] getLibraryPatterns(final String[] libnames, final LibraryTypeEnum libType) {
    //
    // TODO: Looks bogus, should be .a or .so's not .o's
    //
    final String[] libpats = new String[libnames.length];
    for (int i = 0; i < libnames.length; i++) {
      libpats[i] = libnames[i] + ".o";
    }
    return libpats;
  }

  @Override
  public Linker getLinker(final LinkType linkType) {
    return this;
  }

  @Override
  protected int getMaximumCommandLength() {
    return 1024;
  }

  @Override
  protected String[] getOutputFileSwitch(final String outputFile) {
    return new String[] {
        "-o", outputFile
    };
  }

  @Override
  public boolean isCaseSensitive() {
    // TODO Auto-generated method stub
    return false;
  }
}
