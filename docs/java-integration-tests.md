# Java/JNI integration tests

Building and running NAR requires Java 8 and Maven 3.6.3 or newer. Forked test
JVMs also require Java 8 or newer.
`nar-integration-test` runs Java tests during `integration-test`, after native
libraries and their Java archives have been packaged. `nar-test` continues to
run native executables.

NAR uses the released `maven-surefire-plugin:3.5.6:test` goal internally. NAR
prepares native paths and configuration; Surefire handles discovery, providers,
JVMs and reports. Configure these tests on **NAR's** plugin, independently from
the project's ordinary Surefire configuration. Changing the ordinary Surefire
plugin version does not change NAR's runner.

## Jupiter

Add a Jupiter engine and its matching API as test dependencies, for example:

```xml
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.12.2</version>
  <scope>test</scope>
</dependency>
```

Jupiter implements `@TempDir`, extensions, parameter injection and its other
annotations. NAR does not interpret these annotations. The portable regression
also exercises Jupiter 5.10.3.

For `nar` packaging, the normal lifecycle already binds `nar-integration-test`.
For a Java project consuming NAR libraries, keep the download/unpack lifecycle
configuration and bind the goal explicitly:

```xml
<execution>
  <id>native-integration-tests</id>
  <goals><goal>nar-integration-test</goal></goals>
</execution>
```

Use `-DskipTests` to skip ordinary Surefire while retaining NAR integration tests.
Use `-DskipNarTests` to skip NAR integration tests. `-Dmaven.test.skip=true` skips
both test compilation and NAR's Java tests.

## Framework policy

Surefire 3.5.6 retains separate legacy providers. This is deliberate: the
[3.6.0 provider transition](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html)
must be evaluated as a separate compatibility change.

| Tests | Configuration |
| --- | --- |
| JUnit 3 / POJO | Legacy provider selection remains available; tested with JUnit 3.8.2 and a dependency-free POJO. |
| JUnit 4 | Tested with 4.8.2 and 4.13.2. |
| TestNG | Use TestNG's provider; tested with 5.14.9 and 7.5.1, groups and XML suites. Surefire's version check still accepts 4.7+, but older versions are not covered by this suite. |
| Jupiter | Include a matching Platform engine. Tested with Jupiter 5.10.3 and 5.12.2. |
| Jupiter + JUnit 3/4 | Add a matching `junit-vintage-engine`. Tested with Jupiter/Vintage 5.10.3 and JUnit 4.13.2. |
| Jupiter + TestNG | Explicitly configure both providers and align the Platform dependencies. Automatic selection does not promise to run both families. |

The explicit-provider option is `surefirePluginDependencies`. For Jupiter
5.12.2 and TestNG 7.5.1, the tested configuration is:

```xml
<surefirePluginDependencies>
  <dependency>
    <groupId>org.apache.maven.surefire</groupId>
    <artifactId>surefire-junit-platform</artifactId>
    <version>3.5.6</version>
  </dependency>
  <dependency>
    <groupId>org.apache.maven.surefire</groupId>
    <artifactId>surefire-testng</artifactId>
    <version>3.5.6</version>
  </dependency>
</surefirePluginDependencies>
```

All Surefire module dependencies must match NAR's pinned runner version.
Explicit providers bring their own transitive Platform dependencies; align the
project's engine, commons and launcher versions as well. The regression caught
an actual mismatch between an explicit provider's launcher 1.12.2 and project
engine 1.10.3. Ordinary automatic Jupiter selection handles its own resolution.

## Existing configuration

The generated goal reference describes the complete parameters. These behavior
boundaries are retained:

| Area | Behavior |
| --- | --- |
| Skips | `nar.skip`, `skipNar`, `skipNarTests`, `nar.test.skip.exec`, `maven.test.skip` and configured `dryRun` skip NAR as before. Global `skipTests` and `maven.test.skip.exec` alone do not skip this goal. |
| Includes | `**/Test*.java`, `**/*Test.java`, `**/*TestCase.java`; NAR does not add Surefire's `*Tests` default. |
| Excludes | `**/*$*` by default. |
| Selection | `test` overrides include/exclude and TestNG suite selection. Modern Surefire also supports method selectors such as `-Dtest=ExampleTest#method`. |
| No tests | `failIfNoTests` defaults to false, or true with an explicit `test`. Explicit false permits an unmatched selection. A missing test-classes directory remains successful unless `failIfNoTests=true`, even with `test`. |
| Reports | `${project.build.directory}/surefire-reports`, `trimStackTrace=true`, `printSummary=true`, `useFile=true`, `reportFormat=brief`. `disableXmlReport` and `nar.test.redirectTestOutputToFile` remain supported. Report formatting and XML schema follow modern Surefire. |
| Failures | Fail the build in `integration-test`; no deferred `verify` step. `nar.test.failure.ignore`/`testFailureIgnore` use Surefire's failure classification. No-tests failures and fork crashes remain failures even when test failures are ignored. Ordinary test assertion failures and execution errors can be ignored. |
| Classpaths | Configured classes/test directories and `additionalClasspathElements` are forwarded. Own JNI/shared projects also append their packaged artifact, respecting `build.directory` and `finalName`. |
| System properties | `systemProperties` is retained. New `systemPropertiesFile` and `systemPropertyVariables` follow Surefire precedence: legacy properties, file, then variables. NAR supplies `basedir`, `user.dir`, `localRepository` and reserved `nar.integrationTest.executionId`. |
| CLI properties | Promotion defaults to false. Opt in with `promoteUserPropertiesToSystemProperties` / `nar.test.promoteUserPropertiesToSystemProperties`. Non-forked tests naturally see properties already present in Maven's JVM. |
| JVM | `jvm`, `argLine`, `maven.surefire.debug`, `surefire.timeout`, `enableAssertions`, `childDelegation`, `surefire.useSystemClassLoader` and `surefire.useManifestOnlyJar` are retained. |
| Working directory/environment | `workingDirectory` defaults to the project directory. `environmentVariables` values are passed as values, preserving spaces. They apply to forked JVMs. |
| Framework settings | `groups`, `excludedGroups`, `parallel`, `threadCount`, `properties`, `suiteXmlFiles`, `junitArtifactName`, `testNGArtifactName` are forwarded. Their framework-specific interpretation is Surefire's. |
| Toolchains | An existing JDK toolchain is honored; `jdkToolchain` can select a child JDK directly. An explicit `jvm` takes precedence. A selected toolchain promotes a zero fork count (including `0C`) to a fork. |
| Modules | `useModulePath` defaults to false for compatibility; opt in with `nar.test.useModulePath`. |

