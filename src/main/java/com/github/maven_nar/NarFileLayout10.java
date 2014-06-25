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

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarFileLayout10
    implements NarFileLayout
{

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarFileLayout#getIncludeDirectory()
     */
    public String getIncludeDirectory()
    {
        return "include";
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarFileLayout#getLibDirectory(java.lang.String, java.lang.String)
     */
    public String getLibDirectory( String aol, String type )
    {
        return "lib" + File.separator + aol + File.separator + type;
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarFileLayout#getBinDirectory(java.lang.String)
     */
    public String getBinDirectory( String aol )
    {
        return "bin" + File.separator + aol;
    }
}
