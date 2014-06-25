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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.ForkConfiguration;
import org.apache.maven.surefire.booter.SurefireBooter;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.report.BriefConsoleReporter;
import org.apache.maven.surefire.report.BriefFileReporter;
import org.apache.maven.surefire.report.ConsoleReporter;
import org.apache.maven.surefire.report.DetailedConsoleReporter;
import org.apache.maven.surefire.report.FileReporter;
import org.apache.maven.surefire.report.ForkingConsoleReporter;
import org.apache.maven.surefire.report.XMLReporter;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;

// Copied from Maven maven-surefire-plugin 2.4.3

/**
 * Run integration tests using Surefire. This goal was copied from Maven's surefire plugin to accomodate a few things
 * for the NAR plugin:
 * <P>
 * 1. To test a jar file with its native module we can only run after the package phase, so we use the integration-test
 * phase.
 * </P>
 * <P>
 * 2. We need to set java.library.path to an AOL (architecture-os-linker) specific value, but AOL is only known in the
 * NAR plugin and thus cannot be set from the pom.
 * </P>
 * <P>
 * 3. To have the java.library.path definition picked up by java we need the "pertest" forkmode. To use this goal you
 * need to put the test sources in the regular test directories but disable the running of the tests by the
 * maven-surefire-plugin by setting maven.test.skip.exec to false in your pom.
 * </P>
 * 
 * @author Jason van Zyl (modified by Mark Donszelmann, noted by DUNS)
 * @version $Id: SurefirePlugin.java 652773 2008-05-02 05:58:54Z dfabulich $ Mods by Duns for NAR
 * @requiresDependencyResolution test
 * @goal nar-integration-test
 * @phase integration-test
 */
