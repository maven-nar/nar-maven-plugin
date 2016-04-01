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
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;

import com.github.maven_nar.cpptasks.CUtil;

/**
 * Set of preprocessor macro defines and undefines.
 *
 * @author Mark A Russell <a
 *         href="mailto:mark_russell@csgsystems.com">mark_russell@csg_systems.
 *         com
 *         </a>
 * @author Adam Murdoch
 */
public class DefineSet extends DataType {
  private final Vector<UndefineArgument> defineList = new Vector<>();
  private String ifCond = null;
  private String unlessCond = null;

  /**
   * 
   * Adds a define element.
   * 
   * @throws BuildException
   *           if reference
   */
  public void addDefine(final DefineArgument arg) throws BuildException {
    if (isReference()) {
      throw noChildrenAllowed();
    }
    this.defineList.addElement(arg);
  }

  /** Adds defines/undefines. */
  private void addDefines(final String[] defs, final boolean isDefine) {
    for (final String def2 : defs) {
      UndefineArgument def;
      if (isDefine) {
        def = new DefineArgument();
      } else {
        def = new UndefineArgument();
      }
      def.setName(def2);
      this.defineList.addElement(def);
    }
  }

  /**
   * 
   * Adds an undefine element.
   * 
   * @throws BuildException
   *           if reference
   */
  public void addUndefine(final UndefineArgument arg) throws BuildException {
    if (isReference()) {
      throw noChildrenAllowed();
    }
    this.defineList.addElement(arg);
  }

  public void execute() throws org.apache.tools.ant.BuildException {
    throw new org.apache.tools.ant.BuildException("Not an actual task, but looks like one for documentation purposes");
  }

  /** Returns the defines and undefines in this set. */
  public UndefineArgument[] getDefines() throws BuildException {
    if (isReference()) {
      final DefineSet defset = (DefineSet) getCheckedRef(DefineSet.class, "DefineSet");
      return defset.getDefines();
    } else {
      if (isActive()) {
        final UndefineArgument[] defs = new UndefineArgument[this.defineList.size()];
        this.defineList.copyInto(defs);
        return defs;
      } else {
        return new UndefineArgument[0];
      }
    }
  }

  /**
   * Returns true if the define's if and unless conditions (if any) are
   * satisfied.
   * 
   * @exception BuildException
   *              throws build exception if name is not set
   */
  public final boolean isActive() throws BuildException {
    return CUtil.isActive(getProject(), this.ifCond, this.unlessCond);
  }

  /**
   * A comma-separated list of preprocessor macros to define. Use nested
   * define elements to define macro values.
   * 
   * @param defList
   *          comma-separated list of preprocessor macros
   * @throws BuildException
   *           throw if defineset is a reference
   */
  public void setDefine(final CUtil.StringArrayBuilder defList) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    addDefines(defList.getValue(), true);
  }

  /**
   * Sets a description of the current data type.
   */
  @Override
  public void setDescription(final String desc) {
    super.setDescription(desc);
  }

  /**
   * Sets an id that can be used to reference this element.
   * 
   * @param id
   *          id
   */
  public void setId(final String id) {
    //
    // this is actually accomplished by a different
    // mechanism, but we can document it
    //
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

  /**
   * Specifies that this element should behave as if the content of the
   * element with the matching id attribute was inserted at this location. If
   * specified, no other attributes or child content should be specified,
   * other than "description".
   * 
   */
  @Override
  public void setRefid(final Reference r) throws BuildException {
    if (!this.defineList.isEmpty()) {
      throw tooManyAttributes();
    }
    super.setRefid(r);
  }

  /**
   * A comma-separated list of preprocessor macros to undefine.
   * 
   * @param undefList
   *          comma-separated list of preprocessor macros
   * @throws BuildException
   *           throw if defineset is a reference
   */
  public void setUndefine(final CUtil.StringArrayBuilder undefList) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    addDefines(undefList.getValue(), false);
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
