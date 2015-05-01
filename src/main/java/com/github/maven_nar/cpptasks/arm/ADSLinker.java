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
package com.github.maven_nar.cpptasks.arm;

import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * Adapter for the ARM Linker
 *
 * @author CurtA
 */
public class ADSLinker extends CommandLineLinker {
  private static final ADSLinker dllInstance = new ADSLinker(".o");
  private static final ADSLinker instance = new ADSLinker(".axf");

  public static ADSLinker getDllInstance() {
    return dllInstance;
  }

  public static ADSLinker getInstance() {
    return instance;
  }

  private ADSLinker(final String outputSuffix) {
    super("armlink", "-vsn", new String[] {
        ".o", ".lib", ".res"
    }, new String[] {
        ".map", ".pdb", ".lnk"
    }, outputSuffix, false, null);
  }

  protected void addBase(final long base, final Vector<String> args) {
    // TODO Auto-generated method stub
  }

  protected void addEntry(final String entry, final Vector<String> args) {
    // TODO Auto-generated method stub

  }

  protected void addFixed(final Boolean fixed, final Vector<String> args) {
    // TODO Auto-generated method stub
  }

  protected void addImpliedArgs(final boolean debug, final LinkType linkType, final Vector<String> args) {
    if (debug) {
      args.addElement("-debug");
    }
  }

  protected void addIncremental(final boolean incremental, final Vector<String> args) {
    // TODO Auto-generated method stub
  }

  protected void addMap(final boolean map, final Vector<String> args) {
    // TODO Auto-generated method stub
  }

  protected void addStack(final int stack, final Vector<String> args) {
    // TODO Auto-generated method stub
  }

  /**
   * May have to make this String array return
   * 
   * @see com.github.maven_nar.cpptasks.compiler.CommandLineLinker#getCommandFileSwitch(java.lang.String)
   */
  @Override
  protected String getCommandFileSwitch(final String commandFile) {
    return "-via" + commandFile;
  }

  @Override
  public File[] getLibraryPath() {
    return CUtil.getPathFromEnvironment("ARMLIB", ";");
  }

  @Override
  public String[] getLibraryPatterns(final String[] libnames, final LibraryTypeEnum libType) {
    //
    // TODO: looks like bad extension
    //
    return new String[] {
      ".o"
    };
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
        "-output", outputFile
    };
  }

  @Override
  public boolean isCaseSensitive() {
    return false;
  }
}
