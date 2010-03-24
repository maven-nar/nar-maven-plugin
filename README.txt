Modified version of ant contrib cpptasks, used in the freehep/maven-nar-plugin.

*** ALL Changes marked with FREEHEP or BEGINFREEHEP-ENDFREEHEP.

* cpptasks-1.0-beta-5-parallel-1-SNAPSHOT

- Replaced deprecated visual studio C++ compiler option /GZ with current /RTC1
- Distinguish between system and non-system headers for compilers that support it (g++)
- only recurse dependencies for direct includes of current source

- changed default subsystem back to gui
- added linkFortranMain option to link with gfortranbegin, see NAR-112
- added compileOrder to the compilerDef (and thus changed most Hashtables into Maps), see NAR-109
- added .f90 as extension for the fortran compiler, see NAR-108
- merged with cpptasks-1.0b5.tar.gz
- See NAR-103 for tests that have been changed
- [src/net/sf/antcontrib/cpptasks/compiler/CommandLineLinker] throw buildException when absolute paths are too long, Windows only.

* cpptasks-1.0-beta-4-parallel-5

- moved src to src/main/java, and some resource files to src/main/resources for easy merge with cpptasks-1.0b5
- [src/net/sf/antcontrib/cpptasks/compiler/CommandLineLinker] use absolute paths for all paths to overcome windows file length limit.

* cpptasks-1.0-beta-4-parallel-4

- Added fake <distributionManagement> section to pom to avoid MDEPLOY-50. One can now deploy
  this artifact using altDeploymentRepository

* cpptasks-1.0-beta-4-parallel-3

- [src/net/sf/antcontrib/cpptasks/intel/IntelLinuxFortranCompiler.java] added ifort.
- [src/net/sf/antcontrib/cpptasks/CompilerEnum.java] added ifort.

* cpptasks-1.0-beta-4-parallel-2

- [src/net/sf/antcontrib/cpptasks/gcc/GppLinker.java] g++ handles gfortran static linking.
- [src/net/sf/antcontrib/cpptasks/compiler/LinkType.java] g++ handles gfortran static linking.
- [src/net/sf/antcontrib/cpptasks/CCTask.java] g++ handles gfortran static linking.
- [src/net/sf/antcontrib/cpptasks/gcc/GppLinker.java] g++ puts runtime library at the end.

* cpptasks-1.0-beta-4-parallel-1

** Misc.
- [src/net/sf/antcontrib/cpptasks/SubsystemEnum.java] changed default subsystem from "gui" into "console"
- [src/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java] no more need for -prebind as of 10.4
- [src/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java] removed trailing space after "-framework" to avoid quoting
- [src/net/sf/antcontrib/cpptasks/gcc/GppLinker.java] added -shared-libgcc/-static-libgcc for linking gcc and c++. 
- [src/net/sf/antcontrib/cpptasks/gcc/GccLinker.java] added -dynamic as a valid option to GccLinker
- [src/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java] no -Bstatic for Darwin and no -Bdynamic for framework
- [src/net/sf/antcontrib/cpptasks/compaq/CompaqVisualFortranCompiler.java] removed addition of quotes.
- [src/net/sf/antcontrib/cpptasks/gcc/GccCompatibleCCompiler.java] added missing code for -fno-exceptions
- [src/net/sf/antcontrib/cpptasks/CCTask.java] added log statement to identify linker and compiler
- [src/net/sf/antcontrib/cpptasks/gcc/GppLinker.java] g++ linking now includes option -fexceptions
- [src/net/sf/antcontrib/cpptasks/compiler/CommandLineLinker] use absolute paths for filenames if they are shorter than relative paths to overcome windows file length limit.
- [src/net/sf/antcontrib/cpptasks/CCTask.java] added thread to keep progress

** Parallel running change
- [src/net/sf/antcontrib/cpptasks/CCTask.java] fork of a process per core/cpu available.
- [src/net/sf/antcontrib/cpptasks/TargetHistoryTable.java] protect against multi-threaded updates.
- [src/net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleCCompiler.java] replaced /Zi flag with /Z7 to disable writing debug database file (.pdb).
- [src/net/sf/antcontrib/cpptasks/devstudio/DevStudioCCompiler.java] limit command line length to 32000.
- [src/net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleLinker.java] limit command line length to 32000.
- [src/net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleLibrarian.java] limit command line length to 32000.
- [src/net/sf/antcontrib/cpptasks/devstudio/DevStudioMIDLCompiler.java] limit command line length to 32000.
- [src/net/sf/antcontrib/cpptasks/devstudio/DevStudioResourceCompiler.java] limit command line length to 32000.
- [src/net/sf/antcontrib/cpptasks/devstudio/VisualStudioNETProjectWriter.java] limit command line length to 32000.
- [src/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java] limit command line for Windows to 20000.
- [src/net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java] limit command line for Windows to 20000.


