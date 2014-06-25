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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @author Mark Donszelmann
 */
public final class NarUtil
{
    private NarUtil()
    {
        // never instantiate
    }

    public static String getOS( String defaultOs )
    {
        String os = defaultOs;
        // adjust OS if not given
        if ( os == null )
        {
            os = System.getProperty( "os.name" );
            String name = os.toLowerCase();
            if ( name.startsWith( "windows" ) )
            {
                os = OS.WINDOWS;
            }
            if ( name.startsWith( "linux" ) )
            {
                os = OS.LINUX;
            }
            if ( name.startsWith( "freebsd" ) )
            {
                os = OS.FREEBSD;
            }
            if ( name.equals( "mac os x" ) )
            {
                os = OS.MACOSX;
            }
        }
        return os;
    }

    public static String getArchitecture( String architecture )
    {
        if (architecture == null) {
            return System.getProperty( "os.arch" );
        }
        return architecture;
    }

    public static Linker getLinker( Linker linker, final Log log )
    {
        Linker link = linker;
        if ( link == null )
        {
            link = new Linker( log );
        }
        return link;
    }

    public static String getLinkerName(MavenProject project, String architecture, String os, Linker linker, final Log log )
        throws MojoFailureException, MojoExecutionException
    {
        return getLinker( linker, log ).getName( NarProperties.getInstance(project), getArchitecture( architecture ) + "." + getOS( os ) + "." );
    }

    public static AOL getAOL(MavenProject project, String architecture, String os, Linker linker, String aol, final Log log )
        throws MojoFailureException, MojoExecutionException
    {
        // adjust aol
        return aol == null ? new AOL( getArchitecture( architecture ), getOS( os ), getLinkerName( project, architecture, os,
                                                                                                   linker, log ) )
                        : new AOL( aol );
    }

    // FIXME, should go to AOL.
/* NOT USED ?
    public static String getAOLKey( String architecture, String os, Linker linker )
        throws MojoFailureException, MojoExecutionException
    {
        // construct AOL key prefix
        return getArchitecture( architecture ) + "." + getOS( os ) + "." + getLinkerName( architecture, os, linker )
            + ".";
    }
*/

    public static String getAOLKey( String aol )
    {
        // FIXME, this may not always work correctly
        return replace( "-", ".", aol );
    }

    public static File getJavaHome( File javaHome, String os )
    {
        File home = javaHome;
        // adjust JavaHome
        if ( home == null )
        {
            home = new File( System.getProperty( "java.home" ) );
            if ( home.getName().equals("jre") )
            {
                // we want the JDK base directory, not the JRE subfolder
                home = home.getParentFile();
            }
        }
        return home;
    }

    public static void makeExecutable( File file, final Log log )
        throws MojoExecutionException, MojoFailureException
    {
        if ( !file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            File[] files = file.listFiles();
            for ( int i = 0; i < files.length; i++ )
            {
                makeExecutable( files[i], log );
            }
        }
        if ( file.isFile() && file.canRead() && file.canWrite() && !file.isHidden() )
        {
            // chmod +x file
            int result = runCommand( "chmod", new String[] { "+x", file.getPath() }, null, null, log );
            if ( result != 0 )
            {
                throw new MojoExecutionException( "Failed to execute 'chmod +x " + file.getPath() + "'"
                    + " return code: \'" + result + "\'." );
            }
        }
    }

    public static void runRanlib( File file, final Log log )
        throws MojoExecutionException, MojoFailureException
    {
        if ( !file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            File[] files = file.listFiles();
            for ( int i = 0; i < files.length; i++ )
            {
                runRanlib( files[i], log );
            }
        }
        if ( file.isFile() && file.canWrite() && !file.isHidden() && file.getName().endsWith( ".a" ) )
        {
            // ranlib file
            int result = runCommand( "ranlib", new String[] { file.getPath() }, null, null, log );
            if ( result != 0 )
            {
                throw new MojoExecutionException( "Failed to execute 'ranlib " + file.getPath() + "'"
                    + " return code: \'" + result + "\'." );
            }
        }
    }

