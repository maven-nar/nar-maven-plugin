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
        throws MojoExecutionException
    {
        if ( type.equals( Library.EXECUTABLE ) )
        {
            throw new MojoExecutionException(
                                              "NAR: for type EXECUTABLE call getBinDirectory instead of getLibDirectory" );
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
        if ( getNoarchDirectory( baseDir ).exists() )
        {
            attachNar( projectHelper, project, "noarch", getNoarchDirectory( baseDir ), "*/**" );
            narInfo.setNar( null, "noarch", project.getGroupId() + ":" + project.getArtifactId() + ":"
                + NarConstants.NAR_TYPE + ":" + "noarch" );
        }

        File classifierDir = getAolDirectory( baseDir );
        String[] classifier = classifierDir.list();
        for ( int i = 0; ( classifier != null ) && ( i < classifier.length ); i++ )
        {
            File dir = new File( classifierDir, classifier[i] );
            attachNar( projectHelper, project, classifier[i], dir, "*/**" );

            String type = null;
            AOL aol = null;

            File binDir = new File( dir, "bin" );
            String[] aolDir = binDir.list();
            if ( ( aolDir != null ) && aolDir.length > 0 )
            {
                type = Library.EXECUTABLE;
                aol = new AOL(aolDir[0]);
  
                if ( narInfo.getBinding( aol, null ) == null )
                {
                    narInfo.setBinding( aol, Library.EXECUTABLE );
                }
            }
            else
            {
                // look for type in aol/<aol-type>/lib/<aol>/<type>
                File libDir = new File( dir, "lib" );
                aolDir = libDir.list();
                if ( ( aolDir != null ) && aolDir.length > 0 )
                {
                    aol = new AOL(aolDir[0]);
                    String[] typeDir = new File( libDir, aol.toString() ).list();
                    if ( ( typeDir != null ) && ( typeDir.length > 0 ) )
                    {
                        type = typeDir[0];
                    }
                }
                
                assert(aol != null);
                assert(type != null);

                // and not set or override if SHARED
                if (( narInfo.getBinding( aol, null ) == null ) || type.equals( Library.SHARED ) )
                {
                    narInfo.setBinding( aol, type );
                }
            }
            
            assert(type != null);
            narInfo.setNar( null, type, project.getGroupId() + ":" + project.getArtifactId() + ":"
                            + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + type );

        }

    }
}
