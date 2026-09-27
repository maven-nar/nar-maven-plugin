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
package com.github.maven_nar.cpptasks.gcc;

import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CCTask;

/** Shared map-file support for GCC/Clang drivers and direct ld adapters. */
public abstract class GccCompatibleLinker extends AbstractLdLinker {
  private final boolean compilerDriver;

  protected GccCompatibleLinker(final String command, final String identifierArg, final String[] extensions,
      final String[] ignoredExtensions, final String outputPrefix, final String outputSuffix, final boolean isLibtool,
      final AbstractLdLinker libtoolLinker, final boolean compilerDriver) {
    super(command, identifierArg, extensions, ignoredExtensions, outputPrefix, outputSuffix, isLibtool, libtoolLinker);
    this.compilerDriver = compilerDriver;
  }

  @Override
  protected void addMap(final CCTask task, final boolean map, final Vector<String> args) {
    // Defer until the final output filename is known.
  }

  @Override
  protected File getMapFile(final File outputFile, final boolean map) {
    return map ? new File(outputFile.getParentFile(), outputFile.getName() + ".map") : null;
  }

  @Override
  protected String[] getMapFileSwitch(final CCTask task, final String mapFile) {
    // NAR supplies its effective target OS. Standalone cpptasks callers fall back
    // to the host.
    final String targetOS = task.getProject() == null ? null : task.getProject().getProperty("nar.os");
    final String os = targetOS == null ? getOSName() : targetOS;
    final boolean darwin = "MacOSX".equals(os) || "Mac OS X".equals(os) || "Darwin".equals(os);
    final String[] options = darwin ? new String[] {
        "-map", mapFile
    } : new String[] {
        "-Map=" + mapFile
    };
    if (!this.compilerDriver) {
      return options;
    }
    // Forward each argument explicitly: -Wl splits commas, and the generic
    // decorator
    // treats Darwin's -map as a compiler -m option.
    final String[] forwarded = new String[options.length * 2];
    for (int i = 0; i < options.length; i++) {
      forwarded[2 * i] = "-Xlinker";
      forwarded[2 * i + 1] = options[i];
    }
    return forwarded;
  }

  @Override
  public String[] getOutputFileSwitch(final String outputFile) {
    // ProcessBuilder receives individual arguments; response-file quoting is
    // handled separately.
    return new String[] {
        "-o", outputFile.replace('\\', '/')
    };
  }

  @Override
  protected boolean hasRawReplayArguments() {
    return true;
  }

  @Override
  protected String quoteFilename(final StringBuffer buf, final String filename) {
    return filename;
  }
}
