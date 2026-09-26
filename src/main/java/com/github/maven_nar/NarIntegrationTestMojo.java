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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.toolchain.ToolchainManager;

/**
 * Run Java integration tests against packaged native libraries. NAR prepares
 * native library paths and isolation; the released Surefire goal discovers and
 * executes tests and writes reports. Test failures fail this phase immediately.
 */
@Mojo(name = "nar-integration-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  requiresDependencyResolution = ResolutionScope.TEST)
public class NarIntegrationTestMojo extends AbstractDependencyMojo {
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
   * Deprecated alias for skipNarTests, bound to nar.test.skip.exec.
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
  @Parameter(defaultValue = "${basedir}", required = true)
  private File basedir;

  /**
   * The directory containing generated test classes of the project being
   * tested.
   */
  @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
  private File testClassesDirectory;

  /**
   * The directory containing generated classes of the project being tested.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  /**
   * Additional elements to be appended to the classpath.
   * 
   * @since 2.4
   */
  @Parameter
  private List<String> additionalClasspathElements;

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
  @Parameter(defaultValue = "${project.build.testSourceDirectory}", required = true)
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
  private List<String> includes;

  /**
   * List of patterns (separated by commas) used to specify the tests that
   * should be excluded in testing. When not
   * specified and when the <code>test</code> parameter is not specified, the
   * default excludes will be <code>**&#47;*$*</code> (which excludes all inner
   * classes). This parameter is ignored if TestNG suiteXmlFiles are
   * specified.
   */
  @Parameter
  private List<String> excludes;

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
  private Map<String, String> environmentVariables = new HashMap<>();

  /**
   * Command line working directory.
   * 
   * @since 2.1.3
   */
  @Parameter(defaultValue = "${basedir}")
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
  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession session;

  /** Maven configures and executes the real Surefire mojo in its own realm. */
  @Component
  private BuildPluginManager pluginManager;

  @Component
  private ToolchainManager toolchainManager;

  @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
  private MojoExecution mojoExecution;

  /** Modern fork count; overrides forkMode for Java-only projects. */
  @Parameter(property = "nar.test.forkCount")
  private String forkCount;

  /** Reuse forks for Java-only projects; native tests always use fresh forks. */
  @Parameter(property = "nar.test.reuseForks")
  private Boolean reuseForks;

  /** Select the child JDK using Surefire's toolchain requirements. */
  @Parameter
  private Map<String, String> jdkToolchain;

  /** System properties override legacy systemProperties and the properties file. */
  @Parameter
  private Map<String, String> systemPropertyVariables;

  /** Optional test system properties file. */
  @Parameter(property = "nar.test.systemPropertiesFile")
  private File systemPropertiesFile;

  /** Preserve the historical classpath default, including on modular JDKs. */
  @Parameter(property = "nar.test.useModulePath", defaultValue = "false")
  private boolean useModulePath;

  /** Opt in to passing Maven command-line properties into the test JVM. */
  @Parameter(property = "nar.test.promoteUserPropertiesToSystemProperties", defaultValue = "false")
  private boolean promoteUserPropertiesToSystemProperties;

  /** Explicit provider/engine dependencies for the delegated Surefire plugin. */
  @Parameter
  private List<Dependency> surefirePluginDependencies;

  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    return new ScopeFilter(Artifact.SCOPE_TEST, null);
  }

  @Override
  protected File getUnpackDirectory() {
    return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
  }

  public boolean isSkipExec() {
    return this.skipNarTests;
  }

  public void setSkipExec(final boolean skipExec) {
    this.skipNarTests = skipExec;
  }

  @Override
  public void narExecute() throws MojoExecutionException, MojoFailureException {
    if (this.skipTests || this.dryRun || this.skipNar || this.skipNarTests || this.skipNarExec) {
      getLog().info("Tests are skipped.");
      return;
    }
    // Preserve the historical missing-directory case, even with an explicit test selector.
    if (!this.testClassesDirectory.exists()) {
      if (Boolean.TRUE.equals(this.failIfNoTests)) {
        throw new MojoFailureException("No tests to run!");
      }
      getLog().info("No tests to run.");
      return;
    }

    final NarIntegrationTestSupport nativeTests = new NarIntegrationTestSupport(this);
    final boolean toolchainSelected = (this.jdkToolchain != null && !this.jdkToolchain.isEmpty())
        || this.toolchainManager.getToolchainFromBuildContext("jdk", this.session) != null;
    final NarSurefireConfiguration config = new NarSurefireConfiguration();
    config.forks(this.forkMode, this.forkCount, this.reuseForks, nativeTests.requiresIsolation(), toolchainSelected);
    config.value("skip", false).value("skipTests", false).value("skipExec", false);
    config.value("basedir", this.basedir).value("testClassesDirectory", this.testClassesDirectory)
        .value("classesDirectory", this.classesDirectory).value("testSourceDirectory", this.testSourceDirectory)
        .value("reportsDirectory", this.reportsDirectory).value("workingDirectory", this.workingDirectory);
    final boolean noTests = this.failIfNoTests == null ? this.test != null : this.failIfNoTests;
    config.value("failIfNoTests", noTests).value("failIfNoSpecifiedTests", noTests)
        .value("testFailureIgnore", this.testFailureIgnore).value("test", this.test);
    config.list("includes", "include", this.includes == null || this.includes.isEmpty()
        ? Arrays.asList("**/Test*.java", "**/*Test.java", "**/*TestCase.java") : this.includes);
    config.list("excludes", "exclude", this.excludes == null || this.excludes.isEmpty()
        ? Arrays.asList("**/*$*") : this.excludes);
    if (this.test == null && this.suiteXmlFiles != null) {
      config.list("suiteXmlFiles", "suiteXmlFile", Arrays.asList(this.suiteXmlFiles));
    }
    final List<String> classpath = new ArrayList<>();
    if (this.additionalClasspathElements != null) {
      classpath.addAll(this.additionalClasspathElements);
    }
    nativeTests.addProjectArtifact(classpath);
    config.list("additionalClasspathElements", "additionalClasspathElement", classpath);
    config.map("environmentVariables", nativeTests.environment(this.environmentVariables, System.getenv()));
    config.properties("properties", this.properties);
    config.properties("systemProperties", this.systemProperties);
    config.value("systemPropertiesFile", this.systemPropertiesFile);
    final Map<String, String> variables = new HashMap<>();
    if (this.systemPropertyVariables != null) {
      variables.putAll(this.systemPropertyVariables);
    }
    variables.put("basedir", this.basedir.getAbsolutePath());
    variables.put("user.dir", this.workingDirectory.getAbsolutePath());
    variables.put("localRepository", getLocalRepository().getBasedir());
    // Surefire's duplicate-execution checksum includes properties, but not execution IDs.
    variables.put("nar.integrationTest.executionId", this.mojoExecution.getExecutionId());
    config.map("systemPropertyVariables", variables);
    config.map("jdkToolchain", this.jdkToolchain);
    config.value("jvm", this.jvm).value("argLine", this.argLine).value("debugForkedProcess", this.debugForkedProcess)
        .value("forkedProcessTimeoutInSeconds", this.forkedProcessTimeoutInSeconds)
        .value("childDelegation", this.childDelegation).value("groups", this.groups)
        .value("excludedGroups", this.excludedGroups).value("junitArtifactName", this.junitArtifactName)
        .value("testNGArtifactName", this.testNGArtifactName).value("threadCount", this.threadCount)
        .value("parallel", this.parallel).value("trimStackTrace", this.trimStackTrace)
        .value("printSummary", this.printSummary).value("reportFormat", this.reportFormat)
        .value("useFile", this.useFile).value("redirectTestOutputToFile", this.redirectTestOutputToFile)
        .value("disableXmlReport", this.disableXmlReport)
        .value("useSystemClassLoader", this.useSystemClassLoader == null ? true : this.useSystemClassLoader)
        .value("useManifestOnlyJar", this.useManifestOnlyJar).value("enableAssertions", this.enableAssertions)
        .value("useModulePath", this.useModulePath)
        .value("promoteUserPropertiesToSystemProperties", this.promoteUserPropertiesToSystemProperties);
    new SurefireGoalInvoker(this.pluginManager).execute(this.session, getMavenProject(),
        "nar-" + this.mojoExecution.getExecutionId(), config.toDom(), this.surefirePluginDependencies);
  }
}
