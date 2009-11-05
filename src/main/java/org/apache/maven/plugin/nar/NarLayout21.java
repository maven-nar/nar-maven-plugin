package org.apache.maven.plugin.nar;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

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
    extends AbstractNarLayout
{
    private File getNoarchDirectory( File baseDir )
    {
        return new File( baseDir, "noarch" );
    }

    private File getAolDirectory( File baseDir )
    {
        return new File( baseDir, "aol" );
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
        return new File( getNoarchDirectory( baseDir ), "include" );
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarLayout#getLibDir(java.io.File, org.apache.maven.plugin.nar.AOL,
     * java.lang.String)
     */
    public File getLibDirectory( File baseDir, String aol, String type )
    {
        if ( type.equals( Library.EXECUTABLE ) )
        {
            System.err.println( "WARNING, Replace call to getLibDirectory with getBinDirectory" );
            Thread.dumpStack();
        }

        File dir = getAolDirectory( baseDir, aol, type );
        dir = new File( dir, "lib" );
        dir = new File( dir, aol.toString() );
        dir = new File( dir, type );
        return dir;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarLayout#getLibDir(java.io.File, org.apache.maven.plugin.nar.AOL,
     * java.lang.String)
     */
    public File getBinDirectory( File baseDir, String aol )
    {
        File dir = getAolDirectory( baseDir, aol, Library.EXECUTABLE );
        dir = new File( dir, "bin" );
        dir = new File( dir, aol.toString() );
        return dir;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarLayout#attachNars(java.io.File, org.apache.maven.project.MavenProjectHelper,
     * org.apache.maven.project.MavenProject, org.apache.maven.plugin.nar.NarInfo)
     */
    public void attachNars( File baseDir, MavenProjectHelper projectHelper, MavenProject project, NarInfo narInfo )
        throws MojoExecutionException
    {
        if (getNoarchDirectory( baseDir ).exists()) {
        attachNar( projectHelper, project, "noarch", getNoarchDirectory( baseDir ), "*/**" );
        narInfo.setNar( null, "noarch", project.getGroupId() + ":" + project.getArtifactId() + ":"
            + NarConstants.NAR_TYPE + ":" + "noarch" );
        }
        
        String bindingType = null;
        File classifierDir = getAolDirectory( baseDir );
        String[] classifier = classifierDir.list();
        for ( int i = 0; ( classifier != null ) && ( i < classifier.length ); i++ )
        {
            File dir = new File( classifierDir, classifier[i] );
            attachNar( projectHelper, project, classifier[i], dir, "*/**" );

            // look for type in aol/lib/<aol>/type
            String type = Library.EXECUTABLE;
            File libDir = new File( dir, "lib" );
            String[] aolDir = libDir.list();
            if ( ( aolDir != null ) && aolDir.length > 0 )
            {
                String[] typeDir = new File( libDir, aolDir[0] ).list();
                if ( ( typeDir != null ) && ( typeDir.length > 0 ) )
                {
                    type = typeDir[0];
                }
            }

            narInfo.setNar( null, type, project.getGroupId() + ":" + project.getArtifactId() + ":"
                + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + type );

            // set if not Executable 
            if ( !type.equals( Library.EXECUTABLE ) )
            {
                // and not set or override if SHARED
                if ( ( bindingType == null ) || type.equals( Library.SHARED ) )
                {
                    bindingType = type;
                }
            }

        }

        if ( narInfo.getBinding( null, null ) == null )
        {
            narInfo.setBinding( null, bindingType != null ? bindingType : Library.NONE );
        }
    }
}
