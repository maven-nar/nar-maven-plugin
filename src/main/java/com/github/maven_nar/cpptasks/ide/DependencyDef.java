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
package com.github.maven_nar.cpptasks.ide;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.util.StringUtils;

/**
 * Defines a dependency
 *
 */
public final class DependencyDef {
  private String id;
  private File file;
  private String name;
  private String depends;

  public DependencyDef() {
  }

  public String getDepends() {
    return this.depends;
  }

  public List<String> getDependsList() {
    if (this.depends != null) {
      return StringUtils.split(this.depends, ',');
    }
    return Collections.emptyList();
  }

  public File getFile() {
    return this.file;
  }

  public String getID() {
    if (this.id != null) {
      return this.id;
    }
    return getName();
  }

  public String getName() {
    if (this.name != null) {
      return this.name;
    } else if (this.file != null) {
      return this.file.getName();
    }
    return "null";
  }

  public void setDepends(final String val) {
    this.depends = val;
  }

  public void setFile(final File val) {
    this.file = val;
  }

  public void setID(final String val) {
    this.id = val;
  }

  public void setName(final String val) {
    this.name = val;
  }
}
