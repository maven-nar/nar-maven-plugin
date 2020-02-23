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
package com.github.maven_nar.cpptasks.cobol;

import java.util.Vector;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractLdLinker;

/**
 * Adapter for GNU Cobol
 *
 * @author Francis ANDRE
 */
public final class GNUCobolLinker extends AbstractLdLinker {
  private static final String[] discardFiles = new String[] {};
  private static final String[] objFiles = new String[] {
      ".obj", ".o", ".a", ".lib", ".dll", ".so", ".sl"
  };
  private static final GNUCobolLinker dllLinker = new GNUCobolLinker("cobc", objFiles, discardFiles, "lib", ".a");
  private static final GNUCobolLinker instance = new GNUCobolLinker("cobc", objFiles, discardFiles, "", "");

  public static GNUCobolLinker getInstance() {
    return instance;
  }

  private GNUCobolLinker(final String command, final String[] extensions, final String[] ignoredExtensions,
                      final String outputPrefix, final String outputSuffix) {
    //
    // just guessing that -? might display something useful
    //
    super(command, "-?", extensions, ignoredExtensions, outputPrefix, outputSuffix, false, null);
  }

  @Override
  public void
      addImpliedArgs(final CCTask task, final boolean debug, final LinkType linkType, final Vector<String> args) {
    if (debug) {
      args.addElement("-g");
    }
    if (linkType.isSharedLibrary()) {
      args.addElement("-b");
    }
  }

  @Override
  protected String getDynamicLibFlag() {
    return "-bdynamic";
  }

  /**
   * Gets identifier for the compiler.
   * 
   * Initial attempt at extracting version information
   * would lock up. Using a stock response.
   */
  @Override
  public String getIdentifier() {
    return "GNU Cobol linker - unidentified version";
  }

  @Override
  public Linker getLinker(final LinkType type) {
    if (type.isStaticLibrary()) {
      return GNUCobolLibrarian.getInstance();
    }
    if (type.isSharedLibrary()) {
      return dllLinker;
    }
    return instance;
  }

  @Override
  protected String getStaticLibFlag() {
    return "-bstatic";
  }

}
