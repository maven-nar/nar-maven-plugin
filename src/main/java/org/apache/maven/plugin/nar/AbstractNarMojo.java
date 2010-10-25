package org.apache.maven.plugin.nar;

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

import java.io.File;
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
     * @parameter expression="${nar.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Ignore errors and failures.
     * 
     * @parameter expression="${nar.ignore}" default-value="false"
     */
    private boolean ignore;

    /**
     * The Architecture for the nar, Some choices are: "x86", "i386", "amd64", "ppc", "sparc", ... Defaults to a derived
     * value from ${os.arch}
     * 
     * @parameter expression="${os.arch}"
     * @required
     */
    private String architecture;

    /**
     * The Operating System for the nar. Some choices are: "Windows", "Linux", "MacOSX", "SunOS", ... Defaults to a
     * derived value from ${os.name} FIXME table missing
     * 
     * @parameter expression=""
     */
    private String os;

    /**
     * Architecture-OS-Linker name. Defaults to: arch-os-linker.
     * 
     * @parameter expression=""
     */
    private String aol;

    /**
     * Linker
     * 
     * @parameter expression=""
     */
    private Linker linker;

    /**
     * @parameter expression="${project.build.directory}"
     * @readonly
     */
    private File outputDirectory;

    /**
     * @parameter expression="${project.basedir}"
     * @readonly
     */
    private File baseDir;

    /**
     * @parameter expression="${project.build.finalName}"
     * @readonly
     */
    private String finalName;

    /**
     * Target directory for Nar file construction. Defaults to "${project.build.directory}/nar" for "nar-compile" goal
     * 
     * @parameter expression=""
     */
    private File targetDirectory;

    /**
     * Target directory for Nar test construction. Defaults to "${project.build.directory}/test-nar" for "nar-testCompile" goal
     * 
     * @parameter expression=""
     */
    private File testTargetDirectory;

    /**
     * Target directory for Nar file unpacking. Defaults to "${targetDirectory}"
     * 
     * @parameter expression=""
     */
    private File unpackDirectory;

    /**
     * Target directory for Nar test unpacking. Defaults to "${testTargetDirectory}"
     * 
     * @parameter expression=""
     */
    private File testUnpackDirectory;

    /**
     * Layout to be used for building and unpacking artifacts
     * 
     * @parameter expression="${nar.layout}" default-value="org.apache.maven.plugin.nar.NarLayout21"
     * @required
     */
    private String layout;
    
    private NarLayout narLayout;

    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject mavenProject;

    private AOL aolId;

    protected final void validate()
        throws MojoFailureException, MojoExecutionException
    {
        linker = NarUtil.getLinker( linker );

        architecture = NarUtil.getArchitecture( architecture );
        os = NarUtil.getOS( os );
        aolId = NarUtil.getAOL(mavenProject, architecture, os, linker, aol );
        
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

    protected final String getFinalName()
    {
        return finalName;
    }

    protected final File getTargetDirectory()
    {
        return targetDirectory;
    }
    protected final File getTestTargetDirectory()
    {
        return testTargetDirectory;
    }

    protected final File getUnpackDirectory()
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
}
