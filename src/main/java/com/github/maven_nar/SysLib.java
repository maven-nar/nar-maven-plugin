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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

/**
 * Keeps info on a system library
 *
 * @author Mark Donszelmann
 */
public class SysLib {
  /**
   * Name of the system library
   */
  @Parameter(required = true)
  private String name;

  /**
   * Type of linking for this system library
   */
  @Parameter(defaultValue = "shared")
  private String type = Library.SHARED;

  public final SystemLibrarySet getSysLibSet(final Project antProject) throws MojoFailureException {
    if (this.name == null) {
      throw new MojoFailureException("NAR: Please specify <Name> as part of <SysLib>");
    }
    final SystemLibrarySet sysLibSet = new SystemLibrarySet();
    sysLibSet.setProject(antProject);
    sysLibSet.setLibs(new CUtil.StringArrayBuilder(this.name));
    final LibraryTypeEnum sysLibType = new LibraryTypeEnum();
    sysLibType.setValue(this.type);
    sysLibSet.setType(sysLibType);
    return sysLibSet;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SysLib other = (SysLib) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return this.name + " (" + this.type + ")";
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setType(String type) {
    this.type = type;
  }
}
