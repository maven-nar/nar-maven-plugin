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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.LinkerEnum;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.plugin.logging.Log;

/**
 * Linker tag
 * 
 * @author Mark Donszelmann
 */
public class Linker
{

    /**
     * The Linker Some choices are: "msvc", "g++", "CC", "icpc", ... Default is Architecture-OS-Linker specific: FIXME:
     * table missing
     * 
     * @parameter default-value=""
     */
    private String name;

    /**
     * Path location of the linker tool
     *
     * @parameter default-value=""
     */
    private String toolPath;

    /**
     * Enables or disables incremental linking.
     * 
     * @parameter default-value="false"
     * @required
     */
    private boolean incremental = false;

    /**
     * Enables or disables the production of a map file.
     * 
     * @parameter default-value="false"
     * @required
     */
    private boolean map = false;

    /**
     * Options for the linker Defaults to Architecture-OS-Linker specific values. FIXME table missing
     * 
     * @parameter default-value=""
     */
    private List options;

    /**
     * Additional options for the linker when running in the nar-testCompile phase.
     * 
     * @parameter default-value=""
     */
    private List testOptions;

    /**
     * Options for the linker as a whitespace separated list. Defaults to Architecture-OS-Linker specific values. Will
     * work in combination with &lt;options&gt;.
     * 
     * @parameter default-value=""
     */
    private String optionSet;

    /**
     * Clears default options
     * 
     * @parameter default-value="false"
     * @required
     */
    private boolean clearDefaultOptions;

    /**
     * Adds libraries to the linker.
     * 
     * @parameter default-value=""
     */
    private List/* <Lib> */libs;

    /**
     * Adds libraries to the linker. Will work in combination with &lt;libs&gt;. The format is comma separated,
     * colon-delimited values (name:type:dir), like "myLib:shared:/home/me/libs/, otherLib:static:/some/path".
     * 
     * @parameter default-value=""
     */
    private String libSet;

    /**
     * Adds system libraries to the linker.
     * 
     * @parameter default-value=""
     */
    private List/* <SysLib> */sysLibs;

    /**
     * Adds system libraries to the linker. Will work in combination with &lt;sysLibs&gt;. The format is comma
     * separated, colon-delimited values (name:type), like "dl:shared, pthread:shared".
     * 
     * @parameter default-value=""
     */
    private String sysLibSet;

    /**
     * <p>
     * Specifies the link ordering of libraries that come from nar dependencies. The format is a comma separated list of
     * dependency names, given as groupId:artifactId.
     * </p>
     * <p>
     * Example: &lt;narDependencyLibOrder&gt;someGroup:myProduct, other.group:productB&lt;narDependencyLibOrder&gt;
     * </p>
     * 
     * @parameter default-value=""
     */
    private String narDependencyLibOrder;

    private final Log log;

    public Linker()
    {
        // default constructor for use as TAG
        this( null );
    }

    public Linker( final Log log )
    {
        this.log = log;
    }

    /**
     * For use with specific named linker.
     * 
     * @param name
     */
    public Linker( String name, final Log log )
    {
        this.name = name;
        this.log = log;
    }

    public final String getName()
    {
        return name;
    }

    public final String getName( NarProperties properties, String prefix )
        throws MojoFailureException, MojoExecutionException
    {
        if ( ( name == null ) && ( properties != null ) && ( prefix != null ) )
        {
            name = properties.getProperty( prefix + "linker" );
        }
        if ( name == null )
        {
            throw new MojoExecutionException( "NAR: One of two things may be wrong here:\n\n"
                + "1. <Name> tag is missing inside the <Linker> tag of your NAR configuration\n\n"
                + "2. no linker is defined in the aol.properties file for '" + prefix + "linker'\n" );
        }
        return name;
    }

