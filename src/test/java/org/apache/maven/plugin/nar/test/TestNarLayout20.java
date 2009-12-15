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

package org.apache.maven.plugin.nar.test;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.nar.AbstractNarLayout;
import org.apache.maven.plugin.nar.Library;
import org.apache.maven.plugin.nar.NarFileLayout;
import org.apache.maven.plugin.nar.NarFileLayout10;
import org.apache.maven.plugin.nar.NarLayout;
import org.apache.maven.plugin.nar.NarLayout20;

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
     * Test method for {@link org.apache.maven.plugin.nar.NarLayout20#getIncludeDirectory(java.io.File)}.
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
     * {@link org.apache.maven.plugin.nar.NarLayout20#getLibDirectory(java.io.File, java.lang.String, java.lang.String)}
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
     * Test method for {@link org.apache.maven.plugin.nar.NarLayout20#getBinDirectory(java.io.File, java.lang.String)}.
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
