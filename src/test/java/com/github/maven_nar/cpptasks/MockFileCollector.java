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
package com.github.maven_nar.cpptasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.maven_nar.cpptasks.FileVisitor;

/**
 * Implementation of FileVisitor that collects visited files for later
 * retrieval.
 *
 * @author Curt Arnold
 *
 */
public final class MockFileCollector
    implements FileVisitor {

  /**
   * list of fileName parameter values.
   */
  private final List fileNames = new ArrayList();

  /**
   * list of baseDir parameter values.
   */
  private final List baseDirs = new ArrayList();

  /**
   * Constructor.
   *
   */
  public MockFileCollector() {
  }

  /**
   * Implementation of FileVisitor.visit.
   * @param baseDir base directory
   * @param fileName file name
   */
  public void visit(final File baseDir, final String fileName) {
    fileNames.add(fileName);
    baseDirs.add(baseDir);
  }

  /**
   * Get value of fileName parameter for a specified index.
   *
   * @param index
   *            index
   * @return value of failName parameter
   */
  public String getFileName(final int index) {
    return (String) fileNames.get(index);
  }

  /**
   * Get value of baseDir parameter for the specified index.
   *
   * @param index
   *            index
   * @return value of baseDir parameter
   */
  public File getBaseDir(final int index) {
    return (File) baseDirs.get(index);
  }

  /**
   * Get count of calls to FileVisitor.visit.
   *
   * @return count of calls.
   */
  public int size() {
    return fileNames.size();
  }
}
