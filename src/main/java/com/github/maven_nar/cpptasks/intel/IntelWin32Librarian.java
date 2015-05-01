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
package com.github.maven_nar.cpptasks.intel;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.msvc.MsvcCompatibleLibrarian;

/**
 * Adapter for the xilib from the Intel(r) C++ Compiler for IA-32 or IA-64
 * systems running Microsoft (r) operating systems
 *
 * @author Curt Arnold
 */
public class IntelWin32Librarian extends MsvcCompatibleLibrarian {
  private static final IntelWin32Librarian instance = new IntelWin32Librarian();

  public static IntelWin32Librarian getInstance() {
    return instance;
  }

  protected IntelWin32Librarian() {
    super("xilib", "-qv");
  }

  @Override
  public Linker getLinker(final LinkType type) {
    return IntelWin32Linker.getInstance().getLinker(type);
  }
}
