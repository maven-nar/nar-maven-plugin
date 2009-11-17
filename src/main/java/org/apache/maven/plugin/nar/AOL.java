package org.apache.maven.plugin.nar;

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
 * @author Mark Donszelmann
 * @version $Id$
 */
public class AOL
{

    private String architecture;

    private String os;

    private String linkerName;

    // FIXME, need more complicated parsing for numbers as part of os.
    public AOL( String aol )
    {
        String[] aolString = aol.split( "-", 3 );
        switch ( aolString.length )
        {
            case 3:
                linkerName = aolString[2];
            case 2:
                os = aolString[1];
            case 1:
                architecture = aolString[0];
                break;

            default:
                throw new IllegalArgumentException( "AOL '" + aol + "' cannot be parsed." );
        }
    }

    public AOL( String architecture, String os, String linkerName )
    {
        this.architecture = architecture;
        this.os = os;
        this.linkerName = linkerName;
    }

    /**
     * Returns an AOL string (arch-os-linker) to use as directory or file.
     * @return dash separated AOL
     */
    public String toString()
    {
        return architecture + ( ( os == null ) ? "" : "-" + os + ( ( linkerName == null ) ? "" : "-" + linkerName ) );
    }

    // FIXME, maybe change to something like isCompatible (AOL).
    public boolean hasLinker( String linker )
    {
        return linkerName.equals( linker );
    }

    /**
     * Returns an AOL key (arch.os.linker) to search in the properties files. 
     * @return dot separated AOL
     */
    public String getKey()
    {
        return architecture + ( ( os == null ) ? "" : "." + os + ( ( linkerName == null ) ? "" : "." + linkerName ) );
    }
    
    String getOS() {
        return os;
    }
}
