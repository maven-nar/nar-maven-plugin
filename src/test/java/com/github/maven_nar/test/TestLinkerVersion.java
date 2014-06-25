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
