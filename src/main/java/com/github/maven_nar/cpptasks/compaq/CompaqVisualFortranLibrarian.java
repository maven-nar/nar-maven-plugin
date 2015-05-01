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
package com.github.maven_nar.cpptasks.compaq;

import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.msvc.MsvcLibrarian;
import com.github.maven_nar.cpptasks.msvc.MsvcProcessor;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * Adapter for the Compaq(r) Visual Fortran Librarian
 *
 * @author Curt Arnold
 */
public class CompaqVisualFortranLibrarian extends CommandLineLinker {
  private static final CompaqVisualFortranLibrarian instance = new CompaqVisualFortranLibrarian();

  public static CompaqVisualFortranLibrarian getInstance() {
    return instance;
  }

  private CompaqVisualFortranLibrarian() {
    super("lib", "/bogus", new String[] {
      ".obj"
    }, new String[0], ".lib", false, null);
  }

  @Override
  protected void addImpliedArgs(final CCTask task, final boolean debug, final LinkType linkType,
      final Vector<String> args) {
    args.addElement("/nologo");
  }

  @Override
  protected String getCommandFileSwitch(final String commandFile) {
    return MsvcProcessor.getCommandFileSwitch(commandFile);
  }

  @Override
  public File[] getLibraryPath() {
    return new File[0];
  }

  @Override
  public String[] getLibraryPatterns(final String[] libnames, final LibraryTypeEnum libType) {
    return new String[0];
  }

  @Override
  public Linker getLinker(final LinkType type) {
    return CompaqVisualFortranLinker.getInstance().getLinker(type);
  }

  @Override
  protected int getMaximumCommandLength() {
    return MsvcLibrarian.getInstance().getMaximumCommandLength();
  }

  @Override
  protected String[] getOutputFileSwitch(final String outputFile) {
    return MsvcLibrarian.getInstance().getOutputFileSwitch(outputFile);
  }

  @Override
  public boolean isCaseSensitive() {
    return false;
  }
}
