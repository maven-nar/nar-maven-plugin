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
import java.util.Vector;

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.compiler.Linker;

/**
 * Collects object files for the link step.
 *
 * 
 */
public final class ObjectFileCollector implements FileVisitor {
  private final Vector<File> files;
  private final Linker linker;

  public ObjectFileCollector(final Linker linker, final Vector<File> files) {
    this.linker = linker;
    this.files = files;
  }

  @Override
  public void visit(final File parentDir, final String filename) throws BuildException {
    final int bid = this.linker.bid(filename);
    if (bid >= 1) {
      this.files.addElement(new File(parentDir, filename));
    }
  }
}
