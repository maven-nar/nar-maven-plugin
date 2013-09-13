Modified version of ant contrib cpptasks, used by
[nar-maven-plugin](https://github.com/maven-nar/maven-nar-plugin).

## Changes

***ALL Changes marked with FREEHEP or BEGINFREEHEP-ENDFREEHEP.***

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
  - [[net/sf/antcontrib/cpptasks/compiler/CommandLineLinker.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/CommandLineLinker.java)] throw buildException when absolute paths are too long, Windows only

* cpptasks-1.0-beta-4-parallel-5
  - moved src to src/main/java, and some resource files to src/main/resources for easy merge with cpptasks-1.0b5
  - [[net/sf/antcontrib/cpptasks/compiler/CommandLineLinker.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/CommandLineLinker.java)] use absolute paths for all paths to overcome windows file length limit

* cpptasks-1.0-beta-4-parallel-4
  - Added fake <distributionManagement> section to pom to avoid [MDEPLOY-50](http://jira.codehaus.org/browse/MDEPLOY-50). One can now deploy this artifact using altDeploymentRepository

* cpptasks-1.0-beta-4-parallel-3
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinuxFortranCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinuxFortranCompiler.java)] added ifort
  - [[net/sf/antcontrib/cpptasks/CompilerEnum.java](src/main/java/net/sf/antcontrib/cpptasks/CompilerEnum.java)] added ifort

* cpptasks-1.0-beta-4-parallel-2
  - [[net/sf/antcontrib/cpptasks/gcc/GppLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GppLinker.java)] g++ handles gfortran static linking
  - [[net/sf/antcontrib/cpptasks/compiler/LinkType.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/LinkType.java)] g++ handles gfortran static linking
  - [[net/sf/antcontrib/cpptasks/CCTask.java](src/main/java/net/sf/antcontrib/cpptasks/CCTask.java)] g++ handles gfortran static linking
  - [[net/sf/antcontrib/cpptasks/gcc/GppLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GppLinker.java)] g++ puts runtime library at the end

* cpptasks-1.0-beta-4-parallel-1

* Misc.
  - [[net/sf/antcontrib/cpptasks/SubsystemEnum.java](src/main/java/net/sf/antcontrib/cpptasks/SubsystemEnum.java)] changed default subsystem from "gui" into "console"
  - [[net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java)] no more need for -prebind as of 10.4
  - [[net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java)] removed trailing space after "-framework" to avoid quoting
  - [[net/sf/antcontrib/cpptasks/gcc/GppLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GppLinker.java)] added -shared-libgcc/-static-libgcc for linking gcc and c++
  - [[net/sf/antcontrib/cpptasks/gcc/GccLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccLinker.java)] added -dynamic as a valid option to GccLinker
  - [[net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java)] no -Bstatic for Darwin and no -Bdynamic for framework
  - [[net/sf/antcontrib/cpptasks/compaq/CompaqVisualFortranCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/compaq/CompaqVisualFortranCompiler.java)] removed addition of quotes
  - [[net/sf/antcontrib/cpptasks/gcc/GccCompatibleCCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccCompatibleCCompiler.java)] added missing code for -fno-exceptions
  - [[net/sf/antcontrib/cpptasks/CCTask.java](src/main/java/net/sf/antcontrib/cpptasks/CCTask.java)] added log statement to identify linker and compiler
  - [[net/sf/antcontrib/cpptasks/gcc/GppLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GppLinker.java)] g++ linking now includes option -fexceptions
  - [[net/sf/antcontrib/cpptasks/compiler/CommandLineLinker.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/CommandLineLinker.java)] use absolute paths for filenames if they are shorter than relative paths to overcome windows file length limit
  - [[net/sf/antcontrib/cpptasks/CCTask.java](src/main/java/net/sf/antcontrib/cpptasks/CCTask.java)] added thread to keep progress

* Parallel running change
  - [[net/sf/antcontrib/cpptasks/CCTask.java](src/main/java/net/sf/antcontrib/cpptasks/CCTask.java)] fork of a process per core/cpu available
  - [[net/sf/antcontrib/cpptasks/TargetHistoryTable.java](src/main/java/net/sf/antcontrib/cpptasks/TargetHistoryTable.java)] protect against multi-threaded updates
  - [[net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleCCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleCCompiler.java)] replaced /Zi flag with /Z7 to disable writing debug database file (.pdb)
  - [[net/sf/antcontrib/cpptasks/devstudio/DevStudioCCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/devstudio/DevStudioCCompiler.java)] limit command line length to 32000
  - [[net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleLinker.java](src/main/java/net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleLinker.java)] limit command line length to 32000
  - [[net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleLibrarian.java](src/main/java/net/sf/antcontrib/cpptasks/devstudio/DevStudioCompatibleLibrarian.java)] limit command line length to 32000
  - [[net/sf/antcontrib/cpptasks/devstudio/DevStudioMIDLCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/devstudio/DevStudioMIDLCompiler.java)] limit command line length to 32000
  - [[net/sf/antcontrib/cpptasks/devstudio/DevStudioResourceCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/devstudio/DevStudioResourceCompiler.java)] limit command line length to 32000
  - [[net/sf/antcontrib/cpptasks/devstudio/VisualStudioNETProjectWriter.java](src/main/java/net/sf/antcontrib/cpptasks/devstudio/VisualStudioNETProjectWriter.java)] limit command line length to 32000
  - [[net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java)] limit command line for Windows to 20000
  - [[net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java)] limit command line for Windows to 20000

