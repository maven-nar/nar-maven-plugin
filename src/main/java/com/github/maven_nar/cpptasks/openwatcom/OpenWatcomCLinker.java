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
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.openwatcom;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;

/**
 * Adapter for the OpenWatcom linker.
 *
 * @author Curt Arnold
 */
public final class OpenWatcomCLinker
    extends OpenWatcomLinker {
  /**
   * Dll linker.
   */
  private static final OpenWatcomCLinker DLL_LINKER =
      new OpenWatcomCLinker(".dll");
  /**
   * Exe linker.
   */
  private static final OpenWatcomCLinker INSTANCE =
      new OpenWatcomCLinker(".exe");
  /**
   * Get linker instance.
   * @return OpenWatcomCLinker linker
   */
  public static OpenWatcomCLinker getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   * @param outputSuffix String output suffix.
   */
  private OpenWatcomCLinker(final String outputSuffix) {
    super("wcl386", outputSuffix);
  }

  /**
   * Get linker.
   * @param type LinkType link type
   * @return Linker linker
   */
  public Linker getLinker(final LinkType type) {
    if (type.isStaticLibrary()) {
      return OpenWatcomLibrarian.getInstance();
    }
    if (type.isSharedLibrary()) {
      return DLL_LINKER;
    }
    return INSTANCE;
  }
}