// DUNS, changed class name, inheritance, goal and phase
public class NarIntegrationTestMojo
    extends AbstractDependencyMojo
{
	@Override
	protected List/*<Artifact>*/ getArtifacts() {
		return getMavenProject().getTestArtifacts();  // Artifact.SCOPE_TEST 
	}

    protected File getUnpackDirectory()
    {
        return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
    }

    // DUNS added test for JNI module
    private boolean testJNIModule()
    {
        for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
        {
            Library lib = (Library) i.next();
            String type = lib.getType();
            if ( type.equals( Library.JNI ) || type.equals( Library.SHARED ) )
            {
                return true;
            }
        }
        return false;
    }

    // DUNS added because of naming conflict
    /**
     * Skip running of NAR integration test plugin
     * 
     * @parameter property="skipNar" default-value="false"
     */
    private boolean skipNar;

    // DUNS changed to nar. because of naming conflict
    /**
     * Set this to 'true' to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     * 
     * @parameter property="skipNarTests"
     * @since 2.4
     */
    private boolean skipNarTests;

    // DUNS changed to nar. because of naming conflict
    /**
     * DEPRECATED This old parameter is just like skipTests, but bound to the old property maven.test.skip.exec. Use
     * -DskipTests instead; it's shorter.
     * 
     * @deprecated
     * @parameter property="nar.test.skip.exec"
     * @since 2.3
     */
    private boolean skipNarExec;

    // DUNS changed to nar. because of naming conflict
    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     * 
     * @parameter property="nar.test.failure.ignore"
     */
    private boolean testFailureIgnore;

    /**
     * The base directory of the project being tested. This can be obtained in your unit test by
     * System.getProperty("basedir").
     * 
     * @parameter property="basedir"
     * @required
     */
    private File basedir;

    /**
     * The directory containing generated test classes of the project being tested.
     * 
     * @parameter property="project.build.testOutputDirectory"
     * @required
     */
    private File testClassesDirectory;

    /**
     * The directory containing generated classes of the project being tested.
     * 
     * @parameter property="project.build.outputDirectory"
     * @required
     */
    private File classesDirectory;

    /**
     * The Maven Project Object
     * 
     * @parameter property="project"
     * @required
     * @readonly
     */
    // DUNS, made private
    private MavenProject project;

    /**
     * The classpath elements of the project being tested.
     * 
     * @parameter property="project.testClasspathElements"
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * Additional elements to be appended to the classpath.
     * 
     * @parameter
     * @since 2.4
     */
    private List additionalClasspathElements;

    /**
     * Base directory where all reports are written to.
     * 
     * @parameter default-value="${project.build.directory}/surefire-reports"
     */
    private File reportsDirectory;

    /**
     * The test source directory containing test class sources.
     * 
     * @parameter property="project.build.testSourceDirectory"
     * @required
     * @since 2.2
     */
    private File testSourceDirectory;

    /**
     * Specify this parameter to run individual tests by file name, overriding the <code>includes/excludes</code>
     * parameters. Each pattern you specify here will be used to create an include pattern formatted like
     * <code>**&#47;${test}.java</code>, so you can just type "-Dtest=MyTest" to run a single test called
     * "foo/MyTest.java". This parameter will override the TestNG suiteXmlFiles parameter.
     * 
     * @parameter property="test"
     */
    private String test;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be included in testing. When not
     * specified and when the <code>test</code> parameter is not specified, the default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>. This parameter is ignored if TestNG
     * suiteXmlFiles are specified.
     * 
     * @parameter
     */
    private List includes;

    /**
     * List of patterns (separated by commas) used to specify the tests that should be excluded in testing. When not
     * specified and when the <code>test</code> parameter is not specified, the default excludes will be
     * <code>**&#47;*$*</code> (which excludes all inner classes). This parameter is ignored if TestNG suiteXmlFiles are
     * specified.
     * 
     * @parameter
     */
    private List excludes;

    /**
     * ArtifactRepository of the localRepository. To obtain the directory of localRepository in unit tests use
     * System.setProperty( "localRepository").
     * 
     * @parameter property="localRepository"
     * @required
     * @readonly
     */
    // DUNS, removed, already in superclass
    // private ArtifactRepository localRepository;

    /**
     * List of System properties to pass to the JUnit tests.
     * 
     * @parameter
     */
    private Properties systemProperties;

    /**
     * List of properties for configuring all TestNG related configurations. This is the new preferred method of
     * configuring TestNG.
     * 
     * @parameter
     * @since 2.4
     */
    private Properties properties;

    /**
     * Map of of plugin artifacts.
     * 
     * @parameter property="plugin.artifactMap"
     * @required
     * @readonly
     */
    private Map pluginArtifactMap;

    /**
     * Map of of project artifacts.
     * 
     * @parameter property="project.artifactMap"
     * @required
     * @readonly
     */
    private Map projectArtifactMap;

    /**
     * Option to print summary of test suites or just print the test cases that has errors.
     * 
     * @parameter property="surefire.printSummary" default-value="true"
     */
    private boolean printSummary;

    /**
     * Selects the formatting for the test report to be generated. Can be set as brief or plain.
     * 
     * @parameter property="surefire.reportFormat" default-value="brief"
     */
    private String reportFormat;

    /**
     * Option to generate a file test report or just output the test report to the console.
     * 
     * @parameter property="surefire.useFile" default-value="true"
     */
    private boolean useFile;

    // DUNS changed to nar. because of naming conflict
    /**
     * When forking, set this to true to redirect the unit test standard output to a file (found in
     * reportsDirectory/testName-output.txt).
     * 
     * @parameter property="nar.test.redirectTestOutputToFile" default-value="false"
     * @since 2.3
     */
    private boolean redirectTestOutputToFile;

    /**
     * Set this to "true" to cause a failure if there are no tests to run. Defaults to false.
     * 
     * @parameter property="failIfNoTests"
     * @since 2.4
     */
    private Boolean failIfNoTests;

    /**
     * Option to specify the forking mode. Can be "never", "once" or "always". "none" and "pertest" are also accepted
     * for backwards compatibility.
     * 
     * @parameter property="forkMode" default-value="once"
     * @since 2.1
     */
    private String forkMode;

    /**
     * Option to specify the jvm (or path to the java executable) to use with the forking options. For the default, the
     * jvm will be the same as the one used to run Maven.
     * 
     * @parameter property="jvm"
     * @since 2.1
     */
    private String jvm;

    /**
     * Arbitrary JVM options to set on the command line.
     * 
     * @parameter property="argLine"
     * @since 2.1
     */
    private String argLine;

    /**
     * Attach a debugger to the forked JVM. If set to "true", the process will suspend and wait for a debugger to attach
     * on port 5005. If set to some other string, that string will be appended to the argLine, allowing you to configure
     * arbitrary debuggability options (without overwriting the other options specified in the argLine).
     * 
     * @parameter property="maven.surefire.debug"
     * @since 2.4
     */
    private String debugForkedProcess;

    /**
     * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for the process, never
     * timing out.
     * 
     * @parameter property="surefire.timeout"
     * @since 2.4
     */
    private int forkedProcessTimeoutInSeconds;

    /**
     * Additional environments to set on the command line.
     * 
     * @parameter
     * @since 2.1.3
     */
    private Map environmentVariables = new HashMap();

    /**
     * Command line working directory.
     * 
     * @parameter property="basedir"
     * @since 2.1.3
     */
    private File workingDirectory;

    /**
     * When false it makes tests run using the standard classloader delegation instead of the default Maven isolated
     * classloader. Only used when forking (forkMode is not "none").<br/>
     * Setting it to false helps with some problems caused by conflicts between xml parsers in the classpath and the
     * Java 5 provider parser.
     * 
     * @parameter property="childDelegation" default-value="false"
     * @since 2.1
     */
    private boolean childDelegation;

    /**
     * (TestNG only) Groups for this test. Only classes/methods/etc decorated with one of the groups specified here will
     * be included in test run, if specified. This parameter is overridden if suiteXmlFiles are specified.
     * 
     * @parameter property="groups"
     * @since 2.2
     */
    private String groups;

    /**
     * (TestNG only) Excluded groups. Any methods/classes/etc with one of the groups specified in this list will
     * specifically not be run. This parameter is overridden if suiteXmlFiles are specified.
     * 
     * @parameter property="excludedGroups"
     * @since 2.2
     */
    private String excludedGroups;

    /**
     * (TestNG only) List of TestNG suite xml file locations, seperated by commas. Note that suiteXmlFiles is
     * incompatible with several other parameters on this plugin, like includes/excludes. This parameter is ignored if
     * the "test" parameter is specified (allowing you to run a single test instead of an entire suite).
     * 
     * @parameter
     * @since 2.2
     */
    private File[] suiteXmlFiles;

    /**
     * Allows you to specify the name of the JUnit artifact. If not set, <code>junit:junit</code> will be used.
     * 
     * @parameter property="junitArtifactName" default-value="junit:junit"
     * @since 2.3.1
     */
    private String junitArtifactName;

    /**
     * Allows you to specify the name of the TestNG artifact. If not set, <code>org.testng:testng</code> will be used.
     * 
     * @parameter property="testNGArtifactName" default-value="org.testng:testng"
     * @since 2.3.1
     */
    private String testNGArtifactName;

    /**
     * (TestNG only) The attribute thread-count allows you to specify how many threads should be allocated for this
     * execution. Only makes sense to use in conjunction with parallel.
     * 
     * @parameter property="threadCount"
     * @since 2.2
     */
    private int threadCount;

    /**
     * (TestNG only) When you use the parallel attribute, TestNG will try to run all your test methods in separate
     * threads, except for methods that depend on each other, which will be run in the same thread in order to respect
     * their order of execution.
     * 
     * @parameter property="parallel"
     * @todo test how this works with forking, and console/file output parallelism
     * @since 2.2
     */
    private String parallel;

    /**
     * Whether to trim the stack trace in the reports to just the lines within the test, or show the full trace.
     * 
     * @parameter property="trimStackTrace" default-value="true"
     * @since 2.2
     */
    private boolean trimStackTrace;

    /**
     * Creates the artifact
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * For retrieval of artifact's metadata.
     * 
     * @component
     */
    private ArtifactMetadataSource metadataSource;

    private static final String BRIEF_REPORT_FORMAT = "brief";

    private static final String PLAIN_REPORT_FORMAT = "plain";

    private Properties originalSystemProperties;

    /**
     * Flag to disable the generation of report files in xml format.
     * 
     * @parameter property="disableXmlReport" default-value="false"
     * @since 2.2
     */
    private boolean disableXmlReport;

    /**
     * Option to pass dependencies to the system's classloader instead of using an isolated class loader when forking.
     * Prevents problems with JDKs which implement the service provider lookup mechanism by using the system's
     * classloader. Default value is "true".
     * 
     * @parameter property="surefire.useSystemClassLoader"
     * @since 2.3
     */
    private Boolean useSystemClassLoader;

    /**
     * By default, Surefire forks your tests using a manifest-only jar; set this parameter to "false" to force it to
     * launch your tests with a plain old Java classpath. (See
     * http://maven.apache.org/plugins/maven-surefire-plugin/examples/class-loading.html for a more detailed explanation
     * of manifest-only jars and their benefits.) Default value is "true". Beware, setting this to "false" may cause
     * your tests to fail on Windows if your classpath is too long.
     * 
     * @parameter property="surefire.useManifestOnlyJar" default-value="true"
     * @since 2.4.3
     */
    private boolean useManifestOnlyJar;

    /**
     * By default, Surefire enables JVM assertions for the execution of your test cases. To disable the assertions, set
     * this flag to <code>false</code>.
     * 
     * @parameter property="enableAssertions" default-value="true"
     * @since 2.3.1
     */
    private boolean enableAssertions;

    /**
     * The current build session instance.
     * 
     * @parameter property="session"
     * @required
     * @readonly
     */
    private MavenSession session;

    // DUNS, changed name
    public void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skipTests )
        {
            getLog().info( "Tests are skipped" );
        }
        else if ( verifyParameters() )
        {
            super.narExecute();

            SurefireBooter surefireBooter = constructSurefireBooter();

            getLog().info( "Surefire report directory: " + reportsDirectory );

            int result;
            try
            {
                result = surefireBooter.run();
            }
            catch ( SurefireBooterForkException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
            catch ( SurefireExecutionException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }

            if ( originalSystemProperties != null && !surefireBooter.isForking() )
            {
                // restore system properties, only makes sense when not forking..
                System.setProperties( originalSystemProperties );
            }

            if ( result == 0 )
            {
                return;
            }

            String msg;

            if ( result == SurefireBooter.NO_TESTS_EXIT_CODE )
            {
                if ( ( failIfNoTests == null ) || !failIfNoTests.booleanValue() )
                {
                    return;
                }
                // TODO: i18n
                throw new MojoFailureException(
                                                "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
            }
            else
            {
                // TODO: i18n
                msg =
                    "There are test failures.\n\nPlease refer to " + reportsDirectory
                        + " for the individual test results.";

            }

            if ( testFailureIgnore )
            {
                getLog().error( msg );
            }
            else
            {
                throw new MojoFailureException( msg );
            }
        }
    }

    private boolean verifyParameters()
        throws MojoFailureException
    {
        // DUNS
        if ( skipNar || skipNarTests || skipNarExec )
        {
            getLog().info( "Tests are skipped." );
            return false;
        }

        if ( !testClassesDirectory.exists() )
        {
            if ( failIfNoTests != null && failIfNoTests.booleanValue() )
            {
                throw new MojoFailureException( "No tests to run!" );
            }
            getLog().info( "No tests to run." );
            return false;
        }

        if ( useSystemClassLoader != null && ForkConfiguration.FORK_NEVER.equals( forkMode ) )
        {
            getLog().warn( "useSystemClassloader setting has no effect when not forking" );
        }

        return true;
    }

    /**
     * Converts old TestNG configuration parameters over to new properties based configuration method. (if any are
     * defined the old way)
     */
    private void convertTestNGParameters()
    {
        if ( properties == null )
        {
            properties = new Properties();
        }

        if ( this.parallel != null )
        {
            properties.setProperty( "parallel", this.parallel );
        }
        if ( this.excludedGroups != null )
        {
            properties.setProperty( "excludegroups", this.excludedGroups );
        }
        if ( this.groups != null )
        {
            properties.setProperty( "groups", this.groups );
        }

        if ( this.threadCount > 0 )
        {
            properties.setProperty( "threadcount", new Integer( this.threadCount ).toString() );
        }
    }

    private SurefireBooter constructSurefireBooter()
        throws MojoExecutionException, MojoFailureException
    {
        SurefireBooter surefireBooter = new SurefireBooter();

        Artifact surefireArtifact = (Artifact) pluginArtifactMap.get( "org.apache.maven.surefire:surefire-booter" );
        if ( surefireArtifact == null )
        {
            throw new MojoExecutionException( "Unable to locate surefire-booter in the list of plugin artifacts" );
        }

        surefireArtifact.isSnapshot(); // TODO: this is ridiculous, but it fixes getBaseVersion to be -SNAPSHOT if
        // needed

        Artifact junitArtifact;
        Artifact testNgArtifact;
        try
        {
            addArtifact( surefireBooter, surefireArtifact );

            junitArtifact = (Artifact) projectArtifactMap.get( junitArtifactName );
            // SUREFIRE-378, junit can have an alternate artifact name
            if ( junitArtifact == null && "junit:junit".equals( junitArtifactName ) )
            {
                junitArtifact = (Artifact) projectArtifactMap.get( "junit:junit-dep" );
            }

            // TODO: this is pretty manual, but I'd rather not require the plugin > dependencies section right now
            testNgArtifact = (Artifact) projectArtifactMap.get( testNGArtifactName );

            if ( testNgArtifact != null )
            {
                VersionRange range = VersionRange.createFromVersionSpec( "[4.7,)" );
                if ( !range.containsVersion( new DefaultArtifactVersion( testNgArtifact.getVersion() ) ) )
                {
                    throw new MojoFailureException(
                                                    "TestNG support requires version 4.7 or above. You have declared version "
                                                        + testNgArtifact.getVersion() );
                }

                convertTestNGParameters();

                if ( this.testClassesDirectory != null )
                {
                    properties.setProperty( "testng.test.classpath", testClassesDirectory.getAbsolutePath() );
                }

                addArtifact( surefireBooter, testNgArtifact );

                // The plugin uses a JDK based profile to select the right testng. We might be explicity using a
                // different one since its based on the source level, not the JVM. Prune using the filter.
                addProvider( surefireBooter, "surefire-testng", surefireArtifact.getBaseVersion(), testNgArtifact );
            }
            else if ( junitArtifact != null && junitArtifact.getBaseVersion().startsWith( "4" ) )
            {
                addProvider( surefireBooter, "surefire-junit4", surefireArtifact.getBaseVersion(), null );
            }
            else
            {
                // add the JUnit provider as default - it doesn't require JUnit to be present,
                // since it supports POJO tests.
                addProvider( surefireBooter, "surefire-junit", surefireArtifact.getBaseVersion(), null );
            }
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to locate required surefire provider dependency: "
                + e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( "Error determining the TestNG version requested: " + e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Error to resolving surefire provider dependency: " + e.getMessage(), e );
        }

        if ( suiteXmlFiles != null && suiteXmlFiles.length > 0 && test == null )
        {
            if ( testNgArtifact == null )
            {
                throw new MojoExecutionException( "suiteXmlFiles is configured, but there is no TestNG dependency" );
            }

            // TODO: properties should be passed in here too
            surefireBooter.addTestSuite( "org.apache.maven.surefire.testng.TestNGXmlTestSuite", new Object[] {
                suiteXmlFiles, testSourceDirectory.getAbsolutePath(), testNgArtifact.getBaseVersion(),
                testNgArtifact.getClassifier(), properties, reportsDirectory } );
        }
        else
        {
            List includeList;
            List excludeList;

            if ( test != null )
            {
                // Check to see if we are running a single test. The raw parameter will
                // come through if it has not been set.

                // FooTest -> **/FooTest.java

                includeList = new ArrayList();

                excludeList = new ArrayList();

                if ( failIfNoTests == null )
                {
                    failIfNoTests = Boolean.TRUE;
                }

                String[] testRegexes = StringUtils.split( test, "," );

                for ( int i = 0; i < testRegexes.length; i++ )
                {
                    String testRegex = testRegexes[i];
                    if ( testRegex.endsWith( ".java" ) )
                    {
                        testRegex = testRegex.substring( 0, testRegex.length() - 5 );
                    }
                    // Allow paths delimited by '.' or '/'
                    testRegex = testRegex.replace( '.', '/' );
                    includeList.add( "**/" + testRegex + ".java" );
                }
            }
            else
            {
                includeList = this.includes;

                excludeList = this.excludes;

                // defaults here, qdox doesn't like the end javadoc value
                // Have to wrap in an ArrayList as surefire expects an ArrayList instead of a List for some reason
                if ( includeList == null || includeList.size() == 0 )
                {
                    includeList =
                        new ArrayList( Arrays.asList( new String[] { "**/Test*.java", "**/*Test.java",
                            "**/*TestCase.java" } ) );
                }
                if ( excludeList == null || excludeList.size() == 0 )
                {
                    excludeList = new ArrayList( Arrays.asList( new String[] { "**/*$*" } ) );
                }
            }

            if ( testNgArtifact != null )
            {
                surefireBooter.addTestSuite( "org.apache.maven.surefire.testng.TestNGDirectoryTestSuite", new Object[] {
                    testClassesDirectory, includeList, excludeList, testSourceDirectory.getAbsolutePath(),
                    testNgArtifact.getBaseVersion(), testNgArtifact.getClassifier(), properties, reportsDirectory } );
            }
            else
            {
                String junitDirectoryTestSuite;
                if ( junitArtifact != null && junitArtifact.getBaseVersion() != null
                    && junitArtifact.getBaseVersion().startsWith( "4" ) )
                {
                    junitDirectoryTestSuite = "org.apache.maven.surefire.junit4.JUnit4DirectoryTestSuite";
                }
                else
                {
                    junitDirectoryTestSuite = "org.apache.maven.surefire.junit.JUnitDirectoryTestSuite";
                }

                // fall back to JUnit, which also contains POJO support. Also it can run
                // classes compiled against JUnit since it has a dependency on JUnit itself.
                surefireBooter.addTestSuite( junitDirectoryTestSuite, new Object[] { testClassesDirectory, includeList,
                    excludeList } );
            }
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        getLog().debug( "Test Classpath :" );

        // Check if we need to add configured classes/test classes directories here.
        // If they are configured, we should remove the default to avoid conflicts.
        if ( !project.getBuild().getOutputDirectory().equals( classesDirectory.getAbsolutePath() ) )
        {
            classpathElements.remove( project.getBuild().getOutputDirectory() );
            classpathElements.add( classesDirectory.getAbsolutePath() );
        }
        if ( !project.getBuild().getTestOutputDirectory().equals( testClassesDirectory.getAbsolutePath() ) )
        {
            classpathElements.remove( project.getBuild().getTestOutputDirectory() );
            classpathElements.add( testClassesDirectory.getAbsolutePath() );
        }

        for ( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            String classpathElement = (String) i.next();

            getLog().debug( "  " + classpathElement );

            surefireBooter.addClassPathUrl( classpathElement );
        }

        Toolchain tc = getToolchain();

        if ( tc != null )
        {
            getLog().info( "Toolchain in surefire-plugin: " + tc );
            if ( ForkConfiguration.FORK_NEVER.equals( forkMode ) )
            {
                forkMode = ForkConfiguration.FORK_ONCE;
            }
            if ( jvm != null )
            {
                getLog().warn( "Toolchains are ignored, 'executable' parameter is set to " + jvm );
            }
            else
            {
                jvm = tc.findTool( "java" ); // NOI18N
            }
        }

        if ( additionalClasspathElements != null )
        {
            for ( Iterator i = additionalClasspathElements.iterator(); i.hasNext(); )
            {
                String classpathElement = (String) i.next();

                getLog().debug( "  " + classpathElement );

                surefireBooter.addClassPathUrl( classpathElement );
            }
        }

        // ----------------------------------------------------------------------
        // Forking
        // ----------------------------------------------------------------------

        ForkConfiguration fork = new ForkConfiguration();

        // DUNS
        if ( project.getPackaging().equals( "nar" ) || ( getNarArtifacts().size() > 0 ) )
        {
            forkMode = "pertest";
        }

        fork.setForkMode( forkMode );

        processSystemProperties( !fork.isForking() );

        if ( getLog().isDebugEnabled() )
        {
            showMap( systemProperties, "system property" );
        }

        if ( fork.isForking() )
        {
            useSystemClassLoader = useSystemClassLoader == null ? Boolean.TRUE : useSystemClassLoader;
            fork.setUseSystemClassLoader( useSystemClassLoader.booleanValue() );
            fork.setUseManifestOnlyJar( useManifestOnlyJar );

            fork.setSystemProperties( systemProperties );

            if ( "true".equals( debugForkedProcess ) )
            {
                debugForkedProcess =
                    "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";
            }

            fork.setDebugLine( debugForkedProcess );

            if ( jvm == null || "".equals( jvm ) )
            {
                // use the same JVM as the one used to run Maven (the "java.home" one)
                jvm = System.getProperty( "java.home" ) + File.separator + "bin" + File.separator + "java";
                getLog().debug( "Using JVM: " + jvm );
            }

            fork.setJvmExecutable( jvm );

            if ( workingDirectory != null )
            {
                fork.setWorkingDirectory( workingDirectory );
            }
            else
            {
                fork.setWorkingDirectory( basedir );
            }

            // BEGINDUNS
            if ( argLine == null )
            {
                argLine = "";
            }

            StringBuffer javaLibraryPath = new StringBuffer();
            if ( testJNIModule() )
            {
                // Add libraries to java.library.path for testing
                File jniLibraryPathEntry =
                    getLayout().getLibDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                                 getMavenProject().getVersion(), getAOL().toString(), Library.JNI );
                if ( jniLibraryPathEntry.exists() )
                {
                    getLog().debug( "Adding library directory to java.library.path: " + jniLibraryPathEntry );
                    if ( javaLibraryPath.length() > 0 )
                    {
                        javaLibraryPath.append( File.pathSeparator );
                    }
                    javaLibraryPath.append( jniLibraryPathEntry );
                }

                File sharedLibraryPathEntry =
                    getLayout().getLibDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                                 getMavenProject().getVersion(), getAOL().toString(), Library.SHARED );
                if ( sharedLibraryPathEntry.exists() )
                {
                    getLog().debug( "Adding library directory to java.library.path: " + sharedLibraryPathEntry );
                    if ( javaLibraryPath.length() > 0 )
                    {
                        javaLibraryPath.append( File.pathSeparator );
                    }
                    javaLibraryPath.append( sharedLibraryPathEntry );
                }

                // add jar file to classpath, as one may want to read a
                // properties file for artifactId and version
                String narFile = "target/" + project.getArtifactId() + "-" + project.getVersion() + ".jar";
                getLog().debug( "Adding to surefire test classpath: " + narFile );
                surefireBooter.addClassPathUrl( narFile );
            }

            List dependencies = getNarArtifacts(); // TODO: get seems heavy, not sure if we can push this up to before the fork to use it multiple times.
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                NarArtifact dependency = (NarArtifact) i.next();
                // FIXME this should be overridable
                // NarInfo info = dependency.getNarInfo();
                // String binding = info.getBinding(getAOL(), Library.STATIC);
                // NOTE: fixed to shared, jni
                String[] bindings = { Library.SHARED, Library.JNI };
                for ( int j = 0; j < bindings.length; j++ )
                {
                    String binding = bindings[j];
                    if ( !binding.equals( Library.STATIC ) )
                    {
                        File depLibPathEntry =
                            getLayout().getLibDirectory( getUnpackDirectory(), dependency.getArtifactId(),
                                                         dependency.getVersion(), getAOL().toString(), binding );
                        if ( depLibPathEntry.exists() )
                        {
                            getLog().debug( "Adding dependency directory to java.library.path: " + depLibPathEntry );
                            if ( javaLibraryPath.length() > 0 )
                            {
                                javaLibraryPath.append( File.pathSeparator );
                            }
                            javaLibraryPath.append( depLibPathEntry );
                        }
                    }
                }
            }

            // add final javalibrary path
            if ( javaLibraryPath.length() > 0 )
            {
                // NOTE java.library.path only works for the jni lib itself, and
                // not for its dependent shareables.
                // NOTE: java.library.path does not work with arguments with
                // spaces as
                // SureFireBooter splits the line in parts and then quotes
                // it wrongly
                NarUtil.addLibraryPathToEnv( javaLibraryPath.toString(), environmentVariables, getOS() );
            }

            // necessary to find WinSxS
            if ( getOS().equals( OS.WINDOWS ) )
            {
                environmentVariables.put( "SystemRoot", NarUtil.getEnv( "SystemRoot", "SystemRoot", "C:\\Windows" ) );
            }
            // ENDDUNS

            fork.setArgLine( argLine );

            fork.setEnvironmentVariables( environmentVariables );

            if ( getLog().isDebugEnabled() )
            {
                showMap( environmentVariables, "environment variable" );

                fork.setDebug( true );
            }

            if ( argLine != null )
            {
                List args = Arrays.asList( argLine.split( " " ) );
                if ( args.contains( "-da" ) || args.contains( "-disableassertions" ) )
                {
                    enableAssertions = false;
                }
            }
        }

        surefireBooter.setFailIfNoTests( failIfNoTests == null ? false : failIfNoTests.booleanValue() );

        surefireBooter.setForkedProcessTimeoutInSeconds( forkedProcessTimeoutInSeconds );

        surefireBooter.setRedirectTestOutputToFile( redirectTestOutputToFile );

        surefireBooter.setForkConfiguration( fork );

        surefireBooter.setChildDelegation( childDelegation );

        surefireBooter.setEnableAssertions( enableAssertions );

        surefireBooter.setReportsDirectory( reportsDirectory );

        addReporters( surefireBooter, fork.isForking() );

        return surefireBooter;
    }

    private void showMap( Map map, String setting )
    {
        for ( Iterator i = map.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();
            String value = (String) map.get( key );
            getLog().debug( "Setting " + setting + " [" + key + "]=[" + value + "]" );
        }
    }

    private void addProvider( SurefireBooter surefireBooter, String provider, String version, Artifact filteredArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact providerArtifact =
            artifactFactory.createDependencyArtifact( "org.apache.maven.surefire", provider,
                                                      VersionRange.createFromVersion( version ), "jar", null,
                                                      Artifact.SCOPE_TEST );
        ArtifactResolutionResult result = resolveArtifact( filteredArtifact, providerArtifact );

        for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            getLog().debug( "Adding to surefire test classpath: " + artifact.getFile().getAbsolutePath() );

            surefireBooter.addSurefireClassPathUrl( artifact.getFile().getAbsolutePath() );
        }
    }

    private ArtifactResolutionResult resolveArtifact( Artifact filteredArtifact, Artifact providerArtifact )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactFilter filter = null;
        if ( filteredArtifact != null )
        {
            filter =
                new ExcludesArtifactFilter( Collections.singletonList( filteredArtifact.getGroupId() + ":"
                    + filteredArtifact.getArtifactId() ) );
        }

        Artifact originatingArtifact = artifactFactory.createBuildArtifact( "dummy", "dummy", "1.0", "jar" );

        // DUNS, use access method rather than "localRepository" field.
        return artifactResolver.resolveTransitively( Collections.singleton( providerArtifact ), originatingArtifact,
                                                     getLocalRepository(), getRemoteRepositories(), metadataSource, filter );
    }

    private void addArtifact( SurefireBooter surefireBooter, Artifact surefireArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        ArtifactResolutionResult result = resolveArtifact( null, surefireArtifact );

        for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            getLog().debug( "Adding to surefire booter test classpath: " + artifact.getFile().getAbsolutePath() );

            surefireBooter.addSurefireBootClassPathUrl( artifact.getFile().getAbsolutePath() );
        }
    }

    protected void processSystemProperties( boolean setInSystem )
    {
        if ( systemProperties == null )
        {
            systemProperties = new Properties();
        }

        originalSystemProperties = (Properties) System.getProperties().clone();

        // We used to take all of our system properties and dump them in with the
        // user specified properties for SUREFIRE-121, causing SUREFIRE-491.
        // Not gonna do THAT any more... but I'm leaving this code here in case
        // we need it later when we try to fix SUREFIRE-121 again.

        // Get the properties from the MavenSession instance to make embedded use work correctly
        Properties userSpecifiedProperties = (Properties) session.getExecutionProperties().clone();
        userSpecifiedProperties.putAll( systemProperties );
        // systemProperties = userSpecifiedProperties;

        systemProperties.setProperty( "basedir", basedir.getAbsolutePath() );
        systemProperties.setProperty( "user.dir", workingDirectory.getAbsolutePath() );

        // DUNS, use access method rather than "localRepository" field.
        systemProperties.setProperty( "localRepository", getLocalRepository().getBasedir() );

        if ( setInSystem )
        {
            // Add all system properties configured by the user
            Iterator iter = systemProperties.keySet().iterator();

            while ( iter.hasNext() )
            {
                String key = (String) iter.next();

                String value = systemProperties.getProperty( key );

                System.setProperty( key, value );
            }
        }
    }

    /**
     * <p>
     * Adds Reporters that will generate reports with different formatting.
     * <p>
     * The Reporter that will be added will be based on the value of the parameter useFile, reportFormat, and
     * printSummary.
     * 
     * @param surefireBooter The surefire booter that will run tests.
     * @param forking
     */
    private void addReporters( SurefireBooter surefireBooter, boolean forking )
    {
        Boolean trimStackTrace = Boolean.valueOf( this.trimStackTrace );
        if ( useFile )
        {
            if ( printSummary )
            {
                if ( forking )
                {
                    surefireBooter.addReport( ForkingConsoleReporter.class.getName(), new Object[] { trimStackTrace } );
                }
                else
                {
                    surefireBooter.addReport( ConsoleReporter.class.getName(), new Object[] { trimStackTrace } );
                }
            }

            if ( BRIEF_REPORT_FORMAT.equals( reportFormat ) )
            {
                surefireBooter.addReport( BriefFileReporter.class.getName(), new Object[] { reportsDirectory,
                    trimStackTrace } );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( reportFormat ) )
            {
                surefireBooter.addReport( FileReporter.class.getName(),
                                          new Object[] { reportsDirectory, trimStackTrace } );
            }
        }
        else
        {
            if ( BRIEF_REPORT_FORMAT.equals( reportFormat ) )
            {
                surefireBooter.addReport( BriefConsoleReporter.class.getName(), new Object[] { trimStackTrace } );
            }
            else if ( PLAIN_REPORT_FORMAT.equals( reportFormat ) )
            {
                surefireBooter.addReport( DetailedConsoleReporter.class.getName(), new Object[] { trimStackTrace } );
            }
        }

        if ( !disableXmlReport )
        {
            surefireBooter.addReport( XMLReporter.class.getName(), new Object[] { reportsDirectory, trimStackTrace } );
        }
    }

    /**
     * @return SurefirePlugin Returns the skipExec.
     */
    public boolean isSkipExec()
    {
        return this.skipNarTests;
    }

    /**
     * @param skipExec the skipExec to set
     */
    public void setSkipExec( boolean skipExec )
    {
        this.skipNarTests = skipExec;
    }

    // TODO remove the part with ToolchainManager lookup once we depend on
    // 3.0.9 (have it as prerequisite). Define as regular component field then.
    private Toolchain getToolchain()
    {
        Toolchain tc = null;
        try
        {
            if ( session != null ) // session is null in tests..
            {
                ToolchainManager toolchainManager =
                    (ToolchainManager) session.getContainer().lookup( ToolchainManager.ROLE );
                if ( toolchainManager != null )
                {
                    tc = toolchainManager.getToolchainFromBuildContext( "jdk", session );
                }
            }
        }
        catch ( ComponentLookupException componentLookupException )
        {
            // just ignore, could happen in pre-3.0.9 builds..
        }
        return tc;
    }
}
