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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.github.maven_nar.cpptasks.*;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

/**
 * Compiles native source files.
 * 
 * @requiresSession
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-compile", defaultPhase = LifecyclePhase.COMPILE, requiresProject = true,
  requiresDependencyResolution = ResolutionScope.COMPILE)
public class NarCompileMojo extends AbstractCompileMojo {
  /**
   * Specify that the final manifest should be embedded in the output (default
   * true) or false for side by side.
   */
  @Parameter(property = "nar.embedManifest", defaultValue = "true")
  protected boolean embedManifest = true;

  /**
   * The current build session instance.
   */
  @Component
  protected MavenSession session;

  private void copyInclude(final Compiler c) throws IOException, MojoExecutionException, MojoFailureException {
    if (c == null) {
      return;
    }
    c.copyIncludeFiles(
        getMavenProject(),
        getLayout().getIncludeDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
            getMavenProject().getVersion()));
  }

  private void createLibrary(final Project antProject, final Library library)
      throws MojoExecutionException, MojoFailureException {
    getLog().debug("Creating Library " + library);
    // configure task
    final CCTask task = new CCTask();
    task.setCommandLogLevel(this.commandLogLevel);
    task.setProject(antProject);

    task.setDecorateLinkerOptions(this.decorateLinkerOptions);

    // subsystem
    final SubsystemEnum subSystem = new SubsystemEnum();
    subSystem.setValue(library.getSubSystem());
    task.setSubsystem(subSystem);

    // set max cores
    task.setMaxCores(getMaxCores(getAOL()));

    // outtype
    final OutputTypeEnum outTypeEnum = new OutputTypeEnum();
    final String type = library.getType();
    outTypeEnum.setValue(type);
    task.setOuttype(outTypeEnum);

    // stdc++
    task.setLinkCPP(library.linkCPP());

    // fortran
    task.setLinkFortran(library.linkFortran());
    task.setLinkFortranMain(library.linkFortranMain());

    // outDir
    File outDir;
    if (type.equals(Library.EXECUTABLE)) {
      outDir = getLayout().getBinDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
          getMavenProject().getVersion(), getAOL().toString());
    } else {
      outDir = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
          getMavenProject().getVersion(), getAOL().toString(), type);
    }
    outDir.mkdirs();

    // outFile
    // FIXME NAR-90 we could get the final name from layout
    final File outFile = new File(outDir, getOutput(getAOL(), type));
    getLog().debug("NAR - output: '" + outFile + "'");
    task.setOutfile(outFile);

    // object directory
    File objDir = new File(getTargetDirectory(), "obj");
    objDir = new File(objDir, getAOL().toString());
    objDir.mkdirs();
    task.setObjdir(objDir);

    // failOnError, libtool
    task.setFailonerror(failOnError(getAOL()));
    task.setLibtool(useLibtool(getAOL()));

    // runtime
    final RuntimeType runtimeType = new RuntimeType();
    runtimeType.setValue(getRuntime(getAOL()));
    task.setRuntime(runtimeType);

    // IDL, MC, RC compilations should probably be 'generate source' type
    // actions, seperate from main build.
    // Needs resolution of handling for generate sources.
    // Order is somewhat important here, IDL and MC generate outputs that are
    // (often) included in the RC compilation
    if (getIdl() != null) {
      final CompilerDef idl = getIdl().getCompiler(Compiler.MAIN, null);
      if (idl != null) {
        task.addConfiguredCompiler(idl);
        task.createIncludePath().setPath(objDir.getPath()); // generated
                                                            // 'sources'
      }
    }
    if (getMessage() != null) {
      final CompilerDef mc = getMessage().getCompiler(Compiler.MAIN, null);
      if (mc != null) {
        task.addConfiguredCompiler(mc);
        task.createIncludePath().setPath(objDir.getPath()); // generated
                                                            // 'sources'
      }
    }
    if (getResource() != null) {
      final CompilerDef res = getResource().getCompiler(Compiler.MAIN, null);
      if (res != null) {
        task.addConfiguredCompiler(res);
      }
    }

    // Darren Sargent Feb 11 2010: Use Compiler.MAIN for "type"...appears the
    // wrong "type" variable was being used
    // since getCompiler() expects "main" or "test", whereas the "type" variable
    // here is "executable", "shared" etc.
    // add C++ compiler
    if (getCpp() != null) {
      final CompilerDef cpp = getCpp().getCompiler(Compiler.MAIN, null);
      if (cpp != null) {
        // Set FortifyID attribute
        cpp.setFortifyID(getfortifyID());
        task.addConfiguredCompiler(cpp);
      }
    }

    // add C compiler
    if (getC() != null) {
      final CompilerDef c = getC().getCompiler(Compiler.MAIN, null);
      if (c != null) {
        // Set FortifyID attribute
        c.setFortifyID(getfortifyID());  
        task.addConfiguredCompiler(c);
      }
    }

    // add Fortran compiler
    if (getFortran() != null) {
      final CompilerDef fortran = getFortran().getCompiler(Compiler.MAIN, null);
      if (fortran != null) {
        task.addConfiguredCompiler(fortran);
      }
    }

      // Add VersionInfo for the Windows binaries
      if(getOS().equals( OS.WINDOWS ) && getLinker().getName( null, null ).equals( "msvc" ))
      {
         NARVersionInfo narVersioninfo = getNARVersionInfo() ;
          if(narVersioninfo !=  null)
          {

              VersionInfo versionInfo=narVersioninfo.getVersionInfo(getAntProject());

              if(versionInfo != null)
              {
                  task.addConfiguredVersioninfo(versionInfo);
                 
              }
          }
      }

    // end Darren

    // add javah include path
    final File jniDirectory = getJavah().getJniDirectory();
    if (jniDirectory.exists()) {
      task.createIncludePath().setPath(jniDirectory.getPath());
    }

    // add java include paths
    getJava().addIncludePaths(task, type);

    getMsvc().configureCCTask(task);

    final List<NarArtifact> dependencies = getNarArtifacts();
    // add dependency include paths
    for (final Object element : dependencies) {
      // FIXME, handle multiple includes from one NAR
      final NarArtifact narDependency = (NarArtifact) element;
      final String binding = getBinding(library, narDependency);
      getLog().debug("Looking for " + narDependency + " found binding " + binding);
      if (!binding.equals(Library.JNI)) {
        final File unpackDirectory = getUnpackDirectory();
        final File include = getLayout().getIncludeDirectory(unpackDirectory, narDependency.getArtifactId(),
            narDependency.getBaseVersion());
        getLog().debug("Looking for include directory: " + include);
        if (include.exists()) {
          task.createIncludePath().setPath(include.getPath());
        } else {
          // Ideally includes are used from lib (static or shared)
          // however it's not required.
          // make a note in the log if something has gone wrong,
          // but don't block compilation
          getLog().warn(String.format("Unable to locate %1$s lib include path '%2$s'", binding, include));
        }
      }
    }

    // add linker
    final LinkerDef linkerDefinition = getLinker().getLinker(this, task, getOS(), getAOL().getKey() + ".linker.", type);
    task.addConfiguredLinker(linkerDefinition);

    // add dependency libraries
    // FIXME: what about PLUGIN and STATIC, depending on STATIC, should we
    // not add all libraries, see NARPLUGIN-96
    final boolean skipDepLink = linkerDefinition.isSkipDepLink();
    if (((type.equals(Library.SHARED) || type.equals(Library.JNI) || type.equals(Library.EXECUTABLE))) && !skipDepLink) {

      final List depLibOrder = getDependencyLibOrder();
      List depLibs = dependencies;

      // reorder the libraries that come from the nar dependencies
      // to comply with the order specified by the user
      if (depLibOrder != null && !depLibOrder.isEmpty()) {
        final List tmp = new LinkedList();

        for (final Object aDepLibOrder : depLibOrder) {
          final String depToOrderName = (String) aDepLibOrder;

          for (final Iterator j = depLibs.iterator(); j.hasNext(); ) {
            final NarArtifact dep = (NarArtifact) j.next();
            final String depName = dep.getGroupId() + ":" + dep.getArtifactId();

            if (depName.equals(depToOrderName)) {
              tmp.add(dep);
              j.remove();
            }
          }
        }

        tmp.addAll(depLibs);
        depLibs = tmp;
      }

      for (final Object depLib : depLibs) {
        final NarArtifact dependency = (NarArtifact) depLib;

        // FIXME no handling of "local"

        // FIXME, no way to override this at this stage
        final String binding = dependency.getNarInfo().getBinding(getAOL(), Library.NONE);
        getLog().debug("Using Binding: " + binding);
        AOL aol = getAOL();
        aol = dependency.getNarInfo().getAOL(getAOL());
        getLog().debug("Using Library AOL: " + aol.toString());

        if (!binding.equals(Library.JNI) && !binding.equals(Library.NONE) && !binding.equals(Library.EXECUTABLE)) {
          final File unpackDirectory = getUnpackDirectory();

          final File dir = getLayout()
              .getLibDirectory(unpackDirectory, dependency.getArtifactId(), dependency.getBaseVersion(), aol.toString(),
                  binding);

          getLog().debug("Looking for Library Directory: " + dir);
          if (dir.exists()) {
            final LibrarySet libSet = new LibrarySet();
            libSet.setProject(antProject);

            // FIXME, no way to override
            final String libs = dependency.getNarInfo().getLibs(getAOL());
            if (libs != null && !libs.equals("")) {
              getLog().debug("Using LIBS = " + libs);
              libSet.setLibs(new CUtil.StringArrayBuilder(libs));
              libSet.setDir(dir);
              task.addLibset(libSet);
            }
          } else {
            getLog().debug("Library Directory " + dir + " does NOT exist.");
          }

          // FIXME, look again at this, for multiple dependencies we may need to
          // remove duplicates
          final String options = dependency.getNarInfo().getOptions(getAOL());
          if (options != null && !options.equals("")) {
            getLog().debug("Using OPTIONS = " + options);
            final LinkerArgument arg = new LinkerArgument();
            arg.setValue(options);
            linkerDefinition.addConfiguredLinkerArg(arg);
          }

          final String sysLibs = dependency.getNarInfo().getSysLibs(getAOL());
          if (sysLibs != null && !sysLibs.equals("")) {
            getLog().debug("Using SYSLIBS = " + sysLibs);
            final SystemLibrarySet sysLibSet = new SystemLibrarySet();
            sysLibSet.setProject(antProject);

            sysLibSet.setLibs(new CUtil.StringArrayBuilder(sysLibs));
            task.addSyslibset(sysLibSet);
          }
        }
      }
    }

    // Add JVM to linker
    getJava().addRuntime(task, getJavaHome(getAOL()), getOS(), getAOL().getKey() + ".java.");

    // execute
    try {
      task.execute();
    } catch (final BuildException e) {
      throw new MojoExecutionException("NAR: Compile failed", e);
    }

    // FIXME, this should be done in CPPTasks at some point
    // getRuntime(getAOL()).equals("dynamic") &&
    if ((isEmbedManifest() || getLinker().isGenerateManifest()) && getOS().equals(OS.WINDOWS)
        && getLinker().getName().equals("msvc") && !getLinker().getVersion(this).startsWith("6.")) {
      final String[] env = new String[] {
        "PATH=" + getMsvc().getPathVariable().getValue()
      };
      final String libType = library.getType();
      if (Library.JNI.equals(libType) || Library.SHARED.equals(libType) || Library.EXECUTABLE.equals(libType)) {
        Vector<String> commandlineArgs = new Vector<>();
        commandlineArgs.add("/manifest");
        getManifests(outFile.getPath(), commandlineArgs);
        if (commandlineArgs.size() == 1) {
          if (isEmbedManifest())
            getLog().warn("Embed manifest requested, no source manifests to embed, no manifest generated");
        } else {
          if (Library.JNI.equals(libType) || Library.SHARED.equals(libType)) {
            String dll = outFile.getPath() + ".dll";
            if (isEmbedManifest()) {
              commandlineArgs.add("/outputresource:" + dll + ";#2");
            } else {
              commandlineArgs.add("/out:" + dll + ".manifest");
            }
          } else // if (Library.EXECUTABLE.equals( libType ))
          {
            String exe = outFile.getPath() + ".exe";
            if (isEmbedManifest()) {
              commandlineArgs.add("/outputresource:" + exe + ";#1");
            } else {
              commandlineArgs.add("/out:" + exe + ".manifest");
            }
          }
          String[] commandlineArgsArray = commandlineArgs.toArray(new String[0]);
          String mtexe = "mt.exe";
          if (getMsvc().compareVersion( getMsvc().getWindowsSdkVersion(),"7.0")<0 && getLinker().getVersion(this).startsWith("8.")) { // VS2005 VC8 only one that includes mt.exe
            File mtexeFile = new File(getMsvc().getToolPath(), mtexe);
            if (mtexeFile.exists())
              mtexe = mtexeFile.getAbsolutePath();
          } else {
            File mtexeFile = new File(getMsvc().getSDKToolPath(), mtexe);
            if (mtexeFile.exists())
              mtexe = mtexeFile.getAbsolutePath();
          }
          int result = NarUtil.runCommand(mtexe, commandlineArgsArray, null, null, getLog());
          if (result != 0) {
            throw new MojoFailureException("MT.EXE failed with exit code: " + result);
          }
        }
      }
    }
    if( getOS().equals(OS.WINDOWS) && Library.STATIC.equals(library.getType()) ){  // option? should debug symbols always be provided.
      getLog().debug( "Copy static pdbs from intermediat dir to " + task.getOutfile().getParentFile() );
      try {
        NarUtil.copyDirectoryStructure(task.getObjdir(), task.getOutfile().getParentFile(), "**/*.pdb", NarUtil.DEFAULT_EXCLUDES );
      } catch (IOException e) {
        getLog().info( "Failed to copy pdbs from " + task.getObjdir() + "\nexception" + e.getMessage() );
      }
    }
  }

  /**
   * List the dependencies needed for compilation, those dependencies are used
   * to get the include paths needed for
   * compilation and to get the libraries paths and names needed for linking.
   */
  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    return new ScopeFilter(Artifact.SCOPE_COMPILE, null);
  }

  private List getSourcesFor(final Compiler compiler) throws MojoFailureException, MojoExecutionException {
    if (compiler == null) {
      return Collections.emptyList();
    }

    try {
      final List files = new ArrayList();
      final List srcDirs = compiler.getSourceDirectories();
      for (final Object srcDir : srcDirs) {
        final File dir = (File) srcDir;
        if (dir.exists()) {
          files.addAll(FileUtils.getFiles(dir, StringUtils.join(compiler.getIncludes().iterator(), ","), null));
        }
      }
      return files;
    } catch (final IOException e) {
      return Collections.emptyList();
    }
  }

  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {

    // make sure destination is there
    getTargetDirectory().mkdirs();

    // check for source files
    int noOfSources = 0;
    noOfSources += getSourcesFor(getCpp()).size();
    noOfSources += getSourcesFor(getC()).size();
    noOfSources += getSourcesFor(getFortran()).size();
    if (noOfSources > 0) {
      getLog().info("Compiling " + noOfSources + " native files");
      for (final Library library : getLibraries()) {
        createLibrary(getAntProject(), library);
      }
    } else {
      getLog().info("Nothing to compile");
    }

    try {
      // FIXME, should the include paths be defined at a higher level ?
      copyInclude(getCpp());
      copyInclude(getC());
      copyInclude(getFortran());
    } catch (final IOException e) {
      throw new MojoExecutionException("NAR: could not copy include files", e);
    }

    getNarInfo().writeToDirectory(this.classesDirectory);
  }

  public boolean isEmbedManifest() {
    return embedManifest;
  }

  private void getManifests(String generated, Vector<String> manifests) {
    // TODO: /manifest should be followed by the list of manifest files
    // - the one generated by link, any others provided in source.
    // search the source for .manifest files.
    if (getLinker().isGenerateManifest())
      manifests.add(generated + ".manifest");
  }

}
