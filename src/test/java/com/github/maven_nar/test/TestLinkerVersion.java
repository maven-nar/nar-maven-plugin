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
package com.github.maven_nar.test;

import com.github.maven_nar.Linker;
import com.github.maven_nar.NarProperties;
import com.github.maven_nar.NarUtil;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 * @version $Id$
 */
public class TestLinkerVersion
    extends TestCase
{
    private Linker linker;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();
        String architecture = System.getProperty( "os.arch" );
        linker = new Linker();
//        String name =
            linker.getName( NarProperties.getInstance(null), NarUtil.getArchitecture( architecture ) + "." + NarUtil.getOS( null )
                + "." );
    }

    public void testVersion()
        throws Exception
    {
        if ( "Windows".equals( NarUtil.getOS( null ) ) &&
	    null == System.getenv( "DevEnvDir" ) )
	  {
	    // Skip testing the MSVC linker on Win if vsvars32.bat has not run
	    return;
	  }
        String version = linker.getVersion();
        Assert.assertNotNull( version );
    }

}