    public final String getVersion() 
        throws MojoFailureException, MojoExecutionException
    {
        if ( name == null )
        {
            throw new MojoFailureException( "Cannot deduce linker version if name is null" );
        }

        String version = null;

        TextStream out = new StringTextStream();
        TextStream err = new StringTextStream();
        TextStream dbg = new StringTextStream();

        if ( name.equals( "g++" ) || name.equals( "gcc" ) )
        {
            NarUtil.runCommand( "gcc", new String[] { "--version" }, null, null, out, err, dbg, log );
            Pattern p = Pattern.compile( "\\d+\\.\\d+\\.\\d+" );
            Matcher m = p.matcher( out.toString() );
            if ( m.find() )
            {
                version = m.group( 0 );
            }
        }
        else if ( name.equals( "msvc" ) )
        {
            NarUtil.runCommand( "link", new String[] { "/?" }, null, null, out, err, dbg, log, true );
            Pattern p = Pattern.compile( "\\d+\\.\\d+\\.\\d+(\\.\\d+)?" );
            Matcher m = p.matcher( out.toString() );
            if ( m.find() )
            {
                version = m.group( 0 );
            }
        }
        else if ( name.equals( "icc" ) || name.equals( "icpc" ) )
        {
            NarUtil.runCommand( "icc", new String[] { "--version" }, null, null, out, err, dbg, log );
            Pattern p = Pattern.compile( "\\d+\\.\\d+" );
            Matcher m = p.matcher( out.toString() );
            if ( m.find() )
            {
                version = m.group( 0 );
            }
        }
        else if ( name.equals( "icl" ) )
        {
            NarUtil.runCommand( "icl", new String[] { "/QV" }, null, null, out, err, dbg, log );
            Pattern p = Pattern.compile( "\\d+\\.\\d+" );
            Matcher m = p.matcher( err.toString() );
            if ( m.find() )
            {
                version = m.group( 0 );
            }
        }
        else if ( name.equals( "CC" ) )
        {
        	NarUtil.runCommand( "CC", new String[] { "-V" }, null, null, out, err, dbg, log );
        	Pattern p = Pattern.compile( "\\d+\\.d+" );
        	Matcher m = p.matcher( err.toString() );
        	if ( m.find() )
        	{ 
        		version = m.group( 0 ); 
        	}
        }
        else
        {
            throw new MojoFailureException( "Cannot find version number for linker '" + name + "'" );
        }
        
        if (version == null) {
        	throw new MojoFailureException( "Cannot deduce version number from: " + out.toString() );
        }
        return version;
    }

    /**
     * @return The standard Linker configuration with 'testOptions' added to the argument list.
     */
    public final LinkerDef getTestLinker( AbstractCompileMojo mojo, Project antProject, String os, String prefix,
                                          String type )
        throws MojoFailureException, MojoExecutionException
    {
        LinkerDef linker = getLinker(mojo, antProject, os, prefix, type);
        if ( testOptions != null )
        {
            for ( Iterator i = testOptions.iterator(); i.hasNext(); )
            {
                LinkerArgument arg = new LinkerArgument();
                arg.setValue( (String) i.next() );
                linker.addConfiguredLinkerArg( arg );
            }
        }
        return linker;
    }

