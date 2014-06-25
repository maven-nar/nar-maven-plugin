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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.OutputTypeEnum;
import com.github.maven_nar.cpptasks.RuntimeType;
import com.github.maven_nar.cpptasks.SubsystemEnum;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Compiles native source files.
 * 
 * @goal nar-compile
 * @phase compile
 * @requiresSession
 * @requiresProject
 * @requiresDependencyResolution compile
 * @author Mark Donszelmann
 */
public class NarCompileMojo
    extends AbstractCompileMojo
{
    /**
     * The current build session instance.
     * 
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    @Override
    protected List/*<Artifact>*/ getArtifacts() {
        return getMavenProject().getCompileArtifacts();  // Artifact.SCOPE_COMPILE
    }

    private void copyInclude( Compiler c )
        throws IOException, MojoExecutionException, MojoFailureException
    {
        if ( c == null )
        {
            return;
        }
        c.copyIncludeFiles( getMavenProject(),
                            getLayout().getIncludeDirectory( getTargetDirectory(),
                                                             getMavenProject().getArtifactId(),
                                                             getMavenProject().getVersion() ) );
    }

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        super.narExecute();

        // make sure destination is there
        getTargetDirectory().mkdirs();

        // check for source files
        int noOfSources = 0;
        noOfSources += getSourcesFor(getCpp()).size();
        noOfSources += getSourcesFor(getC()).size();
        noOfSources += getSourcesFor(getFortran()).size();
        if ( noOfSources > 0 )
        {
            getLog().info( "Compiling " + noOfSources + " native files" );
            for ( Iterator<Library> i = getLibraries().iterator(); i.hasNext(); )
            {
                createLibrary(getAntProject(), (Library) i.next());
            }
        }
        else
        {
            getLog().info( "Nothing to compile" );
        }

        try
        {
            // FIXME, should the include paths be defined at a higher level ?
            copyInclude(getCpp());
            copyInclude(getC());
            copyInclude(getFortran());
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NAR: could not copy include files", e );
        }

        getNarInfo().writeToDirectory( classesDirectory );
    }

    private List getSourcesFor( Compiler compiler )
        throws MojoFailureException, MojoExecutionException
    {
        if ( compiler == null )
        {
            return Collections.EMPTY_LIST;
        }

        try
        {
            List files = new ArrayList();
            List srcDirs = compiler.getSourceDirectories();
            for ( Iterator i = srcDirs.iterator(); i.hasNext(); )
            {
                File dir = (File) i.next();
                if ( dir.exists() )
                {
                    files.addAll( FileUtils.getFiles( dir, StringUtils.join( compiler.getIncludes().iterator(), "," ),
                                                      null ) );
                }
            }
            return files;
        }
        catch ( IOException e )
        {
            return Collections.EMPTY_LIST;
        }
    }

    private void createLibrary(Project antProject, Library library)
        throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Creating Library " + library );
        // configure task
        CCTask task = new CCTask();
        task.setCommandLogLevel( commandLogLevel );
        task.setProject(antProject);

        task.setDecorateLinkerOptions(decorateLinkerOptions);

        task.setDecorateLinkerOptions(decorateLinkerOptions);

        // subsystem
        SubsystemEnum subSystem = new SubsystemEnum();
        subSystem.setValue( library.getSubSystem() );
        task.setSubsystem( subSystem );

        // set max cores
        task.setMaxCores(getMaxCores(getAOL()));

        // outtype
        OutputTypeEnum outTypeEnum = new OutputTypeEnum();
        String type = library.getType();
        outTypeEnum.setValue(type);
        task.setOuttype(outTypeEnum);

        // stdc++
        task.setLinkCPP(library.linkCPP());

        // fortran
        task.setLinkFortran(library.linkFortran());
        task.setLinkFortranMain( library.linkFortranMain() );

        // outDir
        File outDir;
        if ( type.equals( Library.EXECUTABLE ) )
        {
            outDir =
                getLayout().getBinDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                             getMavenProject().getVersion(), getAOL().toString() );
        }
        else
        {
            outDir =
                getLayout().getLibDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                             getMavenProject().getVersion(), getAOL().toString(), type );
        }
        outDir.mkdirs();

        // outFile
        // FIXME NAR-90 we could get the final name from layout
        File outFile = new File(outDir, getOutput(getAOL(), type) );
        getLog().debug("NAR - output: '" + outFile + "'");
        task.setOutfile(outFile);

        // object directory
        File objDir = new File(getTargetDirectory(), "obj");
        objDir = new File(objDir, getAOL().toString());
        objDir.mkdirs();
        task.setObjdir(objDir);

        // failOnError, libtool
        task.setFailonerror(failOnError(getAOL()));
        task.setLibtool(useLibtool(getAOL()));

        // runtime
        RuntimeType runtimeType = new RuntimeType();
        runtimeType.setValue(getRuntime(getAOL()));
        task.setRuntime(runtimeType);

        // IDL, MC, RC compilations should probably be 'generate source' type actions, seperate from main build.
        // Needs resolution of handling for generate sources.
        // Order is somewhat important here, IDL and MC generate outputs that are (often) included in the RC compilation
        if (getIdl() != null) {
            CompilerDef idl = getIdl().getCompiler( Compiler.MAIN, null );
            if ( idl != null )
            {
                task.addConfiguredCompiler( idl );
            }
        }
        if (getMessage() != null) {
            CompilerDef mc = getMessage().getCompiler( Compiler.MAIN, null );
            if ( mc != null )
            {
                task.addConfiguredCompiler( mc );
            }
        }
        if (getResource() != null) {
            CompilerDef res = getResource().getCompiler( Compiler.MAIN, null );
            if ( res != null )
            {
                task.addConfiguredCompiler( res );
            }
        }
        
        // Darren Sargent Feb 11 2010: Use Compiler.MAIN for "type"...appears the wrong "type" variable was being used
        // since getCompiler() expects "main" or "test", whereas the "type" variable here is "executable", "shared" etc.
        // add C++ compiler
        if (getCpp() != null) {
            CompilerDef cpp = getCpp().getCompiler( Compiler.MAIN, null );
            if ( cpp != null )
            {
                task.addConfiguredCompiler( cpp );
            }
        }

        // add C compiler
        if (getC() != null) {
            CompilerDef c = getC().getCompiler( Compiler.MAIN, null );
            if ( c != null )
            {
                task.addConfiguredCompiler( c );
            }
        }

        // add Fortran compiler
        if (getFortran() != null) {
            CompilerDef fortran = getFortran().getCompiler( Compiler.MAIN, null );
            if ( fortran != null )
            {
                task.addConfiguredCompiler( fortran );
            }
        }
        // end Darren

        // add javah include path
        File jniDirectory = getJavah().getJniDirectory();
        if (jniDirectory.exists())
        {
            task.createIncludePath().setPath(jniDirectory.getPath());
        }

        // add java include paths
        getJava().addIncludePaths(task, type);

        List<NarArtifact> dependencies = getNarArtifacts();
        // add dependency include paths
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            // FIXME, handle multiple includes from one NAR
            NarArtifact narDependency = (NarArtifact) i.next();
            String binding = narDependency.getNarInfo().getBinding(getAOL(), Library.STATIC);
            getLog().debug( "Looking for " + narDependency + " found binding " + binding);
            if ( !binding.equals(Library.JNI ) )
            {
                File unpackDirectory = getUnpackDirectory();
                File include =
                    getLayout().getIncludeDirectory( unpackDirectory, narDependency.getArtifactId(),
                                                     narDependency.getBaseVersion() );
                getLog().debug( "Looking for include directory: " + include );
                if ( include.exists() )
                {
                    task.createIncludePath().setPath(include.getPath());
                } else {
                    throw new MojoExecutionException(
                            "NAR: unable to locate include path: " + include);
                }
            }
        }

        // add linker
        LinkerDef linkerDefinition =
            getLinker().getLinker( this, antProject, getOS(), getAOL().getKey() + ".linker.", type );
        task.addConfiguredLinker(linkerDefinition);

        // add dependency libraries
        // FIXME: what about PLUGIN and STATIC, depending on STATIC, should we
        // not add all libraries, see NARPLUGIN-96
        if ( type.equals( Library.SHARED ) || type.equals( Library.JNI ) || type.equals( Library.EXECUTABLE ) )
        {

            List depLibOrder = getDependencyLibOrder();
            List depLibs = dependencies;

            // reorder the libraries that come from the nar dependencies
            // to comply with the order specified by the user
            if ( ( depLibOrder != null ) && !depLibOrder.isEmpty() )
            {
                List tmp = new LinkedList();

                for ( Iterator i = depLibOrder.iterator(); i.hasNext(); )
                {
                    String depToOrderName = (String) i.next();

                    for ( Iterator j = depLibs.iterator(); j.hasNext(); )
                    {
                        NarArtifact dep = (NarArtifact) j.next();
                        String depName = dep.getGroupId() + ":" + dep.getArtifactId();

                        if (depName.equals(depToOrderName))
                        {
                            tmp.add(dep);
                            j.remove();
                        }
                    }
                }

                tmp.addAll(depLibs);
                depLibs = tmp;
            }

            for ( Iterator i = depLibs.iterator(); i.hasNext(); )
            {
                NarArtifact dependency = (NarArtifact) i.next();

                // FIXME no handling of "local"

                // FIXME, no way to override this at this stage
                String binding = dependency.getNarInfo().getBinding( getAOL(), Library.NONE );
                getLog().debug("Using Binding: " + binding);
                AOL aol = getAOL();
                aol = dependency.getNarInfo().getAOL(getAOL());
                getLog().debug("Using Library AOL: " + aol.toString());

                if ( !binding.equals( Library.JNI ) && !binding.equals( Library.NONE ) && !binding.equals( Library.EXECUTABLE) )
                {
                    File unpackDirectory = getUnpackDirectory();

                    File dir =
                        getLayout().getLibDirectory( unpackDirectory, dependency.getArtifactId(),
                                                     dependency.getBaseVersion(), aol.toString(), binding );

                    getLog().debug("Looking for Library Directory: " + dir);
                    if ( dir.exists() )
                    {
                        LibrarySet libSet = new LibrarySet();
                        libSet.setProject(antProject);

                        // FIXME, no way to override
                        String libs = dependency.getNarInfo().getLibs(getAOL());
                        if ( ( libs != null ) && !libs.equals( "" ) )
                        {
                            getLog().debug("Using LIBS = " + libs);
                            libSet.setLibs(new CUtil.StringArrayBuilder(libs));
                            libSet.setDir(dir);
                            task.addLibset(libSet);
                        }
                    }
                    else
                    {
                        getLog().debug( "Library Directory " + dir + " does NOT exist." );
                    }

                    // FIXME, look again at this, for multiple dependencies we may need to remove duplicates
                    String options = dependency.getNarInfo().getOptions( getAOL() );
                    if ( ( options != null ) && !options.equals( "" ) )
                    {
                        getLog().debug("Using OPTIONS = " + options);
                        LinkerArgument arg = new LinkerArgument();
                        arg.setValue(options);
                        linkerDefinition.addConfiguredLinkerArg(arg);
                    }

                    String sysLibs = dependency.getNarInfo().getSysLibs( getAOL() );
                    if ( ( sysLibs != null ) && !sysLibs.equals( "" ) )
                    {
                        getLog().debug("Using SYSLIBS = " + sysLibs);
                        SystemLibrarySet sysLibSet = new SystemLibrarySet();
                        sysLibSet.setProject(antProject);

                        sysLibSet.setLibs( new CUtil.StringArrayBuilder( sysLibs ) );
                        task.addSyslibset(sysLibSet);
                    }
                }
            }
        }

        // Add JVM to linker
        getJava().addRuntime( task, getJavaHome( getAOL() ), getOS(), getAOL().getKey() + ".java." );

        // execute
        try
        {
            task.execute();
        }
        catch ( BuildException e )
        {
            throw new MojoExecutionException("NAR: Compile failed", e);
        }

        // FIXME, this should be done in CPPTasks at some point
        if ( getRuntime( getAOL() ).equals( "dynamic" ) && getOS().equals( OS.WINDOWS )
            && getLinker().getName( null, null ).equals( "msvc" ) && !getLinker().getVersion().startsWith( "6." ) )
        {
            String libType = library.getType();
            if ( libType.equals( Library.JNI ) || libType.equals( Library.SHARED ) )
            {
                String dll = outFile.getPath() + ".dll";
                String manifest = dll + ".manifest";
                int result =
                    NarUtil.runCommand( "mt.exe", new String[] { "/manifest", manifest,
                        "/outputresource:" + dll + ";#2" }, null, null, getLog() );
                if (result != 0)
                {
                    throw new MojoFailureException("MT.EXE failed with exit code: " + result);
                }
            } else if (libType.equals(Library.EXECUTABLE)) {
                String exe = outFile.getPath() + ".exe";
                String manifest = exe + ".manifest";
                int result = NarUtil.runCommand("mt.exe",
                        new String[] { "/manifest", manifest,
                                "/outputresource:" + exe + ";#1" }, null, null, getLog());
                if (result != 0)
                    throw new MojoFailureException(
                            "MT.EXE failed with exit code: " + result);
            }
        }
    }
}
