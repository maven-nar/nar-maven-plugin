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

Links
-----
* [Documentation](http://maven-nar.github.io/nar-maven-plugin/)
* [Mailing list](https://groups.google.com/group/maven-nar)
* [Issues](https://github.com/maven-nar/nar-maven-plugin/issues)
* [SCM](https://github.com/maven-nar)

What about maven-nar-plugin?
----------------------------
This *is* the [official](https://github.com/maven-nar/nar-maven-plugin/wiki/Frequently_Asked_Questions#q-is-this-repository-httpsgithubcommaven-narnar-maven-plugin-the-official-home-of-the-nar-plugin) `maven-nar-plugin` project, renamed to `nar-maven-plugin` [as per Apache Maven's requirements](https://github.com/maven-nar/nar-maven-plugin/wiki/Frequently_Asked_Questions#q-why-was-the-plugin-renamed-to-nar-maven-plugin).

Is this plugin available on Maven Central?
------------------------------------------

[Yes](http://search.maven.org/#search|ga|1|g%3A%22com.github.maven-nar%22%20a%3A%22nar-maven-plugin%22).

Is there a FAQ?
---------------

[Yes](https://github.com/maven-nar/nar-maven-plugin/wiki/Frequently_Asked_Questions).
