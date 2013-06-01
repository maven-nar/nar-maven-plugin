The maven-nar-plugin allows you to compile native code (C++, C and
Fortran) on a number of different architectures (Linux, Windows, MacOSX,
Solaris, ...) and with a number of different compilers/linkers (g++,
Microsoft Visual C++, CC, ...) The output produced is wrapped up in
Native ARchive files (.nar) some of which are machine independent
(-noarch), while others are machine specific and thus depend on a
combination of machine architecture(A), operating-system(O) and
linker(L) identified as AOL. These nar files can be installed in the
local maven repository and deployed to a standard maven (web) server,
using the standard maven-install-plugin and maven-deploy-plugin.

Links
-----
* [Documentation](https://maven-nar.github.com/maven-nar-plugin)
* [Mailing list](https://groups.google.com/forum/?fromgroups#!forum/maven-nar)
* [Issues](https://github.com/maven-nar/maven-nar-plugin/issues)
* [SCM](https://github.com/maven-nar)
* Maven Repository coming soon

Old
---
* https://github.com/duns/maven-nar-plugin
* http://duns.github.com/maven-snapshots
* http://java.freehep.org/freehep-nar-plugin/intro.html
* https://github.com/duns/freehep-nar-plugin
