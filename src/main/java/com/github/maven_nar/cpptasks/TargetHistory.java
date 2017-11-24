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

/**
 * A description of a file built or to be built
 */
public final class TargetHistory {
  private final/* final */String config;
  private final/* final */String output;
  private final/* final */long outputLastModified;
  private final/* final */SourceHistory[] sources;

  /**
   * Constructor from build step
   */
  public TargetHistory(final String config, final String output, final long outputLastModified,
      final SourceHistory[] sources) {
    if (config == null) {
      throw new NullPointerException("config");
    }
    if (sources == null) {
      throw new NullPointerException("source");
    }
    if (output == null) {
      throw new NullPointerException("output");
    }
    this.config = config;
    this.output = output;
    this.outputLastModified = outputLastModified;
    this.sources = sources.clone();
  }

  public String getOutput() {
    return this.output;
  }

  public long getOutputLastModified() {
    return this.outputLastModified;
  }

  public String getProcessorConfiguration() {
    return this.config;
  }

  public SourceHistory[] getSources() {
    final SourceHistory[] clone = this.sources.clone();
    return clone;
  }
}
