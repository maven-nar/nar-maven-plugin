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

import java.io.IOException;

import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;
import com.github.maven_nar.cpptasks.compiler.ProgressMonitor;

public class CCTaskProgressMonitor implements ProgressMonitor {
  private ProcessorConfiguration config;
  private final TargetHistoryTable history;
  private final VersionInfo versionInfo;
  private long lastCommit = -1;

  public CCTaskProgressMonitor(final TargetHistoryTable history, final VersionInfo versionInfo) {
    this.history = history;
    this.versionInfo = versionInfo;
  }

  @Override
  public void finish(final ProcessorConfiguration config, final boolean normal) {
    final long current = System.currentTimeMillis();
    if (current - this.lastCommit > 120000) {
      try {
        this.history.commit();
        this.lastCommit = System.currentTimeMillis();
      } catch (final IOException ex) {
      }
    }
  }

  @Override
  public void progress(final String[] sources) {
    this.history.update(this.config, sources, this.versionInfo);
    final long current = System.currentTimeMillis();
    if (current - this.lastCommit > 120000) {
      try {
        this.history.commit();
        this.lastCommit = current;
      } catch (final IOException ex) {
      }
    }
  }

  @Override
  public void start(final ProcessorConfiguration config) {
    if (this.lastCommit < 0) {
      this.lastCommit = System.currentTimeMillis();
    }
    this.config = config;
  }
}
