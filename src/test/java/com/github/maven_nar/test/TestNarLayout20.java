package com.github.maven_nar.test;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import com.github.maven_nar.AbstractNarLayout;
import com.github.maven_nar.Library;
import com.github.maven_nar.NarFileLayout;
import com.github.maven_nar.NarFileLayout10;
import com.github.maven_nar.NarLayout;
import com.github.maven_nar.NarLayout20;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class TestNarLayout20
    extends TestCase
{
    private NarFileLayout fileLayout;

    private Log log;

    private NarLayout layout;

    private File baseDir;

    private String aol;

    private String type;

    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        log = new SystemStreamLog();
        fileLayout = new NarFileLayout10();
        layout = new NarLayout20( log );
        baseDir = new File( "/Users/maven" );
        aol = "x86_64-MacOSX-g++";
        type = Library.SHARED;
    }

    public final void testGetLayout()
        throws MojoExecutionException
    {
        AbstractNarLayout.getLayout( "NarLayout20", log );
    }

    /**
     * Test method for {@link com.github.maven_nar.NarLayout20#getIncludeDirectory(java.io.File)}.
     * 
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    public final void testGetIncludeDirectory()
        throws MojoExecutionException, MojoFailureException
    {
        Assert.assertEquals( new File( baseDir, fileLayout.getIncludeDirectory() ),
                             layout.getIncludeDirectory( baseDir, null, null ) );
    }

    /**
     * Test method for
     * {@link com.github.maven_nar.NarLayout20#getLibDirectory(java.io.File, java.lang.String, java.lang.String)}
     * .
     * 
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    public final void testGetLibDirectory()
        throws MojoExecutionException, MojoFailureException
    {
        Assert.assertEquals( new File( baseDir, fileLayout.getLibDirectory( aol, type ) ),
                             layout.getLibDirectory( baseDir, null, null, aol, type ) );
    }

    /**
     * Test method for {@link com.github.maven_nar.NarLayout20#getBinDirectory(java.io.File, java.lang.String)}.
     * 
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    public final void testGetBinDirectory()
        throws MojoExecutionException, MojoFailureException
    {
        Assert.assertEquals( new File( baseDir, fileLayout.getBinDirectory( aol ) ), layout.getBinDirectory( baseDir,
                                                                                                             null,
                                                                                                             null, aol ) );
    }
}
