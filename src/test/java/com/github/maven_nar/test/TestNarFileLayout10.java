package com.github.maven_nar.test;

import java.io.File;

import com.github.maven_nar.Library;
import com.github.maven_nar.NarFileLayout;
import com.github.maven_nar.NarFileLayout10;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 * @version $Id$
 */
public class TestNarFileLayout10
    extends TestCase
{
    protected NarFileLayout fileLayout;

    protected String artifactId;

    protected String version;

    protected String aol;

    protected String type;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        fileLayout = new NarFileLayout10();
        artifactId = "artifactId";
        version = "version";
        aol = "x86_64-MacOSX-g++";
        type = Library.SHARED;
    }

    public final void testGetIncludeDirectory()
    {
        Assert.assertEquals( "include", fileLayout.getIncludeDirectory() );
    }

    public final void testGetLibDirectory()
    {
        Assert.assertEquals( "lib" + File.separator + aol + File.separator + type, fileLayout.getLibDirectory( aol,
                                                                                                               type ) );
    }

    public final void testGetBinDirectory()
    {
        Assert.assertEquals( "bin" + File.separator + aol, fileLayout.getBinDirectory( aol ) );
    }
}