    static void runInstallNameTool( File[] files, final Log log )
        throws MojoExecutionException, MojoFailureException
    {
        Set libs = findInstallNameToolCandidates( files, log );

        for ( Iterator i = libs.iterator(); i.hasNext(); )
        {
            File subjectFile = ( File )i.next();
            String subjectName = subjectFile.getName();
            String subjectPath = subjectFile.getPath();

            int idResult = runCommand(
                    "install_name_tool",
                    new String[] { "-id", subjectPath, subjectPath },
                    null, null, log );

            if ( idResult != 0 )
            {
                throw new MojoExecutionException(
                    "Failed to execute 'install_name_tool -id "
                    + subjectPath
                    + " "
                    + subjectPath
                    + "'"
                    + " return code: \'"
                    + idResult
                    + "\'." );
             }

             for ( Iterator j = libs.iterator(); j.hasNext(); )
             {
                 File dependentFile = ( File )j.next();
                 String dependentPath = dependentFile.getPath();

                 if (dependentPath == subjectPath) continue;

                 int changeResult = runCommand(
                        "install_name_tool",
                         new String[] { "-change", subjectName, subjectPath, dependentPath },
                         null, null, log );

                 if ( changeResult != 0 )
                 {
                     throw new MojoExecutionException(
                         "Failed to execute 'install_name_tool -change "
                         + subjectName
                         + " "
                         + subjectPath
                         + " "
                         + dependentPath
                         + "'"
                         + " return code: \'"
                         + changeResult
                         + "\'." );
                  }
             }
        }
    }

    static Set findInstallNameToolCandidates( File[] files, final Log log )
        throws MojoExecutionException, MojoFailureException
    {
        HashSet candidates = new HashSet();

        for (int i = 0; i < files.length; i++)
        {
            File file = files[i];

            if ( !file.exists() )
            {
                continue;
            }

            if ( file.isDirectory() )
            {
                candidates.addAll(findInstallNameToolCandidates( file.listFiles(), log ));
            }

            String fileName = file.getName();
            if ( file.isFile() && file.canWrite()
               && ( fileName.endsWith( ".so" ) || fileName.endsWith( ".dylib" ) || (fileName.endsWith( ".jnilib" ))))
            {
                candidates.add(file);
            }
        }

        return candidates;
    }

    public static void makeLink( File file, final Log log )
        throws MojoExecutionException, MojoFailureException
    {
        if ( !file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            File[] files = file.listFiles();
            for ( int i = 0; i < files.length; i++ )
            {
                makeLink( files[i], log );
            }
        }
        if ( file.isFile() && file.canRead() && file.canWrite() && !file.isHidden()
            && file.getName().matches( ".*\\.so(\\.\\d+)+$" ) )
        {
            File sofile =
                new File( file.getParent(), file.getName().substring( 0, file.getName().indexOf( ".so" ) + 3 ) );
            if ( !sofile.exists() )
            {
                // ln -s lib.so.xx lib.so
                int result =
                    runCommand( "ln", new String[] { "-s", file.getName(), sofile.getPath() }, null, null, log );
                if ( result != 0 )
                {
                    throw new MojoExecutionException( "Failed to execute 'ln -s " + file.getName() + " "
                        + sofile.getPath() + "'" + " return code: \'" + result + "\'." );
                }
            }
        }
    }

    /**
     * Returns the Bcel Class corresponding to the given class filename
     * 
     * @param filename the absolute file name of the class
     * @return the Bcel Class.
     * @throws IOException
     */
    public static JavaClass getBcelClass( String filename )
        throws IOException
    {
        ClassParser parser = new ClassParser( filename );
        return parser.parse();
    }

    /**
     * Returns the header file name (javah) corresponding to the given class file name
     * 
     * @param filename the absolute file name of the class
     * @return the header file name.
     */
    public static String getHeaderName( String basename, String filename )
    {
        String base = basename.replaceAll( "\\\\", "/" );
        String file = filename.replaceAll( "\\\\", "/" );
        if ( !file.startsWith( base ) )
        {
            throw new IllegalArgumentException( "Error " + file + " does not start with " + base );
        }
        String header = file.substring( base.length() + 1 );
        header = header.replaceAll( "/", "_" );
        header = header.replaceAll( "\\.class", ".h" );
        return header;
    }

    /**
     * Replaces target with replacement in string. For jdk 1.4 compatiblity.
     * 
     * @param target
     * @param replacement
     * @param string
     * @return
     */
    public static String replace( CharSequence target, CharSequence replacement, String string )
    {
        return Pattern.compile( quote( target.toString() )/*
                                                           * , Pattern.LITERAL jdk 1.4
                                                           */).matcher( string ).replaceAll(
        /* Matcher. jdk 1.4 */quoteReplacement( replacement.toString() ) );
    }

    /* for jdk 1.4 */
    private static String quote( String s )
    {
        final String escQ = "\\Q";
        final String escE = "\\E";

        int slashEIndex = s.indexOf( escE );
        if ( slashEIndex == -1 )
        {
            return escQ + s + escE;
        }

        StringBuffer sb = new StringBuffer( s.length() * 2 );
        sb.append( escQ );
        slashEIndex = 0;
        int current = 0;
        while ( ( slashEIndex = s.indexOf( escE, current ) ) != -1 )
        {
            sb.append( s.substring( current, slashEIndex ) );
            current = slashEIndex + 2;
            sb.append( escE );
            sb.append( "\\" );
            sb.append( escE );
            sb.append( escQ );
        }
        sb.append( s.substring( current, s.length() ) );
        sb.append( escE );
        return sb.toString();
    }

