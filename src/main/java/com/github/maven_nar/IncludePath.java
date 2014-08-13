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
package com.github.maven_nar;

import java.io.File;
import java.util.List;

/**
 * An include path.
 *
 * Binds together include path itself (e.g. folder path) and allowed
 * include file masks (e.g. *.h).
 *
 * @author Ivan Drobyshevskyi
 */
public class IncludePath {
    /**
     * Include path itself.
     *
     * @parameter
     * @required
     */
    private String path;

    /**
     * List of include files masks.
     *
     * @parameter
     */
    private List/* <String> */includes;

    /**
     * File corresponding to the path above.
     */
    private File file;

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
        file = new File(path);
    }
    public File getFile() {
        return file;
    }
    public final String getIncludes()
    {
        StringBuilder includesString = new StringBuilder();

        if (includes == null)
            return null;

        for (String s : (List<String>) includes)
             includesString.append(s + ",");

        return includesString.toString();
    }
    boolean exists() {
        return file.exists() && file.isDirectory();
    }
}
