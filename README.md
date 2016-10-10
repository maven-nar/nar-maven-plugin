The NAR plugin for Maven allows you to compile native code (C++, C and
Fortran) on a number of different architectures (Linux, Windows, MacOSX,
Solaris, FreeBSD, ...) and with a number of different compilers/linkers
(g++, Microsoft Visual C++, CC, ...) The output produced is wrapped up
in Native ARchive files (.nar) some of which are machine independent
(-noarch), while others are machine specific and thus depend on a
combination of machine architecture(A), operating-system(O) and
linker(L) identified as AOL. These nar files can be installed in the
local Maven repository and deployed to a standard Maven (web) server,
using the standard `maven-install-plugin` and `maven-deploy-plugin`.

Usage
-----

In your POM:

```xml
<build>
	<plugins>
		<plugin>
			<groupId>com.github.maven-nar</groupId>
			<artifactId>nar-maven-plugin</artifactId>
			<version>3.5.1</version>
			<extensions>true</extensions>
			<configuration>
				...
			</configuration>
		</plugin>
	</plugins>
</build>
```

Of course, it is recommended that you use the
[latest version](http://search.maven.org/#search|ga|1|g%3A%22com.github.maven-nar%22%20a%3A%22nar-maven-plugin%22).

What you put in the `<configuration>` section will depend on your build;
for ideas, see:
* [Working examples](https://github.com/maven-nar/nar-maven-plugin/wiki/Working-examples)
* [integration tests](https://github.com/maven-nar/nar-maven-plugin/tree/master/src/it)

Documentation
-------------
* [Wiki](https://github.com/maven-nar/nar-maven-plugin/wiki)
    * [How to contribute](https://github.com/maven-nar/nar-maven-plugin/wiki/How-to-contribute)
    * [FAQ](https://github.com/maven-nar/nar-maven-plugin/wiki/Frequently_Asked_Questions)
* [Maven site](http://maven-nar.github.io/)

Community
---------
* [Mailing list](https://groups.google.com/group/maven-nar)
* [Issues](https://github.com/maven-nar/nar-maven-plugin/issues)

What about maven-nar-plugin?
----------------------------
This *is* the [official](https://github.com/maven-nar/nar-maven-plugin/wiki/Frequently_Asked_Questions#q-is-this-repository-httpsgithubcommaven-narnar-maven-plugin-the-official-home-of-the-nar-plugin) `maven-nar-plugin` project, renamed to `nar-maven-plugin` [as per Apache Maven's requirements](https://github.com/maven-nar/nar-maven-plugin/wiki/Frequently_Asked_Questions#q-why-was-the-plugin-renamed-to-nar-maven-plugin).

Alternatives and Complements
----------------------------
* [Native Library Loader](https://github.com/scijava/native-lib-loader) which
  [integrates with NAR](https://github.com/maven-nar/nar-maven-plugin/wiki/Native-Library-Loader)
* [Native Maven Plugin](https://github.com/mojohaus/maven-native) from Mojohaus
* [Java Native Loader](https://github.com/uw-dims/java-native-loader)
* [Gradle Native](http://gradle.org/getting-started-native/)
