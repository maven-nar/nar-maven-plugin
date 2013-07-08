The NAR plugin for Maven allows you to compile native code (C++, C and
Fortran) on a number of different architectures (Linux, Windows, MacOSX,
Solaris, ...) and with a number of different compilers/linkers (g++,
Microsoft Visual C++, CC, ...) The output produced is wrapped up in
Native ARchive files (.nar) some of which are machine independent
(-noarch), while others are machine specific and thus depend on a
combination of machine architecture(A), operating-system(O) and
linker(L) identified as AOL. These nar files can be installed in the
local Maven repository and deployed to a standard Maven (web) server,
using the standard `maven-install-plugin` and `maven-deploy-plugin`.

Links
-----
* [Documentation](https://maven-nar.github.com/nar-maven-plugin)
* [Mailing list](https://groups.google.com/group/maven-nar)
* [Issues](https://github.com/maven-nar/nar-maven-plugin/issues)
* [SCM](https://github.com/maven-nar)
* Maven Repository coming soon

FAQ
---
**Q:**
Is this repository (https://github.com/maven-nar/nar-maven-plugin) the official
home of the NAR plugin?

**A:**
Yes. [@duns](https://github.com/duns), the original author, [donated the
plugin](http://mail-archives.apache.org/mod_mbox/maven-users/201210.mbox/%3C67C29597-AFA3-4562-96B6-921481793AA5%40gmail.com%3E)
(with [Sonatype's
approval](http://mail-archives.apache.org/mod_mbox/maven-users/201210.mbox/%3C4DFF8AA4-A2DC-440B-9A59-0D94C18E73D4@tesla.io%3E))
to the community, since he no longer has time to maintain it.

**Q:**
Is the NAR project being actively maintained?

**A:**
Yes, it is now. It was rather dormant for the past few years, but now has at
least two active maintainers ([@ctrueden](https://github.com/ctrueden) and
[@dscho](https://github.com/dscho)), with [a few
others](https://github.com/maven-nar?tab=members) participating in the
development as well.

**Q:**
Why was the plugin renamed to `nar-maven-plugin`?

**A:**
To comply with the permitted usages of the Maven trademark. [According to the
Apache Project Management
Committee](http://markmail.org/search/?q=list%3Aorg.apache.maven.dev#query:list%3Aorg.apache.maven.dev+page:1+mid:cmqxvj6ddshmnzwr+state:results):
<blockquote>The pmc is permitting persons who develop plugins for maven to use
the mark maven in their plugin name provided the name and its usage meets
certain criteria, amongst which is the "\_\_\_-maven-plugin" naming
scheme.</blockquote>
The NAR plugin was previously named `maven-nar-plugin` because it was slated
for adoption as an official Maven plugin, but that never happened. So the
`artifactId` has been changed to `nar-maven-plugin`. (The `groupId` will need
to change as well, but that is still pending.)

**Q:**
Where is the official forum/list?

**A:**
The [maven-nar Google group](https://groups.google.com/group/maven-nar).

**Q:**
Where is the official issue tracker?

**A:**
[GitHub Issues](https://github.com/maven-nar/nar-maven-plugin/issues). There
was a [category for NAR in the Sonatype
JIRA](https://issues.sonatype.org/browse/NAR), which was active until May 2013,
but it has apparently been deleted, so we unfortunately cannot migrate those
issues to GitHub. If you had filed an issue there which is still relevant to
the latest `master` branch, please file a new GitHub issue for it. Thanks!

**Q:**
When do you expect the 3.0.0 release? Will it be deployed to Central?

**A:**
Yes, we plan to deploy to Central, but we have no set timeline for a release.
If it is something you need urgently, please write to the list about it. All of
the current NAR maintainers have day jobs, and so have limited time to spend on
NAR. So this project is largely driven by A) submitted pull requests; B) the
needs of the maintainers; and C) community complaints. ;-)

**Q:**
Is there a public Maven repository with release builds?

**A:**
Although we have not yet deployed a stable 3.0.0 release to Central, the
[SciJava project](https://scijava.org/) publishes *UNOFFICIAL* releases to its
Maven repository:

    <pluginRepository>
        <id>imagej.thirdparty</id>
        <url>http://maven.imagej.net/content/repositories/thirdparty</url>
    </pluginRepository>

As of this writing, the newest such release is:

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>nar-maven-plugin</artifactId>
        <version>3.0.0-scijava-2</version>
    </plugin>

The corresponding source is [tagged on
GitHub](https://github.com/scijava/maven-nar-plugin/tree/nar-maven-plugin-3.0.0-scijava-2).

**Q:**
Where can I find a working example of how to configure the NAR plugin?

**A:**
Several open source projects make use of the NAR plugin:

1. [DevZendo Cross-Platform FileSystem
   Access](https://code.google.com/p/devzendo-org-xpfsa/source/browse) uses
   NAR. The POM is straightforward and may be a good first project to study for
   learning about NAR.
2. [SLIM-curve](https://github.com/uw-loci/slim-curve) uses NAR with some extra
   configuration, and may be a good second project to study for seeing a few
   more parameters in action.
3. [ImageJ](http://developer.imagej.net/) has a [native launcher
   component](https://github.com/imagej/imagej-launcher) which uses NAR. Search
   the `pom.xml` for "nar" to find the relevant blocks. It is not a simple
   "hello world" example (e.g., it uses OS-specific profiles), but it is a
   working real-world use case which successfully builds on a [Jenkins server
   farm](http://jenkins.imagej.net/job/ImageJ-launcher/) including Windows, OS
   X and Linux nodes.

We would gladly welcome any other working examples of the NAR plugin in use!

**Q:**
How can I test the latest SNAPSHOTs?

**A:**
To use the latest code on the master branch and/or topic branches: clone the
code from GitHub, and build it using `mvn install`. This installs the JAR into
your local Maven repository cache (typically at
`~/.m2/repository/org/apache/maven/plugins/nar-maven-plugin/3.0.0-SNAPSHOT`).
Then add a reference to the plugin within your project's POM:

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>nar-maven-plugin</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </plugin>

It will resolve the plugin from your local cache.

Note that this approach is recommended only for testing, not for production
code, because anyone else building your code will then also need to build
`nar-maven-plugin` beforehand as well.
