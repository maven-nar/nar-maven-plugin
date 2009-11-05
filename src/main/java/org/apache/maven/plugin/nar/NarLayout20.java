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
 * Initial layout which expands a nar file into:
 * 
 * <pre>
 * nar/includue
 * nar/bin
 * nar/lib
 * </pre>
 * 
 * this layout was abandoned because there is no one-to-one relation between the nar file and its directory structure.
 * Therefore SNAPSHOTS could not be fully deleted when replaced.
 * 
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarLayout20
    extends AbstractNarLayout
{
    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarLayout#getIncludeDirectory(java.io.File)
     */
    public File getIncludeDirectory( File baseDir )
    {
        return new File( baseDir, "include" );
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarLayout#getLibDir(java.io.File, org.apache.maven.plugin.nar.AOL, String type)
     */
    public File getLibDirectory( File baseDir, String aol, String type )
    {
        if ( type.equals( Library.EXECUTABLE ) )
        {
            System.err.println( "WARNING, Replace call to getLibDirectory with getBinDirectory" );
            Thread.dumpStack();
        }

        File dir = new File( baseDir, "lib" );
        dir = new File( dir, aol.toString() );
        dir = new File( dir, type );
        return dir;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.nar.NarLayout#getBinDirectory(java.io.File, java.lang.String)
     */
    public File getBinDirectory( File baseDir, String aol )
    {
        File dir = new File( baseDir, "bin" );
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
        if ( getIncludeDirectory( baseDir ).exists() )
        {
            attachNar( projectHelper, project, "noarch", baseDir, "include/**" );
            narInfo.setNar( null, "noarch", project.getGroupId() + ":" + project.getArtifactId() + ":"
                + NarConstants.NAR_TYPE + ":" + "noarch" );
        }

        String[] binAOL = new File( baseDir, "bin" ).list();
        for ( int i = 0; ( binAOL != null ) && ( i < binAOL.length ); i++ )
        {
            attachNar( projectHelper, project, binAOL[i] + "-" + Library.EXECUTABLE, baseDir, "bin/" + binAOL[i]
                + "/**" );
            narInfo.setNar( null, Library.EXECUTABLE, project.getGroupId() + ":" + project.getArtifactId() + ":"
                + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + Library.EXECUTABLE );
        }

        File libDir = new File( baseDir, "lib" );
        String[] libAOL = libDir.list();
        for ( int i = 0; ( libAOL != null ) && ( i < libAOL.length ); i++ )
        {
            String bindingType = null;
            String[] libType = new File( libDir, libAOL[i] ).list();
            for ( int j = 0; ( libType != null ) && ( j < libType.length ); j++ )
            {
                attachNar( projectHelper, project, libAOL[i] + "-" + libType[j], baseDir, "lib/" + libAOL[i] + "/"
                    + libType[j] + "/**" );
                narInfo.setNar( null, libType[j], project.getGroupId() + ":" + project.getArtifactId() + ":"
                    + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + libType[j] );

                // set if not set or override if SHARED
                if ( ( bindingType == null ) || libType[j].equals( Library.SHARED ) )
                {
                    bindingType = libType[j];
                }
            }

            if ( narInfo.getBinding( null, null ) == null )
            {
                narInfo.setBinding( null, bindingType != null ? bindingType : Library.NONE );
            }
        }
    }
}
