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

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;

/**
 * Keeps info on a system library
 * 
 * @author Mark Donszelmann
 */
public class SysLib
{
    /**
     * Name of the system library
     * 
     * @parameter default-value=""
     * @required
     */
    private String name;

    /**
     * Type of linking for this system library
     * 
     * @parameter default-value="shared"
     * @required
     */
    private String type = Library.SHARED;

    public final SystemLibrarySet getSysLibSet( Project antProject )
        throws MojoFailureException
    {
        if ( name == null )
        {
            throw new MojoFailureException( "NAR: Please specify <Name> as part of <SysLib>" );
        }
        SystemLibrarySet sysLibSet = new SystemLibrarySet();
        sysLibSet.setProject( antProject );
        sysLibSet.setLibs( new CUtil.StringArrayBuilder( name ) );
        LibraryTypeEnum sysLibType = new LibraryTypeEnum();
        sysLibType.setValue( type );
        sysLibSet.setType( sysLibType );
        return sysLibSet;
    }
}