    /* for jdk 1.4 */
    private static String quoteReplacement( String s )
    {
        if ( ( s.indexOf( '\\' ) == -1 ) && ( s.indexOf( '$' ) == -1 ) )
        {
            return s;
        }
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < s.length(); i++ )
        {
            char c = s.charAt( i );
            if ( c == '\\' )
            {
                sb.append( '\\' );
                sb.append( '\\' );
            }
            else if ( c == '$' )
            {
                sb.append( '\\' );
                sb.append( '$' );
            }
            else
            {
                sb.append( c );
            }
        }
        return sb.toString();
    }

    public static final String DEFAULT_EXCLUDES =
        "**/*~,**/#*#,**/.#*,**/%*%,**/._*," + "**/CVS,**/CVS/**,**/.cvsignore," + "**/SCCS,**/SCCS/**,**/vssver.scc,"
            + "**/.svn,**/.svn/**,**/.DS_Store";

    public static int copyDirectoryStructure( File sourceDirectory, File destinationDirectory, String includes,
                                              String excludes )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
        }

        List files = FileUtils.getFiles( sourceDirectory, includes, excludes );
        String sourcePath = sourceDirectory.getAbsolutePath();

        int copied = 0;
        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            File file = (File) i.next();
            String dest = file.getAbsolutePath();
            dest = dest.substring( sourcePath.length() + 1 );
            File destination = new File( destinationDirectory, dest );
            if ( file.isFile() )
            {
                // destination = destination.getParentFile();
                // use FileUtils from commons-io, because it preserves timestamps
                org.apache.commons.io.FileUtils.copyFile( file, destination );
                copied++;

                // copy executable bit
                try
                {
                    // 1.6 only so coded using introspection
                    // destination.setExecutable( file.canExecute(), false );
                    Method canExecute = file.getClass().getDeclaredMethod( "canExecute", new Class[] {} );
                    Method setExecutable =
                        destination.getClass().getDeclaredMethod( "setExecutable",
                                                                  new Class[] { boolean.class, boolean.class } );
                    setExecutable.invoke( destination, new Object[] {
                        (Boolean) canExecute.invoke( file, new Object[] {} ), Boolean.FALSE } );
                }
                catch ( SecurityException e )
                {
                    // ignored
                }
                catch ( NoSuchMethodException e )
                {
                    // ignored
                }
                catch ( IllegalArgumentException e )
                {
                    // ignored
                }
                catch ( IllegalAccessException e )
                {
                    // ignored
                }
                catch ( InvocationTargetException e )
                {
                    // ignored
                }
            }
            else if ( file.isDirectory() )
            {
                if ( !destination.exists() && !destination.mkdirs() )
                {
                    throw new IOException( "Could not create destination directory '" + destination.getAbsolutePath()
                        + "'." );
                }
                copied += copyDirectoryStructure( file, destination, includes, excludes );
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.getAbsolutePath() );
            }
        }
        return copied;
    }

    public static String getEnv( String envKey, String alternateSystemProperty, String defaultValue )
    {
        String envValue = null;
        try
        {
            envValue = System.getenv( envKey );
            if ( envValue == null && alternateSystemProperty != null )
            {
                envValue = System.getProperty( alternateSystemProperty );
            }
        }
        catch ( Error e )
        {
            // JDK 1.4?
            if ( alternateSystemProperty != null )
            {
                envValue = System.getProperty( alternateSystemProperty );
            }
        }

        if ( envValue == null )
        {
            envValue = defaultValue;
        }

        return envValue;
    }

    public static String addLibraryPathToEnv( String path, Map environment, String os )
    {
        String pathName = null;
        char separator = ' ';
        if ( os.equals( OS.WINDOWS ) )
        {
            pathName = "Path";
            separator = ';';
        }
        else if ( os.equals( OS.MACOSX ) )
        {
            pathName = "DYLD_LIBRARY_PATH";
            separator = ':';
        }
        else
        {
            pathName = "LD_LIBRARY_PATH";
            separator = ':';
        }

        String value = environment != null ? (String) environment.get( pathName ) : null;
        if ( value == null )
        {
            value = NarUtil.getEnv( pathName, pathName, null );
        }

        String libPath = path;
        libPath = libPath.replace( File.pathSeparatorChar, separator );
        if ( value != null )
        {
            value += separator + libPath;
        }
        else
        {
            value = libPath;
        }
        if ( environment != null )
        {
            environment.put( pathName, value );
        }
        return pathName + "=" + value;
    }

    public static int runCommand( String cmd, String[] args, File workingDirectory, String[] env, final Log log )
        throws MojoExecutionException, MojoFailureException
    {
        if ( log.isInfoEnabled() )
        {
            final StringBuilder argLine = new StringBuilder();
            if ( args != null )
            {
                for ( final String arg : args )
                {
                    argLine.append( " " + arg );
                }
            }
            if ( workingDirectory != null )
            {
                log.info( "+ cd " + workingDirectory.getAbsolutePath() );
            }
            log.info( "+ " + cmd + argLine );
        }
        return runCommand( cmd, args, workingDirectory, env, new TextStream()
        {
            public void println( String text )
            {
                log.info( text );
            }
        }, new TextStream()
        {
            public void println( String text )
            {
                log.error( text );
            }

        }, new TextStream()
        {
            public void println( String text )
            {
                log.debug( text );
            }
        } , log );
    }

    public static int runCommand( String cmd, String[] args, File workingDirectory, String[] env, TextStream out,
           TextStream err, TextStream dbg, final Log log )
        throws MojoExecutionException, MojoFailureException
    {
        return runCommand( cmd, args, workingDirectory, env, out, err, dbg, log, false );
    }

    public static int runCommand( String cmd, String[] args, File workingDirectory, String[] env, TextStream out,
           TextStream err, TextStream dbg, final Log log, boolean expectFailure )
        throws MojoExecutionException, MojoFailureException
    {
        Commandline cmdLine = new Commandline();

        try
        {
            dbg.println( "RunCommand: " + cmd );
            cmdLine.setExecutable( cmd );
            if ( args != null )
            {
                for ( int i = 0; i < args.length; i++ )
                {
                    dbg.println( "  '" + args[i] + "'" );
                }
                cmdLine.addArguments( args );
            }
            if ( workingDirectory != null )
            {
                dbg.println( "in: " + workingDirectory.getPath() );
                cmdLine.setWorkingDirectory( workingDirectory );
            }

            if ( env != null )
            {
                dbg.println( "with Env:" );
                for ( int i = 0; i < env.length; i++ )
                {
                    String[] nameValue = env[i].split( "=", 2 );
                    if ( nameValue.length < 2 )
                    {
                        throw new MojoFailureException( "   Misformed env: '" + env[i] + "'" );
                    }
                    dbg.println( "   '" + nameValue[0] + "=" + nameValue[1] + "'" );
                    cmdLine.addEnvironment( nameValue[0], nameValue[1] );
                }
            }

            Process process = cmdLine.execute();
            StreamGobbler errorGobbler = new StreamGobbler( process.getErrorStream(), err );
            StreamGobbler outputGobbler = new StreamGobbler( process.getInputStream(), out );

            errorGobbler.start();
            outputGobbler.start();
            process.waitFor();
            int exitValue = process.exitValue();
            dbg.println( "ExitValue: " + exitValue );
            final int timeout = 5000;
            errorGobbler.join( timeout );
            outputGobbler.join( timeout );
            if ( exitValue != 0 ^ expectFailure )
            {
                 if ( log == null )
                 {
                      System.err.println(err.toString());
                      System.err.println(out.toString());
                      System.err.println(dbg.toString());
                 }
                 else
                 {
                      log.warn(err.toString());
                      log.warn(out.toString());
                      log.warn(dbg.toString());
                 }
                 throw new MojoExecutionException( "exit code: " + exitValue );
            }
            return exitValue;
        }
        catch ( MojoExecutionException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Could not launch " + cmdLine, e );
        }
    }

    private static final class StreamGobbler
        extends Thread
    {
        private InputStream is;

        private TextStream ts;

        private StreamGobbler( InputStream is, TextStream ts )
        {
            this.is = is;
            this.ts = ts;
        }

        public void run()
        {
            try
            {
                BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );
                String line = null;
                while ( ( line = reader.readLine() ) != null )
                {
                    ts.println( line );
                }
                reader.close();
            }
            catch ( IOException e )
            {
                // e.printStackTrace()
                StackTraceElement[] stackTrace = e.getStackTrace();
                for ( int i = 0; i < stackTrace.length; i++ )
                {
                    ts.println( stackTrace[i].toString() );
                }
            }
        }
    }


    /**
     * (Darren) this code lifted from mvn help:active-profiles plugin Recurses
     * into the project's parent poms to find the active profiles of the
     * specified project and all its parents.
     * 
     * @param project
     *            The project to start with
     * @return A list of active profiles
     */
    static List collectActiveProfiles(MavenProject project) {
        List profiles = project.getActiveProfiles();

        if (project.hasParent()) {
            profiles.addAll(collectActiveProfiles(project.getParent()));
        }

        return profiles;
    }

    static void removeNulls( Collection<?> collection )
    {
        for ( Iterator<?> iter = collection.iterator(); iter.hasNext(); ) {
            if ( iter.next() == null ) {
                iter.remove();
            }
        }
    }
}
