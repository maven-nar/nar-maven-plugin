# Maven compatibility

NAR retains a minimum of **Maven 3.6.3 and Java 8**. The same plugin JAR also
runs on Maven 4 through its support for Maven 3 plugins. Maven 4 itself requires
Java 17 or newer; that does not change NAR's Java 8 bytecode target.
See Maven's [migration guide](https://maven.apache.org/guides/mini/guide-migration-to-mvn4).

## Repository resolution

NAR uses the `RepositorySystem` and `RepositorySystemSession` supplied by the
running Maven process. It compiles against the Maven 3.6.3 and Resolver 1.4.1
APIs, with Maven Core and Resolver dependencies in `provided` scope. It does
not bundle another Resolver implementation or require `maven-compat`.

Attachment resolution uses the project's remote repositories and the host
session's workspace reader, offline mode, update policies, mirrors,
authentication and proxies. NAR reads metadata and attachment contents from the
actual resolved files. It never derives a cache path from artifact coordinates
or from a parent archive's directory. This also supports Resolver's
[split local repository](https://maven.apache.org/resolver/local-repository.html).
The `localRepository` system property supplied to Java integration tests remains
the root directory of the host's local repository.

Timestamped snapshot archives retain their logical `-SNAPSHOT` version in NAR's
extraction directory names. Native include and library paths therefore keep the
same names for remote and locally installed snapshots. Assembly uses the same
resolved attachment objects as downloading and unpacking. Missing attachments
still fail the build with their coordinates and the underlying Resolver error.

Existing Maven 3 plugin annotations, Plexus component declarations and `nar`
lifecycle mappings are retained. Java integration tests still use Surefire
3.5.6, as described in [Java/JNI integration tests](java-integration-tests.md).

## Reproducible builds

Pin lifecycle plugin versions in the consuming project's `pluginManagement`.
This matters for custom packaging: an unversioned binding can select a plugin
whose Maven or Java requirements differ from the project's requirements.
The integration-test parent uses the same compatible versions as NAR's build:
Resources 3.5.0, Compiler 3.16.0, Surefire 3.5.6, JAR 3.5.1, Clean 3.5.0 and
Install 3.2.0. An initial Maven 4 run selected Resources 4.0.0-beta-1 and failed
inside that plugin with an incompatible Maven 4 API. Two incorrect relative
parent paths in the GNU fixture were also corrected for Maven 4 model validation.

## Validation

Run the complete configured suite using:

```sh
mvn -B clean verify -Prun-its
```

`it0047-resolver-compatibility` uses controlled file repositories and isolated
caches. It checks releases, timestamped and locally installed snapshots,
`-U`, offline resolution, missing artifacts, native/noarch classifiers,
extraction and assembly, and split repositories. Split flags take effect on
Maven 3.9.16 and Maven 4.0.0-rc-6; Maven 3.6.3 ignores them. The fixture checks that each
nested build uses the exact plugin JAR installed by Invoker, so another cached
snapshot cannot silently replace the build under test.

The unit tests also cover metadata from reactor class directories and archives
outside a normal repository layout, version ranges, use of the host session,
and extraction names independent of the resolved archive's filename.

To reuse a Java 8-built plugin on another Maven/JDK pair, retain the plugin
installed in `target/it-repo` by the first build. Change `JAVA_HOME` and the
Maven executable, then run only Invoker:

```sh
mvn -B -Prun-its invoker:run invoker:verify
```

This does not recompile or reinstall the plugin. Compare the SHA-256 of the
plugin JAR in `target/it-repo` with the original build. Independent workspaces
can copy that exact artifact and its POM into the same repository coordinates.

### Results (2026-09-27)

| Maven | Build JVM | Unit tests | Integration fixtures |
| --- | --- | --- | --- |
| 3.6.3 | Temurin 8u462 | 491 passed | All 53 configured fixtures passed |
| 3.9.16 | Temurin 21.0.11 | 491 passed | All 53 configured fixtures passed, including the targeted rerun described below |
| 4.0.0-rc-6 | Temurin 21.0.11 | 491 passed | All 53 configured fixtures passed |
| 4.0.0-rc-6 | Temurin 17.0.16 | Not repeated | 11 selected fixtures passed: setup, JNI, reactor dependencies, Jupiter, Surefire configuration/frameworks, native isolation, toolchains and repositories |

Full suites were run on the first three pairs. The final Maven 3.9 run caught
a fixture timing issue: two generated ZIPs could have equal entry timestamps,
so assembly's existing incremental-copy policy retained the previous scenario's
marker. The repository fixture now clears extraction and assembly outputs
between scenarios while retaining the cache. That fixture passed a targeted
rerun on all four pairs after the correction; unaffected tests were not repeated.

All final integration runs used the identical Java 8-built plugin JAR, with
SHA-256 `44ce2b5872ba538b2540e3d75532bc846831d916a649e7efb162e8e4e554cbc0`.
Its classes use major version 52 and its descriptor declares Java 8 and Maven
3.6.3. The new Java files pass the saved Eclipse-derived Spotless check.
No Enforcer bypass was used; dependency bytecode and minimum Maven/Java checks
remained enabled.

Native validation used Linux x86_64 with GCC/GFortran 13.3.0. Existing exclusions
for `it0021` and `it0022` remain. Windows, macOS and AIX native builds were not run.
