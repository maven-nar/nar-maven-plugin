This plugin is an attempt to 
- merge/cherrypick various changes in forks from the github.com/duns/maven-nar-plugin
- introduce matrix style build - architecture x types x liblinkage
- try to introduce separation between artifact handling and building.
- address some of the Issues listed.

peterjanes/maven-nar-plugin  master
still to work through these changes.
-

1spatial/maven-nar-plugin  master
- NAR-189 dependency resolution (applied)
  merged from mephi42
  
- Fix off by one (applied)

- Visual Studio setup mojo - vs2012 (considering)
  Seems like good idea, have some ideas on variations to templates 


BitWig/maven-nar-plugin  master
- Working dir  for test execution (change considered and revised)
  not good using existing and modified hard coding either just $(basedir) ie. project/  or  $(testTargetDir)/test-reports ie. project/nar-test/testname/test-reports
  make test workspace dir a configurable option and default back to the longer form.

- Singleton objDir  (change considered and revised)
  multi AOL build requires more than one objDir, make target intermediate dir a configurable option under which AOL subfolders are created

- UnpackProcess  removed processing of MacOSX files after unpack  (change documented, but not used)
  Agree that current process doesn't seem entirely appropriate, not entirely sure if some other enhancment needs to be made elsewhere in exchange though.
  
- Precompiler   (change considered and revised)
  config resolution in Nar native compile, 
  config moved from narcompile to library to support variation for test compiles.


scijava/maven-nar-plugin master
- Fix off by one (applied)

- Add Wagon for webdav deployment (not used)
  perhaps this should come from an alternate parent if necessary for others to build rather than claiming org.apache.maven.plugins
  there are various other settings in this pom overiding org.apache.maven.plugins perhaps innaparopriately.


mephi42/maven-nar-plugin master
- NAR-189 dependency resolution (applied)
 
- introduce abstractRunMojo extracted from narTestMojo  and new mojo narRunMojo  ()
  main feature add seems to be system stream interaction for input, and alternate method of settings args (rather than on the library/test)

- valgrind option on executions  (considering)
  seems reasonable, complicated due to narRunMojo change.

rockdreamer/maven-nar-plugin master
- AIX aol.properties (applied)

emetsger/maven-nar-plugin master
- Fix off by one (applied)

- Create only identified/configure Compilers (considered, revised)
  compilation is running kind of promiscuously, however that meets more the convention over configuration
  add revised - new flag to run only configured compilers
  
mirkojahn/maven-nar-plugin master
- AIX aol.properties (applied)
- Local & Remote repositories (considered, revised)

sugree/maven-nar-plugin master
- wingw (g++)  aol.properties (applied)


** deploy change only - not merged
emenifee/maven-nar-plugin master
- derived from duns
vince75/maven-nar-plugin master
- derived from mephi42



Documentation:

- http://duns.github.com/maven-nar-plugin
- https://docs.sonatype.org/display/NAR/Index (empty still)

Issues:

- https://issues.sonatype.org/browse/NAR

SCM:

- http://github.com/duns/maven-nar-plugin
- http://github.com/duns/cpptasks-parallel

Repository:

- http://duns.github.com/maven-snapshots (until it moves to maven central)

Old:

- http://java.freehep.org/freehep-nar-plugin/intro.html
- http://github.com/duns/freehep-nar-plugin


