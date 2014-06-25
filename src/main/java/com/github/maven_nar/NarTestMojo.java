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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Tests NAR files. Runs Native Tests and executables if produced.
 * 
 * @goal nar-test
 * @phase test
 * @requiresProject
 * @requiresDependencyResolution test
 * @author Mark Donszelmann
 */
public class NarTestMojo
    extends AbstractCompileMojo
{
    /**
     * The classpath elements of the project being tested.
     * 
     * @parameter default-value="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * Directory for test resources. Defaults to src/test/resources
     * 
     * @parameter default-value="${basedir}/src/test/resources"
     * @required
     */
    private File testResourceDirectory;

    @Override
    protected List/*<Artifact>*/ getArtifacts() {
        return getMavenProject().getTestArtifacts();  // Artifact.SCOPE_TEST
    }

    protected File getUnpackDirectory()
    {
        return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
    }

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skipTests )
        {
            getLog().info( "Tests are skipped" );
        }
        else
        {
            super.narExecute();
            // run all tests
            for ( Iterator i = getTests().iterator(); i.hasNext(); )
            {
                runTest( (Test) i.next() );
            }

            for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
            {
                runExecutable( (Library) i.next() );
            }
        }
    }

    private void runTest( Test test )
        throws MojoExecutionException, MojoFailureException
    {
        // run if requested
        if ( test.shouldRun() )
        {
            // NOTE should we use layout here ?
            String name = test.getName() + (getOS().equals( OS.WINDOWS ) ? ".exe" : "");
            File path = new File( getTestTargetDirectory(), "bin" );
            path = new File( path, getAOL().toString() );
            path = new File( path, name );
            if ( !path.exists() )
            {
                getLog().warn( "Skipping non-existing test " + path );
                return;
            }

            File workingDir = new File( getTestTargetDirectory(), "test-reports" );
            workingDir.mkdirs();

            // Copy test resources
            try
            {
                int copied = 0;
                if ( testResourceDirectory.exists() )
                {
                    copied += NarUtil.copyDirectoryStructure( testResourceDirectory, workingDir, null, NarUtil.DEFAULT_EXCLUDES );
                }
                getLog().info( "Copied " + copied + " test resources" );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "NAR: Could not copy test resources", e );
            }

            getLog().info( "Running test " + name + " in " + workingDir );

            List args = test.getArgs();
            int result =
                NarUtil.runCommand( path.toString(), (String[]) args.toArray( new String[args.size()] ), workingDir,
                                    generateEnvironment(), getLog() );
            if ( result != 0 )
            {
                throw new MojoFailureException( "Test " + name + " failed with exit code: " + result + " 0x"
                    + Integer.toHexString( result ) );
            }
        }
    }

    private void runExecutable( Library library )
        throws MojoExecutionException, MojoFailureException
    {
        if ( library.getType().equals( Library.EXECUTABLE ) && library.shouldRun() )
        {
            MavenProject project = getMavenProject();
            // FIXME NAR-90, we could make sure we get the final name from layout
            String extension = getOS().equals( OS.WINDOWS ) ? ".exe" : "";
            File executable =
                new File( getLayout().getBinDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                                       getMavenProject().getVersion(), getAOL().toString() ),
                          project.getArtifactId() + extension );
            if ( !executable.exists() )
            {
                getLog().warn( "Skipping non-existing executable " + executable );
                return;
            }
            getLog().info( "Running executable " + executable );
            List args = library.getArgs();
            int result =
                NarUtil.runCommand( executable.getPath(), (String[]) args.toArray( new String[args.size()] ), null,
                                    generateEnvironment(), getLog() );
            if ( result != 0 )
            {
                throw new MojoFailureException( "Test " + executable + " failed with exit code: " + result + " 0x"
                    + Integer.toHexString( result ) );
            }
        }
    }

    private String[] generateEnvironment()
        throws MojoExecutionException, MojoFailureException
    {
        List env = new ArrayList();

        Set/* <File> */sharedPaths = new HashSet();

        // add all shared libraries of this package
        for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
        {
            Library lib = (Library) i.next();
            if ( lib.getType().equals( Library.SHARED ) )
            {
                File path =
                    getLayout().getLibDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                                 getMavenProject().getVersion(), getAOL().toString(), lib.getType() );
                getLog().debug( "Adding path to shared library: " + path );
                sharedPaths.add( path );
            }
        }

        // add dependent shared libraries
        String classifier = getAOL() + "-shared";
        List narArtifacts = getNarArtifacts();
        List dependencies = getNarManager().getAttachedNarDependencies( narArtifacts, classifier );
        for ( Iterator d = dependencies.iterator(); d.hasNext(); )
        {
            Artifact dependency = (Artifact) d.next();
            getLog().debug( "Looking for dependency " + dependency );

            // FIXME reported to maven developer list, isSnapshot
            // changes behaviour
            // of getBaseVersion, called in pathOf.
            dependency.isSnapshot();

            File libDirectory =
                getLayout().getLibDirectory( getUnpackDirectory(), dependency.getArtifactId(), dependency.getBaseVersion(),
                                             getAOL().toString(), Library.SHARED );
            sharedPaths.add( libDirectory );
        }

        // set environment
        if ( sharedPaths.size() > 0 )
        {
            String sharedPath = "";
            for ( Iterator i = sharedPaths.iterator(); i.hasNext(); )
            {
                sharedPath += ( (File) i.next() ).getPath();
                if ( i.hasNext() )
                {
                    sharedPath += File.pathSeparator;
                }
            }

            String sharedEnv = NarUtil.addLibraryPathToEnv( sharedPath, null, getOS() );
            env.add( sharedEnv );
        }

        // necessary to find WinSxS
        if ( getOS().equals( OS.WINDOWS ) )
        {
            env.add( "SystemRoot=" + NarUtil.getEnv( "SystemRoot", "SystemRoot", "C:\\Windows" ) );
        }

        // add CLASSPATH
        env.add( "CLASSPATH=" + StringUtils.join( classpathElements.iterator(), File.pathSeparator ) );

        return env.size() > 0 ? (String[]) env.toArray( new String[env.size()] ) : null;
    }
}
