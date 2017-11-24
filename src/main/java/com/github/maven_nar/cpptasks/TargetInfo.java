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

import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;

/**
 * A description of a file built or to be built
 */
public final class TargetInfo {
  private static final File[] emptyFileArray = new File[0];
  private final/* final */ProcessorConfiguration config;
  private final/* final */File output;
  private boolean rebuild;
  private final/* final */File[] sources;
  private File[] sysSources;

  public TargetInfo(final ProcessorConfiguration config, final File[] sources, final File[] sysSources,
      final File output, boolean rebuild) {
    if (config == null) {
      throw new NullPointerException("config");
    }
    if (sources == null) {
      throw new NullPointerException("sources");
    }
    if (output == null) {
      throw new NullPointerException("output");
    }
    this.config = config;
    this.sources = sources.clone();
    if (sysSources == null) {
      this.sysSources = emptyFileArray;
    } else {
      this.sysSources = sysSources.clone();
    }
    this.output = output;
    this.rebuild = rebuild;
    //
    // if the output doesn't exist, must rebuild it
    //
    if (!output.exists()) {
      rebuild = true;
    }
  }

  public String[] getAllSourcePaths() {
    final String[] paths = new String[this.sysSources.length + this.sources.length];
    for (int i = 0; i < this.sysSources.length; i++) {
      paths[i] = this.sysSources[i].toString();
    }
    final int offset = this.sysSources.length;
    for (int i = 0; i < this.sources.length; i++) {
      paths[offset + i] = this.sources[i].toString();
    }
    return paths;
  }

  public File[] getAllSources() {
    final File[] allSources = new File[this.sources.length + this.sysSources.length];
    System.arraycopy(this.sysSources, 0, allSources, 0, this.sysSources.length);
    final int offset = this.sysSources.length;
    System.arraycopy(this.sources, 0, allSources, 0 + offset, this.sources.length);
    return allSources;
  }

  public ProcessorConfiguration getConfiguration() {
    return this.config;
  }

  public File getOutput() {
    return this.output;
  }

  public boolean getRebuild() {
    return this.rebuild;
  }

  /**
   * Returns an array of SourceHistory objects (contains relative path and
   * last modified time) for the source[s] of this target
   */
  public SourceHistory[] getSourceHistories(final String basePath) {
    final SourceHistory[] histories = new SourceHistory[this.sources.length];
    for (int i = 0; i < this.sources.length; i++) {
      final String relativeName = CUtil.getRelativePath(basePath, this.sources[i]);
      final long lastModified = this.sources[i].lastModified();
      histories[i] = new SourceHistory(relativeName, lastModified);
    }
    return histories;
  }

  public String[] getSourcePaths() {
    final String[] paths = new String[this.sources.length];
    for (int i = 0; i < this.sources.length; i++) {
      paths[i] = this.sources[i].toString();
    }
    return paths;
  }

  public File[] getSources() {
    final File[] clone = this.sources.clone();
    return clone;
  }

  public String[] getSysSourcePaths() {
    final String[] paths = new String[this.sysSources.length];
    for (int i = 0; i < this.sysSources.length; i++) {
      paths[i] = this.sysSources[i].toString();
    }
    return paths;
  }

  public File[] getSysSources() {
    final File[] clone = this.sysSources.clone();
    return clone;
  }

  public void mustRebuild() {
    this.rebuild = true;
  }
}
