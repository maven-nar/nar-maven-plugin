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
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
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
 * Run integration tests using Surefire. This goal was copied from Maven's
 * surefire plugin to accomodate a few things
 * for the NAR plugin:
 * <P>
 * 1. To test a jar file with its native module we can only run after the
 * package phase, so we use the integration-test phase.
 * </P>
 * <P>
 * 2. We need to set java.library.path to an AOL (architecture-os-linker)
 * specific value, but AOL is only known in the NAR plugin and thus cannot be
 * set from the pom.
 * </P>
 * <P>
 * 3. To have the java.library.path definition picked up by java we need the
 * "pertest" forkmode. To use this goal you need to put the test sources in the
 * regular test directories but disable the running of the tests by the
 * maven-surefire-plugin by setting maven.test.skip.exec to false in your pom.
 * </P>
 *
 * @author Jason van Zyl (modified by Mark Donszelmann, noted by DUNS)
 * @version $Id: SurefirePlugin.java 652773 2008-05-02 05:58:54Z dfabulich $
 *          Mods by Duns for NAR
 */
// DUNS, changed class name, inheritance, goal and phase
@Mojo(name = "nar-integration-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  requiresDependencyResolution = ResolutionScope.TEST)
public class NarIntegrationTestMojo extends AbstractDependencyMojo {
  private static final String BRIEF_REPORT_FORMAT = "brief";

  private static final String PLAIN_REPORT_FORMAT = "plain";

  // DUNS added because of naming conflict
  /**
   * Skip running of NAR integration test plugin
   */
  @Parameter(property = "skipNar")
  private boolean skipNar;

  // DUNS changed to nar. because of naming conflict
  /**
   * Set this to 'true' to skip running tests, but still compile them. Its use
   * is NOT RECOMMENDED, but quite
   * convenient on occasion.
   * 
   * @since 2.4
   */
  @Parameter(property = "skipNarTests")
  private boolean skipNarTests;

  // DUNS changed to nar. because of naming conflict
  /**
   * DEPRECATED This old parameter is just like skipTests, but bound to the old
   * property maven.test.skip.exec. Use
   * -DskipTests instead; it's shorter.
   * 
   * @deprecated
   * @since 2.3
   */
  @Deprecated
  @Parameter(property = "nar.test.skip.exec")
  private boolean skipNarExec;

  // DUNS changed to nar. because of naming conflict
  /**
   * Set this to true to ignore a failure during testing. Its use is NOT
   * RECOMMENDED, but quite convenient on
   * occasion.
   * 
   */
  @Parameter(property = "nar.test.failure.ignore")
  private boolean testFailureIgnore;

  /**
   * The base directory of the project being tested. This can be obtained in
   * your unit test by
   * System.getProperty("basedir").
   * 
   */
  @Parameter(property = "basedir", required = true)
  private File basedir;

  /**
   * The directory containing generated test classes of the project being
   * tested.
   */
  @Parameter(property = "project.build.testOutputDirectory", required = true)
  private File testClassesDirectory;

  /**
   * The directory containing generated classes of the project being tested.
   */
  @Parameter(property = "project.build.outputDirectory", required = true)
  private File classesDirectory;

  /**
   * The Maven Project Object
   */
  // DUNS, made private
  @Component
  private MavenProject project;

  /**
   * The classpath elements of the project being tested.
   */
  @Parameter(property = "project.testClasspathElements", required = true, readonly = true)
  private List classpathElements;

  /**
   * Additional elements to be appended to the classpath.
   * 
   * @since 2.4
   */
  @Parameter
  private List additionalClasspathElements;

  /**
   * Base directory where all reports are written to.
   */
  @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
  private File reportsDirectory;

  /**
   * The test source directory containing test class sources.
   * 
   * @since 2.2
   */
  @Parameter(property = "project.build.testSourceDirectory", required = true)
  private File testSourceDirectory;

  /**
   * Specify this parameter to run individual tests by file name, overriding the
   * <code>includes/excludes</code> parameters. Each pattern you specify here
   * will be used to create an include pattern formatted like
   * <code>**&#47;${test}.java</code>, so you can just type "-Dtest=MyTest" to
   * run a single test called
   * "foo/MyTest.java". This parameter will override the TestNG suiteXmlFiles
   * parameter.
   */
  @Parameter(property = "test")
  private String test;

  /**
   * List of patterns (separated by commas) used to specify the tests that
   * should be included in testing. When not
   * specified and when the <code>test</code> parameter is not specified, the
   * default includes will be
   * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java</code>.
   * This parameter is ignored if TestNG
   * suiteXmlFiles are specified.
   */
  @Parameter
  private List includes;

  /**
   * List of patterns (separated by commas) used to specify the tests that
   * should be excluded in testing. When not
   * specified and when the <code>test</code> parameter is not specified, the
   * default excludes will be <code>**&#47;*$*</code> (which excludes all inner
   * classes). This parameter is ignored if TestNG suiteXmlFiles are
   * specified.
   */
  @Parameter
  private List excludes;

  /**
   * List of System properties to pass to the JUnit tests.
   * 
   */
  @Parameter
  private Properties systemProperties;

  /**
   * List of properties for configuring all TestNG related configurations. This
   * is the new preferred method of
   * configuring TestNG.
   */
  @Parameter
  private Properties properties;

  /**
   * Map of of plugin artifacts.
   */
  @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
  private Map pluginArtifactMap;

  /**
   * Map of of project artifacts.
   */
  @Parameter(property = "project.artifactMap", readonly = true, required = true)
  private Map projectArtifactMap;

  /**
   * Option to print summary of test suites or just print the test cases that
   * has errors.
   */
  @Parameter(property = "surefire.printSummary", defaultValue = "true")
  private boolean printSummary = true;

  /**
   * Selects the formatting for the test report to be generated. Can be set as
   * brief or plain.
   */
  @Parameter(property = "surefire.reportFormat", defaultValue = "brief")
  private String reportFormat;

  /**
   * Option to generate a file test report or just output the test report to the
   * console.
   * 
   */
  @Parameter(property = "surefire.useFile", defaultValue = "true")
  private boolean useFile;

  // DUNS changed to nar. because of naming conflict
  /**
   * When forking, set this to true to redirect the unit test standard output to
   * a file (found in
   * reportsDirectory/testName-output.txt).
   * 
   * @since 2.3
   */
  @Parameter(property = "nar.test.redirectTestOutputToFile")
  private boolean redirectTestOutputToFile;

  /**
   * Set this to "true" to cause a failure if there are no tests to run.
   * Defaults to false.
   *
   * @since 2.4
   */
  @Parameter(property = "failIfNoTests")
  private Boolean failIfNoTests;

  /**
   * Option to specify the forking mode. Can be "never", "once" or "always".
   * "none" and "pertest" are also accepted
   * for backwards compatibility.
   * 
   * @since 2.1
   */
  @Parameter(property = "forkMode", defaultValue = "once")
  private String forkMode;

  /**
   * Option to specify the jvm (or path to the java executable) to use with the
   * forking options. For the default, the
   * jvm will be the same as the one used to run Maven.
   * 
   * @since 2.1
   */
  @Parameter(property = "jvm")
  private String jvm;

  /**
   * Arbitrary JVM options to set on the command line.
   * 
   * @since 2.1
   */
  @Parameter(property = "argLine")
  private String argLine;

  /**
   * Attach a debugger to the forked JVM. If set to "true", the process will
   * suspend and wait for a debugger to attach
   * on port 5005. If set to some other string, that string will be appended to
   * the argLine, allowing you to configure
   * arbitrary debuggability options (without overwriting the other options
   * specified in the argLine).
   *
   * @since 2.4
   */
  @Parameter(property = "maven.surefire.debug")
  private String debugForkedProcess;

  /**
   * Kill the forked test process after a certain number of seconds. If set to
   * 0, wait forever for the process, never
   * timing out.
   * 
   * @since 2.4
   */
  @Parameter(property = "surefire.timeout")
  private int forkedProcessTimeoutInSeconds;

  /**
   * Additional environments to set on the command line.
   * 
   * @since 2.1.3
   */
  @Parameter
  private Map environmentVariables = new HashMap();

  /**
   * Command line working directory.
   * 
   * @since 2.1.3
   */
  @Parameter(property = "basedir")
  private File workingDirectory;

  /**
   * When false it makes tests run using the standard classloader delegation
   * instead of the default Maven isolated
   * classloader. Only used when forking (forkMode is not "none").<br/>
   * Setting it to false helps with some problems caused by conflicts between
   * xml parsers in the classpath and the
   * Java 5 provider parser.
   * 
   * @since 2.1
   */
  @Parameter(property = "childDelegation")
  private boolean childDelegation;

  /**
   * (TestNG only) Groups for this test. Only classes/methods/etc decorated with
   * one of the groups specified here will
   * be included in test run, if specified. This parameter is overridden if
   * suiteXmlFiles are specified.
   *
   * @since 2.2
   */
  @Parameter(property = "groups")
  private String groups;

  /**
   * (TestNG only) Excluded groups. Any methods/classes/etc with one of the
   * groups specified in this list will
   * specifically not be run. This parameter is overridden if suiteXmlFiles are
   * specified.
   *
   * @since 2.2
   */
  @Parameter(property = "excludedGroups")
  private String excludedGroups;

  /**
   * (TestNG only) List of TestNG suite xml file locations, seperated by commas.
   * Note that suiteXmlFiles is
   * incompatible with several other parameters on this plugin, like
   * includes/excludes. This parameter is ignored if
   * the "test" parameter is specified (allowing you to run a single test
   * instead of an entire suite).
   * 
   * @since 2.2
   */
  @Parameter
  private File[] suiteXmlFiles;

  /**
   * Allows you to specify the name of the JUnit artifact. If not set,
   * <code>junit:junit</code> will be used.
   * 
   * @since 2.3.1
   */
  @Parameter(property = "junitArtifactName", defaultValue = "junit:junit")
  private String junitArtifactName;

  /**
   * Allows you to specify the name of the TestNG artifact. If not set,
   * <code>org.testng:testng</code> will be used.
   * 
   * @since 2.3.1
   */
  @Parameter(property = "testNGArtifactName", defaultValue = "org.testng:testng")
  private String testNGArtifactName;

  /**
   * (TestNG only) The attribute thread-count allows you to specify how many
   * threads should be allocated for this
   * execution. Only makes sense to use in conjunction with parallel.
   * 
   * @since 2.2
   */
  @Parameter(property = "threadCount")
  private int threadCount;

  /**
   * (TestNG only) When you use the parallel attribute, TestNG will try to run
   * all your test methods in separate
   * threads, except for methods that depend on each other, which will be run in
   * the same thread in order to respect
   * their order of execution.
   * 
   * @todo test how this works with forking, and console/file output parallelism
   * @since 2.2
   */
  @Parameter(property = "parallel")
  private String parallel;

  /**
   * Whether to trim the stack trace in the reports to just the lines within the
   * test, or show the full trace.
   * 
   * @since 2.2
   */
  @Parameter(property = "trimStackTrace", defaultValue = "true")
  private boolean trimStackTrace = true;

  /**
   * Creates the artifact
   */
  @Component
  private ArtifactFactory artifactFactory;

  /**
   * For retrieval of artifact's metadata.
   */
  @Component
  private ArtifactMetadataSource metadataSource;

  private Properties originalSystemProperties;

  /**
   * Flag to disable the generation of report files in xml format.
   * 
   * @since 2.2
   */
  @Parameter(property = "disableXmlReport")
  private boolean disableXmlReport;

  /**
   * Option to pass dependencies to the system's classloader instead of using an
   * isolated class loader when forking.
   * Prevents problems with JDKs which implement the service provider lookup
   * mechanism by using the system's
   * classloader. Default value is "true".
   * 
   * @since 2.3
   */
  @Parameter(property = "surefire.useSystemClassLoader")
  private Boolean useSystemClassLoader;

  /**
   * By default, Surefire forks your tests using a manifest-only jar; set this
   * parameter to "false" to force it to
   * launch your tests with a plain old Java classpath. (See
   * http://maven.apache.org/plugins/maven-surefire-plugin/examples/class-
   * loading.html for a more detailed explanation
   * of manifest-only jars and their benefits.) Default value is "true". Beware,
   * setting this to "false" may cause
   * your tests to fail on Windows if your classpath is too long.
   * 
   * @since 2.4.3
   */
  @Parameter(property = "surefire.useManifestOnlyJar", defaultValue = "true")
  private boolean useManifestOnlyJar = true;

  /**
   * By default, Surefire enables JVM assertions for the execution of your test
   * cases. To disable the assertions, set
   * this flag to <code>false</code>.
   * 
   * @since 2.3.1
   */
  @Parameter(property = "enableAssertions", defaultValue = "true")
  private boolean enableAssertions;

  /**
   * The current build session instance.
   */
  @Component
  private MavenSession session;

  private void addArtifact(final SurefireBooter surefireBooter, final Artifact surefireArtifact)
      throws ArtifactNotFoundException, ArtifactResolutionException {
    final ArtifactResolutionResult result = resolveArtifact(null, surefireArtifact);

    for (final Object element : result.getArtifacts()) {
      final Artifact artifact = (Artifact) element;

      getLog().debug("Adding to surefire booter test classpath: " + artifact.getFile().getAbsolutePath());

      surefireBooter.addSurefireBootClassPathUrl(artifact.getFile().getAbsolutePath());
    }
  }

  private void addProvider(final SurefireBooter surefireBooter, final String provider, final String version,
      final Artifact filteredArtifact) throws ArtifactNotFoundException, ArtifactResolutionException {
    final Artifact providerArtifact = this.artifactFactory.createDependencyArtifact("org.apache.maven.surefire",
        provider, VersionRange.createFromVersion(version), "jar", null, Artifact.SCOPE_TEST);
    final ArtifactResolutionResult result = resolveArtifact(filteredArtifact, providerArtifact);

    for (final Object element : result.getArtifacts()) {
      final Artifact artifact = (Artifact) element;

      getLog().debug("Adding to surefire test classpath: " + artifact.getFile().getAbsolutePath());

      surefireBooter.addSurefireClassPathUrl(artifact.getFile().getAbsolutePath());
    }
  }

  /**
   * <p>
   * Adds Reporters that will generate reports with different formatting.
   * <p>
   * The Reporter that will be added will be based on the value of the parameter
   * useFile, reportFormat, and printSummary.
   * 
   * @param surefireBooter
   *          The surefire booter that will run tests.
   * @param forking
   */
  private void addReporters(final SurefireBooter surefireBooter, final boolean forking) {
    final Boolean trimStackTrace = Boolean.valueOf(this.trimStackTrace);
    if (this.useFile) {
      if (this.printSummary) {
        if (forking) {
          surefireBooter.addReport(ForkingConsoleReporter.class.getName(), new Object[] {
            trimStackTrace
          });
        } else {
          surefireBooter.addReport(ConsoleReporter.class.getName(), new Object[] {
            trimStackTrace
          });
        }
      }

      if (BRIEF_REPORT_FORMAT.equals(this.reportFormat)) {
        surefireBooter.addReport(BriefFileReporter.class.getName(), new Object[] {
            this.reportsDirectory, trimStackTrace
        });
      } else if (PLAIN_REPORT_FORMAT.equals(this.reportFormat)) {
        surefireBooter.addReport(FileReporter.class.getName(), new Object[] {
            this.reportsDirectory, trimStackTrace
        });
      }
    } else {
      if (BRIEF_REPORT_FORMAT.equals(this.reportFormat)) {
        surefireBooter.addReport(BriefConsoleReporter.class.getName(), new Object[] {
          trimStackTrace
        });
      } else if (PLAIN_REPORT_FORMAT.equals(this.reportFormat)) {
        surefireBooter.addReport(DetailedConsoleReporter.class.getName(), new Object[] {
          trimStackTrace
        });
      }
    }

    if (!this.disableXmlReport) {
      surefireBooter.addReport(XMLReporter.class.getName(), new Object[] {
          this.reportsDirectory, trimStackTrace
      });
    }
  }

  private SurefireBooter constructSurefireBooter() throws MojoExecutionException, MojoFailureException {
    final SurefireBooter surefireBooter = new SurefireBooter();

    final Artifact surefireArtifact = (Artifact) this.pluginArtifactMap
        .get("org.apache.maven.surefire:surefire-booter");
    if (surefireArtifact == null) {
      throw new MojoExecutionException("Unable to locate surefire-booter in the list of plugin artifacts");
    }

    surefireArtifact.isSnapshot(); // TODO: this is ridiculous, but it fixes
                                   // getBaseVersion to be -SNAPSHOT if
    // needed

    Artifact junitArtifact;
    Artifact testNgArtifact;
    try {
      addArtifact(surefireBooter, surefireArtifact);

      junitArtifact = (Artifact) this.projectArtifactMap.get(this.junitArtifactName);
      // SUREFIRE-378, junit can have an alternate artifact name
      if (junitArtifact == null && "junit:junit".equals(this.junitArtifactName)) {
        junitArtifact = (Artifact) this.projectArtifactMap.get("junit:junit-dep");
      }

      // TODO: this is pretty manual, but I'd rather not require the plugin >
      // dependencies section right now
      testNgArtifact = (Artifact) this.projectArtifactMap.get(this.testNGArtifactName);

      if (testNgArtifact != null) {
        final VersionRange range = VersionRange.createFromVersionSpec("[4.7,)");
        if (!range.containsVersion(new DefaultArtifactVersion(testNgArtifact.getVersion()))) {
          throw new MojoFailureException("TestNG support requires version 4.7 or above. You have declared version "
              + testNgArtifact.getVersion());
        }

        convertTestNGParameters();

        if (this.testClassesDirectory != null) {
          this.properties.setProperty("testng.test.classpath", this.testClassesDirectory.getAbsolutePath());
        }

        addArtifact(surefireBooter, testNgArtifact);

        // The plugin uses a JDK based profile to select the right testng. We
        // might be explicity using a
        // different one since its based on the source level, not the JVM. Prune
        // using the filter.
        addProvider(surefireBooter, "surefire-testng", surefireArtifact.getBaseVersion(), testNgArtifact);
      } else if (junitArtifact != null && junitArtifact.getBaseVersion().startsWith("4")) {
        addProvider(surefireBooter, "surefire-junit4", surefireArtifact.getBaseVersion(), null);
      } else {
        // add the JUnit provider as default - it doesn't require JUnit to be
        // present,
        // since it supports POJO tests.
        addProvider(surefireBooter, "surefire-junit", surefireArtifact.getBaseVersion(), null);
      }
    } catch (final ArtifactNotFoundException e) {
      throw new MojoExecutionException("Unable to locate required surefire provider dependency: " + e.getMessage(), e);
    } catch (final InvalidVersionSpecificationException e) {
      throw new MojoExecutionException("Error determining the TestNG version requested: " + e.getMessage(), e);
    } catch (final ArtifactResolutionException e) {
      throw new MojoExecutionException("Error to resolving surefire provider dependency: " + e.getMessage(), e);
    }

    if (this.suiteXmlFiles != null && this.suiteXmlFiles.length > 0 && this.test == null) {
      if (testNgArtifact == null) {
        throw new MojoExecutionException("suiteXmlFiles is configured, but there is no TestNG dependency");
      }

      // TODO: properties should be passed in here too
      surefireBooter.addTestSuite("org.apache.maven.surefire.testng.TestNGXmlTestSuite", new Object[] {
          this.suiteXmlFiles, this.testSourceDirectory.getAbsolutePath(), testNgArtifact.getBaseVersion(),
          testNgArtifact.getClassifier(), this.properties, this.reportsDirectory
      });
    } else {
      List includeList;
      List excludeList;

      if (this.test != null) {
        // Check to see if we are running a single test. The raw parameter will
        // come through if it has not been set.

        // FooTest -> **/FooTest.java

        includeList = new ArrayList();

        excludeList = new ArrayList();

        if (this.failIfNoTests == null) {
          this.failIfNoTests = Boolean.TRUE;
        }

        final String[] testRegexes = StringUtils.split(this.test, ",");

        for (final String testRegexe : testRegexes) {
          String testRegex = testRegexe;
          if (testRegex.endsWith(".java")) {
            testRegex = testRegex.substring(0, testRegex.length() - 5);
          }
          // Allow paths delimited by '.' or '/'
          testRegex = testRegex.replace('.', '/');
          includeList.add("**/" + testRegex + ".java");
        }
      } else {
        includeList = this.includes;

        excludeList = this.excludes;

        // defaults here, qdox doesn't like the end javadoc value
        // Have to wrap in an ArrayList as surefire expects an ArrayList instead
        // of a List for some reason
        if (includeList == null || includeList.size() == 0) {
          includeList = new ArrayList(Arrays.asList(new String[] {
              "**/Test*.java", "**/*Test.java", "**/*TestCase.java"
          }));
        }
        if (excludeList == null || excludeList.size() == 0) {
          excludeList = new ArrayList(Arrays.asList(new String[] {
            "**/*$*"
          }));
        }
      }

      if (testNgArtifact != null) {
        surefireBooter.addTestSuite("org.apache.maven.surefire.testng.TestNGDirectoryTestSuite", new Object[] {
            this.testClassesDirectory, includeList, excludeList, this.testSourceDirectory.getAbsolutePath(),
            testNgArtifact.getBaseVersion(), testNgArtifact.getClassifier(), this.properties, this.reportsDirectory
        });
      } else {
        String junitDirectoryTestSuite;
        if (junitArtifact != null && junitArtifact.getBaseVersion() != null
            && junitArtifact.getBaseVersion().startsWith("4")) {
          junitDirectoryTestSuite = "org.apache.maven.surefire.junit4.JUnit4DirectoryTestSuite";
        } else {
          junitDirectoryTestSuite = "org.apache.maven.surefire.junit.JUnitDirectoryTestSuite";
        }

        // fall back to JUnit, which also contains POJO support. Also it can run
        // classes compiled against JUnit since it has a dependency on JUnit
        // itself.
        surefireBooter.addTestSuite(junitDirectoryTestSuite, new Object[] {
            this.testClassesDirectory, includeList, excludeList
        });
      }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    getLog().debug("Test Classpath :");

    // Check if we need to add configured classes/test classes directories here.
    // If they are configured, we should remove the default to avoid conflicts.
    if (!this.project.getBuild().getOutputDirectory().equals(this.classesDirectory.getAbsolutePath())) {
      this.classpathElements.remove(this.project.getBuild().getOutputDirectory());
      this.classpathElements.add(this.classesDirectory.getAbsolutePath());
    }
    if (!this.project.getBuild().getTestOutputDirectory().equals(this.testClassesDirectory.getAbsolutePath())) {
      this.classpathElements.remove(this.project.getBuild().getTestOutputDirectory());
      this.classpathElements.add(this.testClassesDirectory.getAbsolutePath());
    }

    for (final Object classpathElement1 : this.classpathElements) {
      final String classpathElement = (String) classpathElement1;

      getLog().debug("  " + classpathElement);

      surefireBooter.addClassPathUrl(classpathElement);
    }

    final Toolchain tc = getToolchain();

    if (tc != null) {
      getLog().info("Toolchain in surefire-plugin: " + tc);
      if (ForkConfiguration.FORK_NEVER.equals(this.forkMode)) {
        this.forkMode = ForkConfiguration.FORK_ONCE;
      }
      if (this.jvm != null) {
        getLog().warn("Toolchains are ignored, 'executable' parameter is set to " + this.jvm);
      } else {
        this.jvm = tc.findTool("java"); // NOI18N
      }
    }

    if (this.additionalClasspathElements != null) {
      for (final Object additionalClasspathElement : this.additionalClasspathElements) {
        final String classpathElement = (String) additionalClasspathElement;

        getLog().debug("  " + classpathElement);

        surefireBooter.addClassPathUrl(classpathElement);
      }
    }

    // ----------------------------------------------------------------------
    // Forking
    // ----------------------------------------------------------------------

    final ForkConfiguration fork = new ForkConfiguration();

    // DUNS
    if (this.project.getPackaging().equals("nar") || getNarArtifacts().size() > 0) {
      this.forkMode = "pertest";
    }

    fork.setForkMode(this.forkMode);

    processSystemProperties(!fork.isForking());

    if (getLog().isDebugEnabled()) {
      showMap(this.systemProperties, "system property");
    }

    if (fork.isForking()) {
      this.useSystemClassLoader = this.useSystemClassLoader == null ? Boolean.TRUE : this.useSystemClassLoader;
      fork.setUseSystemClassLoader(this.useSystemClassLoader.booleanValue());
      fork.setUseManifestOnlyJar(this.useManifestOnlyJar);

      fork.setSystemProperties(this.systemProperties);

      if ("true".equals(this.debugForkedProcess)) {
        this.debugForkedProcess = "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005";
      }

      fork.setDebugLine(this.debugForkedProcess);

      if (this.jvm == null || "".equals(this.jvm)) {
        // use the same JVM as the one used to run Maven (the "java.home" one)
        this.jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        getLog().debug("Using JVM: " + this.jvm);
      }

      fork.setJvmExecutable(this.jvm);

      if (this.workingDirectory != null) {
        fork.setWorkingDirectory(this.workingDirectory);
      } else {
        fork.setWorkingDirectory(this.basedir);
      }

      // BEGINDUNS
      if (this.argLine == null) {
        this.argLine = "";
      }

      final StringBuffer javaLibraryPath = new StringBuffer();
      if (testJNIModule()) {
        // Add libraries to java.library.path for testing
        final File jniLibraryPathEntry = getLayout().getLibDirectory(getTargetDirectory(),
            getMavenProject().getArtifactId(), getMavenProject().getVersion(), getAOL().toString(), Library.JNI);
        if (jniLibraryPathEntry.exists()) {
          getLog().debug("Adding library directory to java.library.path: " + jniLibraryPathEntry);
          if (javaLibraryPath.length() > 0) {
            javaLibraryPath.append(File.pathSeparator);
          }
          javaLibraryPath.append(jniLibraryPathEntry);
        }

        final File sharedLibraryPathEntry = getLayout().getLibDirectory(getTargetDirectory(),
            getMavenProject().getArtifactId(), getMavenProject().getVersion(), getAOL().toString(), Library.SHARED);
        if (sharedLibraryPathEntry.exists()) {
          getLog().debug("Adding library directory to java.library.path: " + sharedLibraryPathEntry);
          if (javaLibraryPath.length() > 0) {
            javaLibraryPath.append(File.pathSeparator);
          }
          javaLibraryPath.append(sharedLibraryPathEntry);
        }

        // add jar file to classpath, as one may want to read a
        // properties file for artifactId and version
        final String narFile = "target/" + this.project.getArtifactId() + "-" + this.project.getVersion() + ".jar";
        getLog().debug("Adding to surefire test classpath: " + narFile);
        surefireBooter.addClassPathUrl(narFile);
      }

      final List dependencies = getNarArtifacts(); // TODO: get seems heavy, not
                                                   // sure if we can push this
                                                   // up to before the fork to
                                                   // use it multiple times.
      for (final Object dependency1 : dependencies) {
        final NarArtifact dependency = (NarArtifact) dependency1;
        // FIXME this should be overridable
        // NarInfo info = dependency.getNarInfo();
        // String binding = info.getBinding(getAOL(), Library.STATIC);
        // NOTE: fixed to shared, jni
        final String[] bindings = { Library.SHARED, Library.JNI
        };
        for (final String binding2 : bindings) {
          final String binding = binding2;
          if (!binding.equals(Library.STATIC)) {
            final File depLibPathEntry = getLayout()
                .getLibDirectory(getUnpackDirectory(), dependency.getArtifactId(), dependency.getBaseVersion(),
                    getAOL().toString(), binding);
            //dependency.getVersion() calls the maven super class, which is not used when
            //unpacking the NarDependencies in AbstractDependencyMojo.  This causes
            //the path to not exist and not be added to the library path.
            if (depLibPathEntry.exists()) {
              getLog().debug("Adding dependency directory to java.library.path: " + depLibPathEntry);
              if (javaLibraryPath.length() > 0) {
                javaLibraryPath.append(File.pathSeparator);
              }
              javaLibraryPath.append(depLibPathEntry);
            }
          }
        }
      }

      // add final javalibrary path
      if (javaLibraryPath.length() > 0) {
        // NOTE java.library.path only works for the jni lib itself, and
        // not for its dependent shareables.
        // NOTE: java.library.path does not work with arguments with
        // spaces as
        // SureFireBooter splits the line in parts and then quotes
        // it wrongly
        NarUtil.addLibraryPathToEnv(javaLibraryPath.toString(), this.environmentVariables, getOS());
      }

      // necessary to find WinSxS
      if (getOS().equals(OS.WINDOWS)) {
        this.environmentVariables.put("SystemRoot", NarUtil.getEnv("SystemRoot", "SystemRoot", "C:\\Windows"));
      }
      // ENDDUNS

      fork.setArgLine(this.argLine);

      fork.setEnvironmentVariables(this.environmentVariables);

      if (getLog().isDebugEnabled()) {
        showMap(this.environmentVariables, "environment variable");

        fork.setDebug(true);
      }

      if (this.argLine != null) {
        final List args = Arrays.asList(this.argLine.split(" "));
        if (args.contains("-da") || args.contains("-disableassertions")) {
          this.enableAssertions = false;
        }
      }
    }

    surefireBooter.setFailIfNoTests(this.failIfNoTests == null ? false : this.failIfNoTests.booleanValue());

    surefireBooter.setForkedProcessTimeoutInSeconds(this.forkedProcessTimeoutInSeconds);

    surefireBooter.setRedirectTestOutputToFile(this.redirectTestOutputToFile);

    surefireBooter.setForkConfiguration(fork);

    surefireBooter.setChildDelegation(this.childDelegation);

    surefireBooter.setEnableAssertions(this.enableAssertions);

    surefireBooter.setReportsDirectory(this.reportsDirectory);

    addReporters(surefireBooter, fork.isForking());

    return surefireBooter;
  }

  /**
   * Converts old TestNG configuration parameters over to new properties based
   * configuration method. (if any are
   * defined the old way)
   */
  private void convertTestNGParameters() {
    if (this.properties == null) {
      this.properties = new Properties();
    }

    if (this.parallel != null) {
      this.properties.setProperty("parallel", this.parallel);
    }
    if (this.excludedGroups != null) {
      this.properties.setProperty("excludegroups", this.excludedGroups);
    }
    if (this.groups != null) {
      this.properties.setProperty("groups", this.groups);
    }

    if (this.threadCount > 0) {
      this.properties.setProperty("threadcount", Integer.toString(this.threadCount));
    }
  }

  /**
   * List the dependencies needed for integration tests executions, those
   * dependencies are used to declare the paths
   * of shared and jni libraries in java.library.path
   */
  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    return new ScopeFilter( Artifact.SCOPE_TEST, null );
  }

  // TODO remove the part with ToolchainManager lookup once we depend on
  // 3.0.9 (have it as prerequisite). Define as regular component field then.
  private Toolchain getToolchain() {
    Toolchain tc = null;
    try {
      if (this.session != null) // session is null in tests..
      {
        final ToolchainManager toolchainManager = (ToolchainManager) this.session.getContainer().lookup(
            ToolchainManager.ROLE);
        if (toolchainManager != null) {
          tc = toolchainManager.getToolchainFromBuildContext("jdk", this.session);
        }
      }
    } catch (final ComponentLookupException componentLookupException) {
      // just ignore, could happen in pre-3.0.9 builds..
    }
    return tc;
  }

  @Override
  protected File getUnpackDirectory() {
    return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
  }

  /**
   * @return SurefirePlugin Returns the skipExec.
   */
  public boolean isSkipExec() {
    return this.skipNarTests;
  }

  // DUNS, changed name
  @Override
  public void narExecute() throws MojoExecutionException, MojoFailureException {
    if (this.skipTests) {
      getLog().info("Tests are skipped");
    } else if (verifyParameters()) {

      final SurefireBooter surefireBooter = constructSurefireBooter();

      getLog().info("Surefire report directory: " + this.reportsDirectory);

      int result;
      try {
        result = surefireBooter.run();
      } catch (final SurefireBooterForkException | SurefireExecutionException e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }

      if (this.originalSystemProperties != null && !surefireBooter.isForking()) {
        // restore system properties, only makes sense when not forking..
        System.setProperties(this.originalSystemProperties);
      }

      if (result == 0) {
        return;
      }

      String msg;

      if (result == SurefireBooter.NO_TESTS_EXIT_CODE) {
        if (this.failIfNoTests == null || !this.failIfNoTests.booleanValue()) {
          return;
        }
        // TODO: i18n
        throw new MojoFailureException("No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)");
      } else {
        // TODO: i18n
        msg = "There are test failures.\n\nPlease refer to " + this.reportsDirectory
            + " for the individual test results.";

      }

      if (this.testFailureIgnore) {
        getLog().error(msg);
      } else {
        throw new MojoFailureException(msg);
      }
    }
  }

  protected void processSystemProperties(final boolean setInSystem) {
    if (this.systemProperties == null) {
      this.systemProperties = new Properties();
    }

    this.originalSystemProperties = (Properties) System.getProperties().clone();

    // We used to take all of our system properties and dump them in with the
    // user specified properties for SUREFIRE-121, causing SUREFIRE-491.
    // Not gonna do THAT any more... but I'm leaving this code here in case
    // we need it later when we try to fix SUREFIRE-121 again.

    // Get the properties from the MavenSession instance to make embedded use
    // work correctly
    final Properties userSpecifiedProperties = (Properties) this.session.getExecutionProperties().clone();
    userSpecifiedProperties.putAll(this.systemProperties);
    // systemProperties = userSpecifiedProperties;

    this.systemProperties.setProperty("basedir", this.basedir.getAbsolutePath());
    this.systemProperties.setProperty("user.dir", this.workingDirectory.getAbsolutePath());

    // DUNS, use access method rather than "localRepository" field.
    this.systemProperties.setProperty("localRepository", getLocalRepository().getBasedir());

    if (setInSystem) {
      // Add all system properties configured by the user

      for (final Object o : this.systemProperties.keySet()) {
        final String key = (String) o;

        final String value = this.systemProperties.getProperty(key);

        System.setProperty(key, value);
      }
    }
  }

  private ArtifactResolutionResult resolveArtifact(final Artifact filteredArtifact, final Artifact providerArtifact)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    ArtifactFilter filter = null;
    if (filteredArtifact != null) {
      filter = new ExcludesArtifactFilter(Collections.singletonList(filteredArtifact.getGroupId() + ":"
          + filteredArtifact.getArtifactId()));
    }

    final Artifact originatingArtifact = this.artifactFactory.createBuildArtifact("dummy", "dummy", "1.0", "jar");

    // DUNS, use access method rather than "localRepository" field.
    return this.artifactResolver.resolveTransitively(Collections.singleton(providerArtifact), originatingArtifact,
        getLocalRepository(), getRemoteRepositories(), this.metadataSource, filter);
  }

  /**
   * @param skipExec
   *          the skipExec to set
   */
  public void setSkipExec(final boolean skipExec) {
    this.skipNarTests = skipExec;
  }

  private void showMap(final Map map, final String setting) {
    for (final Object o : map.keySet()) {
      final String key = (String) o;
      final String value = (String) map.get(key);
      getLog().debug("Setting " + setting + " [" + key + "]=[" + value + "]");
    }
  }

  // DUNS added test for JNI module
  private boolean testJNIModule() {
    for (final Object element : getLibraries()) {
      final Library lib = (Library) element;
      final String type = lib.getType();
      if (type.equals(Library.JNI) || type.equals(Library.SHARED)) {
        return true;
      }
    }
    return false;
  }

  private boolean verifyParameters() throws MojoFailureException {
    // DUNS
    if (this.skipNar || this.skipNarTests || this.skipNarExec) {
      getLog().info("Tests are skipped.");
      return false;
    }

    if (!this.testClassesDirectory.exists()) {
      if (this.failIfNoTests != null && this.failIfNoTests.booleanValue()) {
        throw new MojoFailureException("No tests to run!");
      }
      getLog().info("No tests to run.");
      return false;
    }

    if (this.useSystemClassLoader != null && ForkConfiguration.FORK_NEVER.equals(this.forkMode)) {
      getLog().warn("useSystemClassloader setting has no effect when not forking");
    }

    return true;
  }
}
