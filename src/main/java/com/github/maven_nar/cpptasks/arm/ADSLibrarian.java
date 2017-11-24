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

import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

/**
 * Adapter for ARM Librarian
 *
 * @author Curt Arnold
 */
public class ADSLibrarian extends CommandLineLinker {

  private static final ADSLibrarian instance = new ADSLibrarian();

  public static ADSLibrarian getInstance() {
    return instance;
  }

  private ADSLibrarian() {
    super("armar", null, new String[] {
      ".o"
    }, new String[0], ".lib", false, null);
  }

  @Override
  protected String getCommandFileSwitch(final String commandFile) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public File[] getLibraryPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String[] getLibraryPatterns(final String[] libnames, final LibraryTypeEnum libType) {
    return new String[0];
  }

  @Override
  public Linker getLinker(final LinkType linkType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected int getMaximumCommandLength() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  protected String[] getOutputFileSwitch(final String outputFile) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isCaseSensitive() {
    // TODO Auto-generated method stub
    return false;
  }

}
