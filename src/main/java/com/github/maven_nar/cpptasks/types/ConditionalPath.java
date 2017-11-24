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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import com.github.maven_nar.cpptasks.CUtil;

/**
 * An Ant Path object augmented with if and unless conditionals
 *
 * @author Curt Arnold
 */
public class ConditionalPath extends Path {
  private String ifCond;
  private String unlessCond;

  public ConditionalPath(final Project project) {
    super(project);
  }

  public ConditionalPath(final Project p, final String path) {
    super(p, path);
  }

  /**
   * Returns true if the Path's if and unless conditions (if any) are
   * satisfied.
   */
  public boolean isActive(final org.apache.tools.ant.Project p) throws BuildException {
    return CUtil.isActive(p, this.ifCond, this.unlessCond);
  }

  /**
   * Sets the property name for the 'if' condition.
   * 
   * The path will be ignored unless the property is defined.
   * 
   * The value of the property is insignificant, but values that would imply
   * misinterpretation ("false", "no") will throw an exception when
   * evaluated.
   * 
   * @param propName
   *          property name
   */
  public void setIf(final String propName) {
    this.ifCond = propName;
  }

  /**
   * Set the property name for the 'unless' condition.
   * 
   * If named property is set, the path will be ignored.
   * 
   * The value of the property is insignificant, but values that would imply
   * misinterpretation ("false", "no") of the behavior will throw an
   * exception when evaluated.
   * 
   * @param propName
   *          name of property
   */
  public void setUnless(final String propName) {
    this.unlessCond = propName;
  }
}
