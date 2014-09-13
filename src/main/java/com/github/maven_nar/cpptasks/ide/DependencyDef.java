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
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.ide;

import org.apache.tools.ant.util.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

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


    public void setID(final String val) {
        id = val;
    }
    public File getFile() {
        return file;
    }
    public void setFile(final File val) {
        file = val;
    }
    public void setName(final String val) {
        name = val;
    }
    public String getName() {
        if (name != null) {
            return name;
        } else if(file != null) {
            return file.getName();
        }
        return "null";
    }
    public String getID() {
        if (id != null) {
            return id;
        }
        return getName();
    }
    public String getDepends() {
        return depends;
    }
    public void setDepends(final String val) {
        depends = val;
    }
    public List<String> getDependsList() {
        if (depends != null) {
            return StringUtils.split(depends, ',');
        }
        return Collections.emptyList();    
    }
}
