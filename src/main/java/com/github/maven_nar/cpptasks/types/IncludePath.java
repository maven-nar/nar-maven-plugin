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
package com.github.maven_nar.cpptasks.types;

import org.apache.tools.ant.Project;

/**
 * An include path.
 *
 * Works like other paths in Ant with with the addition of "if" and "unless"
 * conditions.
 *
 * @author Curt Arnold
 */
public class IncludePath extends ConditionalPath {
  public IncludePath(final Project project) {
    super(project);
  }

  public IncludePath(final Project p, final String path) {
    super(p, path);
  }

  public void execute() throws org.apache.tools.ant.BuildException {
    throw new org.apache.tools.ant.BuildException("Not an actual task, but looks like one for documentation purposes");
  }
}
