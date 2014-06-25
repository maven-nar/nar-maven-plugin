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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractNarMojo
    extends AbstractMojo
    implements NarConstants
{

    /**
     * Skip running of NAR plugins (any) altogether.
     * 
     * @parameter property="nar.skip" default-value="false"
     */
    private boolean skip;

    /**
     * Skip the tests. Listens to Maven's general 'maven.skip.test'.
     * 
     * @parameter property="maven.test.skip"
     */
    boolean skipTests;

    /**
     * Ignore errors and failures.
     * 
     * @parameter property="nar.ignore" default-value="false"
     */
    private boolean ignore;

    /**
     * The Architecture for the nar, Some choices are: "x86", "i386", "amd64", "ppc", "sparc", ... Defaults to a derived
     * value from ${os.arch}
     * 
     * @parameter property="nar.arch"
     */
    private String architecture;

    /**
     * The Operating System for the nar. Some choices are: "Windows", "Linux", "MacOSX", "SunOS", ... Defaults to a
     * derived value from ${os.name} FIXME table missing
     * 
     * @parameter property="nar.os"
     */
    private String os;

    /**
     * Architecture-OS-Linker name. Defaults to: arch-os-linker.
     * 
     * @parameter default-value=""
     */
    private String aol;

    /**
     * Linker
     * 
     * @parameter default-value=""
     */
    private Linker linker;

    /**
     * @parameter property="project.build.directory"
     * @readonly
     */
    private File outputDirectory;

    /**
     * @parameter property="project.build.outputDirectory"
     * @readonly
     */
    protected File classesDirectory;

    /**
     * Name of the output
     *  - for jni default-value="${project.artifactId}-${project.version}"
     *  - for libs default-value="${project.artifactId}-${project.version}"
     *  - for exe default-value="${project.artifactId}"
     *  -- for tests default-value="${test.name}"
     * 
     * @parameter 
     */
    private String output;

    /**
     * @parameter property="project.basedir"
     * @readonly
     */
    private File baseDir;

    /**
     * Target directory for Nar file construction. Defaults to "${project.build.directory}/nar" for "nar-compile" goal
     * 
     * @parameter default-value=""
     */
    private File targetDirectory;

    /**
     * Target directory for Nar test construction. Defaults to "${project.build.directory}/test-nar" for "nar-testCompile" goal
     * 
     * @parameter default-value=""
     */
    private File testTargetDirectory;

    /**
     * Target directory for Nar file unpacking. Defaults to "${targetDirectory}"
     * 
     * @parameter default-value=""
     */
    private File unpackDirectory;

    /**
     * Target directory for Nar test unpacking. Defaults to "${testTargetDirectory}"
     * 
     * @parameter default-value=""
     */
    private File testUnpackDirectory;

    /**
     * List of classifiers which you want download/unpack/assemble 
     * Example ppc-MacOSX-g++, x86-Windows-msvc, i386-Linux-g++.
     * Not setting means all.
     * 
     * @parameter default-value=""
     */
    protected List<String> classifiers;

    /**
     * List of libraries to create
     *
     * @parameter default-value=""
     */
    protected List<Library> libraries;

    /**
     * Layout to be used for building and unpacking artifacts
     * 
     * @parameter property="nar.layout" default-value="com.github.maven_nar.NarLayout21"
     * @required
     */
    private String layout;

    private NarLayout narLayout;

    /**
     * @parameter property="project"
     * @readonly
     * @required
     */
    private MavenProject mavenProject;

    private AOL aolId;

    private NarInfo narInfo;

    /**
     * Javah info
     * 
     * @parameter default-value=""
     */
    private Javah javah;

    /**
     * The home of the Java system. Defaults to a derived value from ${java.home} which is OS specific.
     * 
     * @parameter default-value=""
     * @readonly
     */
    private File javaHome;

    protected final void validate()
        throws MojoFailureException, MojoExecutionException
    {
        linker = NarUtil.getLinker( linker, getLog() );

        architecture = NarUtil.getArchitecture( architecture );
        os = NarUtil.getOS( os );
        aolId = NarUtil.getAOL(mavenProject, architecture, os, linker, aol, getLog() );

        Model model = mavenProject.getModel();
        Properties properties = model.getProperties();
        properties.setProperty("nar.arch", getArchitecture());
        properties.setProperty("nar.os", getOS());
        properties.setProperty("nar.linker", getLinker().getName());
        properties.setProperty("nar.aol", aolId.toString());
        properties.setProperty("nar.aol.key", aolId.getKey());
        model.setProperties(properties);

        if ( targetDirectory == null )
        {
            targetDirectory = new File( mavenProject.getBuild().getDirectory(), "nar" );
        }
        if ( testTargetDirectory == null )
        {
            testTargetDirectory = new File( mavenProject.getBuild().getDirectory(), "test-nar" );
        }

        if ( unpackDirectory == null )
        {
            unpackDirectory = targetDirectory;
        }
        if ( testUnpackDirectory == null )
        {
            testUnpackDirectory = testTargetDirectory;
        }
    }

    protected final String getOutput( boolean versioned )
        throws MojoExecutionException
    {
        if( output != null && !output.trim().isEmpty()){
            return output;
        } else {
            if( versioned )
                return getMavenProject().getArtifactId() + "-" + getMavenProject().getVersion();
            else
                return getMavenProject().getArtifactId();
        }
    }

    protected final String getArchitecture()
    {
        return architecture;
    }

    protected final String getOS()
    {
        return os;
    }

    protected final AOL getAOL()
        throws MojoFailureException, MojoExecutionException
    {
        return aolId;
    }

    protected final Linker getLinker()
    {
        return linker;
    }

    protected final File getBasedir()
    {
        return baseDir;
    }

    protected final File getOutputDirectory()
    {
        return outputDirectory;
    }

    protected final File getTargetDirectory()
    {
        return targetDirectory;
    }
    protected final File getTestTargetDirectory()
    {
        return testTargetDirectory;
    }

    protected File getUnpackDirectory()
    {
        return unpackDirectory;
    }

    protected final File getTestUnpackDirectory()
    {
        return testUnpackDirectory;
    }

    protected final NarLayout getLayout()
        throws MojoExecutionException
    {
        if ( narLayout == null )
        {
            narLayout =
                AbstractNarLayout.getLayout( layout, getLog() );
        }
        return narLayout;
    }

    protected final MavenProject getMavenProject()
    {
        return mavenProject;
    }

    public final void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( getClass().getName() + " skipped" );
            return;
        }

        try
        {
            validate();
            narExecute();
        }
        catch ( MojoFailureException mfe )
        {
            if ( ignore )
            {
                getLog().warn( "IGNORED: " + mfe.getMessage() );
            }
            else
            {
                throw mfe;
            }
        }
        catch ( MojoExecutionException mee )
        {
            if ( ignore )
            {
                getLog().warn( "IGNORED: " + mee.getMessage() );
            }
            else
            {
                throw mee;
            }
        }
    }

    public abstract void narExecute()
        throws MojoFailureException, MojoExecutionException;

    protected NarInfo getNarInfo() throws MojoExecutionException {
        if ( narInfo == null )
        {
            String groupId = getMavenProject().getGroupId();
            String artifactId = getMavenProject().getArtifactId();
            String path = "META-INF/nar/" + groupId + "/" + artifactId + "/" + NarInfo.NAR_PROPERTIES;
            File propertiesFile = new File( classesDirectory, path );
            // should not need to try and read from source.
            if( !propertiesFile.exists() ){
                propertiesFile = new File( getMavenProject().getBasedir(), "src/main/resources/" + path);
            }

            narInfo = new NarInfo(
                    groupId, artifactId,
                    getMavenProject().getVersion(),
                    getLog(),
                    propertiesFile );
        }
        return narInfo;
    }

    protected final List<Library> getLibraries() {
        if ( libraries == null )
        {
            libraries = Collections.EMPTY_LIST;
        }
        return libraries;
    }

    protected final Javah getJavah() {
        if ( javah == null )
        {
            javah = new Javah();
        }
        javah.setAbstractCompileMojo( this );
        return javah;
    }

    protected final File getJavaHome(AOL aol)
        throws MojoExecutionException {
        // FIXME should be easier by specifying default...
        return getNarInfo().getProperty( aol, "javaHome", NarUtil.getJavaHome( javaHome, getOS() ) );
    }
}
