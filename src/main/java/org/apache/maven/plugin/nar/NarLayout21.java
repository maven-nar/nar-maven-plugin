package org.apache.maven.plugin.nar;

import java.io.File;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Layout which expands a nar file into:
 * 
 * <pre>
 * nar/noarch/include
 * nar/aol/<aol>-<type>/bin
 * nar/aol/<aol>-<type>/lib
 * </pre>
 * 
 * This loayout has a one-to-one relation with the aol-type version of the nar.
 * 
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarLayout21
    implements NarLayout
{
    public File getAolDirectory( File baseDir )
    {
        return new File( baseDir, "aol" );
    }

    public File getNoarchDirectory( File baseDir )
    {
        return new File( baseDir, "noarch" );
    }

    private File getAolDirectory( File baseDir, String aol, String type )
    {
        return new File( getAolDirectory( baseDir ), aol + "-" + type );
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarLayout#getIncludeDirectory(java.io.File)
     */
    public File getIncludeDirectory( File baseDir )
    {
        File dir = getNoarchDirectory( baseDir );
        dir = new File( dir, "include" );
        return dir;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarLayout#getLibDir(org.apache.maven.plugin.nar.AOL, java.lang.String)
     */
    public File getLibDirectory( File baseDir, String aol, String type )
    {
        File dir = getAolDirectory( baseDir, aol, type );
        dir = new File( dir, type.equals( Library.EXECUTABLE ) ? "bin" : "lib" );
        dir = new File( dir, aol.toString() );
        if ( !type.equals( Library.EXECUTABLE ) )
            dir = new File( dir, type );
        return dir;
    }

}