** dll for g++ on windows changes
- [src/net/sf/antcontrib/cpptasks/compiler/AbstractProcessor.java] Added isWindows()
- [src/net/sf/antcontrib/cpptasks/gcc/GccLinker.java] refactored dllLinker to soLinker
- [src/net/sf/antcontrib/cpptasks/gcc/GppLinker.java] refactored dllLinker to soLinker
- [src/net/sf/antcontrib/cpptasks/gcc/GccLinker.java] added dllLinker for windows
- [src/net/sf/antcontrib/cpptasks/gcc/GppLinker.java] added dllLinker for windows

** -fno-rtti changes
- [src/net/sf/antcontrib/cpptasks/gcc/GccCompatibleCCompiler.java] removed -fno-rtti flag
- [src/net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java] only add -fno-rtti for g++ and c++ 

** Launch process change
- [src/net/sf/antcontrib/cpptasks/compiler/CaptureStreamHandler.java] rewrote the launching of subprocesses (NARPLUGIN-71).
- [src/net/sf/antcontrib/cpptasks/compiler/CaptureStreamHandler.java] added protection against null return of run() method.

** libstdc++ linking
- [src/net/sf/antcontrib/cpptasks/CCTask.java] added method to link with CPP
- [src/net/sf/antcontrib/cpptasks/compiler/LinkType.java] added method to link with CPP
- [src/net/sf/antcontrib/cpptasks/compiler/gcc/GppLinker.java] link with or without CPP

** jni libraries (MacOS X)
- [src/net/sf/antcontrib/cpptasks/OutputTypeEnum.java] added jni
- [src/net/sf/antcontrib/cpptasks/compiler/LinkType] add jni as type
- [src/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker] added jni
- [src/net/sf/antcontrib/cpptasks/gcc/GccLinker.java] add jni as type and special linker for MacOS X to output jnilib files.
- [src/net/sf/antcontrib/cpptasks/gcc/GppLinker.java] add jni as type and special linker for MacOS X to output jnilib files.

** gfortran compiler
- [src/net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java] added gfortran compiler
- [src/net/sf/antcontrib/cpptasks/CompilerEnum.java] added gfortran compiler

** intel compilers name change
- [src/net/sf/antcontrib/cpptasks/intel/IntelLinux32CLinker.java] added (icpc)
- [src/net/sf/antcontrib/cpptasks/intel/IntelLinux32Compiler.java] added (icpc)
- [src/net/sf/antcontrib/cpptasks/intel/IntelLinux64CLinker.java] added (ecpc)
- [src/net/sf/antcontrib/cpptasks/intel/IntelLinux64Compiler.java] added (ecpc)
- [src/net/sf/antcontrib/cpptasks/LinkerEnum.java] added and changed linkers 
- [src/net/sf/antcontrib/cpptasks/CompilerEnum.java] added and changed compilers.
- [src/net/sf/antcontrib/cpptasks/intel/IntelLinux32CCompiler.java] links to IntelLinux32CLinker.
- [src/net/sf/antcontrib/cpptasks/intel/IntelLinux64CCompiler.java] links to  IntelLinux64CLinker.
- [src/net/sf/antcontrib/cpptasks/intel/IntelLinux32Linker.java] changed linker name from icc to icpc for version 8.1 of Intel Compilers.
- [src/net/sf/antcontrib/cpptasks/intel/IntelLinux64Linker.java] changed linker name from ecc to ecpc for version 8.1 of Intel Compilers.

** sun c and fortran compiles
- [src/net/sf/antcontrib/cpptasks/sun/ForteCCompiler.java] added (suncc)
- [src/net/sf/antcontrib/cpptasks/sun/ForteF77Compiler.java] added (sunf77)
- [src/net/sf/antcontrib/cpptasks/sun/ForteCCLinker.java] changed -static into -staticlib=%all
- [src/net/sf/antcontrib/cpptasks/sun/ForteCCLinker.java] if last was -Bstatic reset it to -Bdynamic so that libc and libm can be found as shareables
- [src/net/sf/antcontrib/cpptasks/CompilerEnum] Added the above (suncc, sunf77).

** bug [ 1109917 ] g++ linker does not add runtime w/o other libs referenced
- [src/net/sf/antcontrib/cpptasks/compiler/CommandLineLinker] always call addLibrarySets

** bug [ 795683 ] cpptasks speedup
- [src/net/sf/antcontrib/cpptasks/DependencyTable.java] cpptasks speedup
- [src/net/sf/antcontrib/cpptasks/DependencyInfo.java] cpptasks speedup


