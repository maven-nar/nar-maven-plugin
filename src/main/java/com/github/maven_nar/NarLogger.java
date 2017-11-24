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
package com.github.maven_nar;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

/**
 * Logger to connect the Ant logging to the Maven logging.
 *
 * @author Mark Donszelmann
 */
public class NarLogger implements BuildListener {

  private final Log log;

  public NarLogger(final Log log) {
    this.log = log;
  }

  @Override
  public void buildFinished(final BuildEvent event) {
  }

  @Override
  public void buildStarted(final BuildEvent event) {
  }

  @Override
  public final void messageLogged(final BuildEvent event) {
    final String msg = event.getMessage();
    switch (event.getPriority()) {
      case Project.MSG_ERR:
        if (msg.contains("ar: creating archive")) {
          this.log.debug(msg);
        } else if (msg.contains("warning")) {
          this.log.warn(msg);
        } else {
          this.log.error(msg);
        }
        break;
      case Project.MSG_WARN:
        this.log.warn(msg);
        break;
      case Project.MSG_INFO:
        if (msg.contains("error")) {
          this.log.error(msg);
        } else {
          this.log.info(msg);
        }
        break;
      case Project.MSG_VERBOSE:
        this.log.debug(msg);
        break;
      default:
      case Project.MSG_DEBUG:
        this.log.debug(msg);
        break;
    }
  }

  @Override
  public void targetFinished(final BuildEvent event) {
  }

  @Override
  public void targetStarted(final BuildEvent event) {
  }

  @Override
  public void taskFinished(final BuildEvent event) {
  }

  @Override
  public void taskStarted(final BuildEvent event) {
  }
}
