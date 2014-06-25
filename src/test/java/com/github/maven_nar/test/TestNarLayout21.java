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

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import com.github.maven_nar.AbstractNarLayout;
import com.github.maven_nar.Library;
import com.github.maven_nar.NarConstants;
import com.github.maven_nar.NarFileLayout;
import com.github.maven_nar.NarFileLayout10;
import com.github.maven_nar.NarLayout;
import com.github.maven_nar.NarLayout21;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class TestNarLayout21
    extends TestCase
{
    private NarFileLayout fileLayout;

    private Log log;

    private NarLayout layout;

    private File baseDir;

    private String artifactId;

    private String version;

    private String aol;

    private String type;

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
        baseDir = new File( "/Users/maven" );
        aol = "x86_64-MacOSX-g++";
        type = Library.SHARED;

        log = new SystemStreamLog();
        layout = new NarLayout21( log );
    }

    public final void testGetLayout()
        throws MojoExecutionException
    {
        AbstractNarLayout.getLayout( "NarLayout21", log );
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
        Assert.assertEquals( new File( baseDir, artifactId + "-" + version + "-" + NarConstants.NAR_NO_ARCH
            + File.separator + fileLayout.getIncludeDirectory() ), layout.getIncludeDirectory( baseDir, artifactId, version ) );
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
        Assert.assertEquals( new File( baseDir, artifactId + "-" + version + "-" + aol + "-" + type + File.separator
            + fileLayout.getLibDirectory( aol, type ) ), layout.getLibDirectory( baseDir, artifactId, version, aol, type ) );
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
        Assert.assertEquals( new File( baseDir, artifactId + "-" + version + "-" + aol + "-" + "executable"
            + File.separator + fileLayout.getBinDirectory( aol ) ), layout.getBinDirectory( baseDir, artifactId, version, aol ) );
    }
}
