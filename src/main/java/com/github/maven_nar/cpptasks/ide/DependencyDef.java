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
    public List getDependsList() {
        if (depends != null) {
            return StringUtils.split(depends, ',');
        }
        return Collections.EMPTY_LIST;    
    }
}