* dll for g++ on windows changes
  - [[net/sf/antcontrib/cpptasks/compiler/AbstractProcessor.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/AbstractProcessor.java)] Added isWindows()
  - [[net/sf/antcontrib/cpptasks/gcc/GccLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccLinker.java)] refactored dllLinker to soLinker
  - [[net/sf/antcontrib/cpptasks/gcc/GppLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GppLinker.java)] refactored dllLinker to soLinker
  - [[net/sf/antcontrib/cpptasks/gcc/GccLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccLinker.java)] added dllLinker for windows
  - [[net/sf/antcontrib/cpptasks/gcc/GppLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GppLinker.java)] added dllLinker for windows

* -fno-rtti changes
  - [[net/sf/antcontrib/cpptasks/gcc/GccCompatibleCCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccCompatibleCCompiler.java)] removed -fno-rtti flag
  - [[net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java)] only add -fno-rtti for g++ and c++

* Launch process change
  - [[net/sf/antcontrib/cpptasks/compiler/CaptureStreamHandler.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/CaptureStreamHandler.java)] rewrote the launching of subprocesses (NARPLUGIN-71)
  - [[net/sf/antcontrib/cpptasks/compiler/CaptureStreamHandler.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/CaptureStreamHandler.java)] added protection against null return of run() method

* libstdc++ linking
  - [[net/sf/antcontrib/cpptasks/CCTask.java](src/main/java/net/sf/antcontrib/cpptasks/CCTask.java)] added method to link with CPP
  - [[net/sf/antcontrib/cpptasks/compiler/LinkType.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/LinkType.java)] added method to link with CPP
  - [[net/sf/antcontrib/cpptasks/compiler/gcc/GppLinker.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/gcc/GppLinker.java)] link with or without CPP

* jni libraries (MacOS X)
  - [[net/sf/antcontrib/cpptasks/OutputTypeEnum.java](src/main/java/net/sf/antcontrib/cpptasks/OutputTypeEnum.java)] added jni
  - [[net/sf/antcontrib/cpptasks/compiler/LinkType.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/LinkType.java)] add jni as type
  - [[net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/AbstractLdLinker.java)] added jni
  - [[net/sf/antcontrib/cpptasks/gcc/GccLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccLinker.java)] add jni as type and special linker for MacOS X to output jnilib files
  - [[net/sf/antcontrib/cpptasks/gcc/GppLinker.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GppLinker.java)] add jni as type and special linker for MacOS X to output jnilib files

* gfortran compiler
  - [[net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/gcc/GccCCompiler.java)] added gfortran compiler
  - [[net/sf/antcontrib/cpptasks/CompilerEnum.java](src/main/java/net/sf/antcontrib/cpptasks/CompilerEnum.java)] added gfortran compiler

* intel compilers name change
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinux32CLinker.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinux32CLinker.java)] added (icpc)
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinux32Compiler.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinux32Compiler.java)] added (icpc)
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinux64CLinker.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinux64CLinker.java)] added (ecpc)
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinux64Compiler.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinux64Compiler.java)] added (ecpc)
  - [[net/sf/antcontrib/cpptasks/LinkerEnum.java](src/main/java/net/sf/antcontrib/cpptasks/LinkerEnum.java)] added and changed linkers
  - [[net/sf/antcontrib/cpptasks/CompilerEnum.java](src/main/java/net/sf/antcontrib/cpptasks/CompilerEnum.java)] added and changed compilers
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinux32CCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinux32CCompiler.java)] links to IntelLinux32CLinker
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinux64CCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinux64CCompiler.java)] links to  IntelLinux64CLinker
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinux32Linker.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinux32Linker.java)] changed linker name from icc to icpc for version 8.1 of Intel Compilers
  - [[net/sf/antcontrib/cpptasks/intel/IntelLinux64Linker.java](src/main/java/net/sf/antcontrib/cpptasks/intel/IntelLinux64Linker.java)] changed linker name from ecc to ecpc for version 8.1 of Intel Compilers

* sun c and fortran compiles
  - [[net/sf/antcontrib/cpptasks/sun/ForteCCompiler.java](src/main/java/net/sf/antcontrib/cpptasks/sun/ForteCCompiler.java)] added (suncc)
  - [[net/sf/antcontrib/cpptasks/sun/ForteF77Compiler.java](src/main/java/net/sf/antcontrib/cpptasks/sun/ForteF77Compiler.java)] added (sunf77)
  - [[net/sf/antcontrib/cpptasks/sun/ForteCCLinker.java](src/main/java/net/sf/antcontrib/cpptasks/sun/ForteCCLinker.java)] changed -static into -staticlib=%all
  - [[net/sf/antcontrib/cpptasks/sun/ForteCCLinker.java](src/main/java/net/sf/antcontrib/cpptasks/sun/ForteCCLinker.java)] if last was -Bstatic reset it to -Bdynamic so that libc and libm can be found as shareables
  - [[net/sf/antcontrib/cpptasks/CompilerEnum.java](src/main/java/net/sf/antcontrib/cpptasks/CompilerEnum.java)] Added the above (suncc, sunf77)

* bug [ 1109917 ] g++ linker does not add runtime w/o other libs referenced
  - [[net/sf/antcontrib/cpptasks/compiler/CommandLineLinker.java](src/main/java/net/sf/antcontrib/cpptasks/compiler/CommandLineLinker.java)] always call addLibrarySets

* bug [ 795683 ] cpptasks speedup
  - [[net/sf/antcontrib/cpptasks/DependencyTable.java](src/main/java/net/sf/antcontrib/cpptasks/DependencyTable.java)] cpptasks speedup
  - [[net/sf/antcontrib/cpptasks/DependencyInfo.java](src/main/java/net/sf/antcontrib/cpptasks/DependencyInfo.java)] cpptasks speedup