NAR's resolved collection settings take precedence when passed to Surefire.
For example, `surefire.includes` and `surefire.excludes` cannot replace NAR's
include/exclude patterns, `maven.test.additionalClasspath` cannot replace its
additional classpath or appended packaged JAR, and `surefire.suiteXmlFiles`
cannot replace a suite list configured on NAR. Descriptor defaults still apply
to parameters NAR does not supply. The `test` selector keeps its documented
precedence over patterns and suites.

Modern Surefire may categorize provider/infrastructure errors differently from
the old 2.6 booter when failure-ignore is enabled. NAR does not parse reports or
catch every build failure to implement a second error policy. Use the default
`testFailureIgnore=false` for strict failure handling. Debug output, report
formatting and advanced selector syntax follow the released runner.

## Forks and native paths

| Java-only legacy `forkMode` | Modern equivalent |
| --- | --- |
| `never`, `none` | `forkCount=0` |
| `once` (default) | `forkCount=1`, `reuseForks=true` |
| `always`, `pertest` | `forkCount=1`, `reuseForks=false` |

New `forkCount` and `reuseForks` parameters (properties `nar.test.forkCount` and
`nar.test.reuseForks`) override legacy choices for Java-only projects. **NAR
packaging or any selected NAR dependency always forces one fork at a time and a
fresh JVM per test class**, including when modern settings request otherwise.

Extraction stays in the existing NAR dependency/unpack goals. The runner adds
existing own JNI/shared directories, followed by dependency shared/JNI
directories from the test unpack location. It uses dependency base versions,
including timestamped snapshots. Native directories precede an explicitly
configured loader-path tail, or the inherited tail when no override exists. If
the environment variable is absent, the legacy JVM-system-property fallback of
the same name is retained:

- Windows: `PATH`, with case-insensitive key matching; explicit `SystemRoot` is preserved.
- macOS: `DYLD_LIBRARY_PATH`.
- AIX: `LIBPATH`.
- Other platforms: `LD_LIBRARY_PATH`.

Loader environment variables also let the OS resolve libraries required by a
JNI library. Passing only `-Djava.library.path` would not cover those dependencies.
The runtime path fixture includes spaces and verifies both a project's own
library and a NAR dependency. An unrelated existing linker quoting defect
prevents compiling object paths with spaces; the fixture copies completed
libraries into such paths before running Java tests.

## Maintainer validation

`mvn -B -ntp clean verify` runs unit tests. `mvn -B -ntp clean verify -Prun-its`
also packages the plugin, installs that exact artifact into the fixture repository,
and runs the integration fixtures. Keep these phases in the same invocation.
The inherited profile already binds Invoker; do not append `invoker:run`.

Portable selection:

```sh
mvn -B -ntp clean verify -Prun-its \
  '-Dinvoker.test=it0042-jupiter-tempdir,it0043-surefire-contract,it0044-surefire-frameworks,it0046-surefire-toolchain'
```

The toolchain fixture needs a Java 8 entry with `<version>1.8</version>` in
`toolchains.xml` and is skipped when absent. Run it with Maven on a newer JDK
to verify cross-JDK execution. The native selection additionally needs
`it-parent`, `it0003-jni`, `it0007-lib-shared`, and `it0045-jni-isolation`.

Validate minimum Java 8/Maven 3.6.3, modern Maven/JDKs, and Windows/macOS native
lanes before changing the runner version. Record the exact Java/Maven versions,
fixture selections, results, and any unavailable platforms with each upgrade.

The adapter is coupled to Maven's `BuildPluginManager` and `MojoExecution`,
Surefire's goal parameters/default descriptor, and its duplicate-execution
checksum. It does not subclass `AbstractSurefireMojo` or use provider internals.
An upgrade must check framework selection, descriptor/default merging,
explicit-provider alignment, immediate errors, and multiple NAR executions.
The reserved execution property prevents Surefire from suppressing distinct NAR
executions that otherwise have identical configuration.
