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

import java.util.Vector;

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CUtil;

/**
 * Preprocessor macro undefinition.
 *
 * @author Mark A Russell <a
 *         href="mailto:mark_russell@csgsystems.com">mark_russell@csg_systems.
 *         com
 *         </a>
 */
public class UndefineArgument {
  /**
   * This method returns an array of UndefineArgument and DefineArgument's by
   * merging a base list with an override list.
   * 
   * Any define in the base list with a name that appears in the override
   * list is suppressed. All entries in the override list are preserved
   * 
   */
  public static UndefineArgument[] merge(final UndefineArgument[] base, final UndefineArgument[] override) {
    if (base.length == 0) {
      final UndefineArgument[] overrideClone = override.clone();
      return overrideClone;
    }
    if (override.length == 0) {
      final UndefineArgument[] baseClone = base.clone();
      return baseClone;
    }
    final Vector<UndefineArgument> unduplicated = new Vector<>(base.length);
    for (final UndefineArgument current : base) {
      final String currentName = current.getName();
      boolean match = false;
      if (currentName == null) {
        match = true;
      } else {
        for (final UndefineArgument over : override) {
          final String overName = over.getName();
          if (overName != null && overName.equals(currentName)) {
            match = true;
            break;
          }
        }
      }
      if (!match) {
        unduplicated.addElement(current);
      }
    }
    final UndefineArgument[] combined = new UndefineArgument[unduplicated.size() + override.length];
    unduplicated.copyInto(combined);
    final int offset = unduplicated.size();
    System.arraycopy(override, 0, combined, offset + 0, override.length);
    return combined;
  }

  private boolean define = false;
  private String ifCond;
  private String name;
  private String unlessCond;

  public UndefineArgument() {
  }

  protected UndefineArgument(final boolean isDefine) {
    this.define = isDefine;
  }

  public void execute() throws org.apache.tools.ant.BuildException {
    throw new org.apache.tools.ant.BuildException("Not an actual task, but looks like one for documentation purposes");
  }

  /** Returns the name of the define */
  public final String getName() {
    return this.name;
  }

  /** Returns the value of the define */
  public String getValue() {
    return null;
  }

  /**
   * Returns true if the define's if and unless conditions (if any) are
   * satisfied.
   * 
   * @exception BuildException
   *              throws build exception if name is not set
   */
  public final boolean isActive(final org.apache.tools.ant.Project p) throws BuildException {
    if (this.name == null) {
      throw new BuildException("<define> is missing name attribute");
    }
    return CUtil.isActive(p, this.ifCond, this.unlessCond);
  }

  /** Returns true if this is a define, false if an undefine. */
  public final boolean isDefine() {
    return this.define;
  }

  /**
   * Sets the property name for the 'if' condition.
   * 
   * The define will be ignored unless the property is defined.
   * 
   * The value of the property is insignificant, but values that would imply
   * misinterpretation ("false", "no") will throw an exception when
   * evaluated.
   * 
   * @param propName
   *          property name
   */
  public final void setIf(final String propName) {
    this.ifCond = propName;
  }

  /** Set the name attribute */
  public final void setName(final String name) {
    this.name = name;
  }

  /**
   * Set the property name for the 'unless' condition.
   * 
   * If named property is set, the define will be ignored.
   * 
   * The value of the property is insignificant, but values that would imply
   * misinterpretation ("false", "no") of the behavior will throw an
   * exception when evaluated.
   * 
   * @param propName
   *          name of property
   */
  public final void setUnless(final String propName) {
    this.unlessCond = propName;
  }
}