    public final LinkerDef getLinker( AbstractCompileMojo mojo, Project antProject, String os, String prefix,
                                      String type )
        throws MojoFailureException, MojoExecutionException
    {
        if ( name == null )
        {
            throw new MojoFailureException( "NAR: Please specify a <Name> as part of <Linker>" );
        }

        LinkerDef linker = new LinkerDef();
        linker.setProject( antProject );
        LinkerEnum linkerEnum = new LinkerEnum();
        linkerEnum.setValue( name );
        linker.setName( linkerEnum );

        // tool path
        if ( toolPath != null )
        {
            linker.setToolPath( toolPath );
        }

        // incremental, map
        linker.setIncremental( incremental );
        linker.setMap( map );

        // Add definitions (Window only)
        if ( os.equals( OS.WINDOWS ) && getName( null, null ).equals( "msvc" ) && ( type.equals( Library.SHARED ) || type.equals( Library.JNI ) ) )
        {
            Set defs = new HashSet();
            try
            {
                if ( mojo.getC() != null )
                {
                    List cSrcDirs = mojo.getC().getSourceDirectories();
                    for ( Iterator i = cSrcDirs.iterator(); i.hasNext(); )
                    {
                        File dir = (File) i.next();
                        if ( dir.exists() )
                        {
                            defs.addAll( FileUtils.getFiles( dir, "**/*.def", null ) );
                        }
                    }
                }
            }
            catch ( IOException e )
            {
            }
            try
            {
                if ( mojo.getCpp() != null )
                {
                    List cppSrcDirs = mojo.getCpp().getSourceDirectories();
                    for ( Iterator i = cppSrcDirs.iterator(); i.hasNext(); )
                    {
                        File dir = (File) i.next();
                        if ( dir.exists() )
                        {
                            defs.addAll( FileUtils.getFiles( dir, "**/*.def", null ) );
                        }
                    }
                }
            }
            catch ( IOException e )
            {
            }
            try
            {
                if ( mojo.getFortran() != null )
                {
                    List fortranSrcDirs = mojo.getFortran().getSourceDirectories();
                    for ( Iterator i = fortranSrcDirs.iterator(); i.hasNext(); )
                    {
                        File dir = (File) i.next();
                        if ( dir.exists() )
                        {
                            defs.addAll( FileUtils.getFiles( dir, "**/*.def", null ) );
                        }
                    }
                }
            }
            catch ( IOException e )
            {
            }

            for ( Iterator i = defs.iterator(); i.hasNext(); )
            {
                LinkerArgument arg = new LinkerArgument();
                arg.setValue( "/def:" + i.next() );
                linker.addConfiguredLinkerArg( arg );
            }
        }

        // FIXME, this should be done in CPPTasks at some point, and may not be necessary, but was for VS 2010 beta 2
        if ( os.equals( OS.WINDOWS ) && getName( null, null ).equals( "msvc" ) && !getVersion().startsWith( "6." ) )
        {
            LinkerArgument arg = new LinkerArgument();
            arg.setValue( "/MANIFEST" );
            linker.addConfiguredLinkerArg( arg );
        }

        // Add options to linker
        if ( options != null )
        {
            for ( Iterator i = options.iterator(); i.hasNext(); )
            {
                LinkerArgument arg = new LinkerArgument();
                arg.setValue( (String) i.next() );
                linker.addConfiguredLinkerArg( arg );
            }
        }

        if ( optionSet != null )
        {

            String[] opts = optionSet.split( "\\s" );

            for ( int i = 0; i < opts.length; i++ )
            {

                LinkerArgument arg = new LinkerArgument();

                arg.setValue( opts[i] );
                linker.addConfiguredLinkerArg( arg );
            }
        }

        if ( !clearDefaultOptions )
        {
            String option = NarProperties.getInstance(mojo.getMavenProject()).getProperty( prefix + "options" );
            if ( option != null )
            {
                String[] opt = option.split( " " );
                for ( int i = 0; i < opt.length; i++ )
                {
                    LinkerArgument arg = new LinkerArgument();
                    arg.setValue( opt[i] );
                    linker.addConfiguredLinkerArg( arg );
                }
            }
        }

        // record the preference for nar dependency library link order
        if ( narDependencyLibOrder != null )
        {

            List libOrder = new LinkedList();

            String[] lib = narDependencyLibOrder.split( "," );

            for ( int i = 0; i < lib.length; i++ )
            {
                libOrder.add( lib[i].trim() );
            }

            mojo.setDependencyLibOrder( libOrder );
        }

        // Add Libraries to linker
        if ( ( libs != null ) || ( libSet != null ) )
        {

            if ( libs != null )
            {

                for ( Iterator i = libs.iterator(); i.hasNext(); )
                {

                    Lib lib = (Lib) i.next();
                    lib.addLibSet( mojo, linker, antProject );
                }
            }

            if ( libSet != null )
            {
                addLibraries( libSet, linker, antProject, false );
            }
        }
        else
        {

            String libsList = NarProperties.getInstance(mojo.getMavenProject()).getProperty( prefix + "libs" );

            addLibraries( libsList, linker, antProject, false );
        }

        // Add System Libraries to linker
        if ( ( sysLibs != null ) || ( sysLibSet != null ) )
        {

            if ( sysLibs != null )
            {

                for ( Iterator i = sysLibs.iterator(); i.hasNext(); )
                {

                    SysLib sysLib = (SysLib) i.next();
                    linker.addSyslibset( sysLib.getSysLibSet( antProject ) );
                }
            }

            if ( sysLibSet != null )
            {
                addLibraries( sysLibSet, linker, antProject, true );
            }
        }
        else
        {

            String sysLibsList = NarProperties.getInstance(mojo.getMavenProject()).getProperty( prefix + "sysLibs" );

            addLibraries( sysLibsList, linker, antProject, true );
        }

        return linker;
    }

    private void addLibraries( String libraryList, LinkerDef linker, Project antProject, boolean isSystem )
    {

        if ( libraryList == null )
        {
            return;
        }

        String[] lib = libraryList.split( "," );

        for ( int i = 0; i < lib.length; i++ )
        {

            String[] libInfo = lib[i].trim().split( ":", 3 );

            LibrarySet librarySet = new LibrarySet();

            if ( isSystem )
            {
                librarySet = new SystemLibrarySet();
            }

            librarySet.setProject( antProject );
            librarySet.setLibs( new CUtil.StringArrayBuilder( libInfo[0] ) );

            if ( libInfo.length > 1 )
            {

                LibraryTypeEnum libType = new LibraryTypeEnum();

                libType.setValue( libInfo[1] );
                librarySet.setType( libType );

                if ( !isSystem && ( libInfo.length > 2 ) )
                {
                    librarySet.setDir( new File( libInfo[2] ) );
                }
            }

            if ( !isSystem )
            {
                linker.addLibset( librarySet );
            }
            else
            {
                linker.addSyslibset( (SystemLibrarySet) librarySet );
            }
        }
    }
}
