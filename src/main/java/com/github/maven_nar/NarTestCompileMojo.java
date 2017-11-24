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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.OutputTypeEnum;
import com.github.maven_nar.cpptasks.RuntimeType;
import com.github.maven_nar.cpptasks.SubsystemEnum;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

/**
 * Compiles native test source files.
 * 
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-testCompile", defaultPhase = LifecyclePhase.TEST_COMPILE,
  requiresDependencyResolution = ResolutionScope.TEST)
public class NarTestCompileMojo extends AbstractCompileMojo {
  /**
   * Skip running of NAR integration test plugins.
   */
  @Parameter(property = "skipNar")
  protected boolean skipNar;

  private void createTest(final Project antProject, final Test test)
      throws MojoExecutionException, MojoFailureException {
    final String type = "test";

    // configure task
    final CCTask task = new CCTask();
    task.setProject(antProject);

    // subsystem
    final SubsystemEnum subSystem = new SubsystemEnum();
    subSystem.setValue("console");
    task.setSubsystem(subSystem);

    // outtype
    final OutputTypeEnum outTypeEnum = new OutputTypeEnum();
    outTypeEnum.setValue(test.getType());
    task.setOuttype(outTypeEnum);

    // outDir
    File outDir = new File(getTestTargetDirectory(), "bin");
    outDir = new File(outDir, getAOL().toString());
    outDir.mkdirs();

    // outFile
    final File outFile = new File(outDir, test.getName());
    getLog().debug("NAR - output: '" + outFile + "'");
    task.setOutfile(outFile);

    // object directory
    File objDir = new File(getTestTargetDirectory(), "obj");
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

    // add C++ compiler
    final Cpp cpp = getCpp();
    if (cpp != null) {
      final CompilerDef cppCompiler = getCpp().getTestCompiler(type, test.getName());
      if (cppCompiler != null) {
        task.addConfiguredCompiler(cppCompiler);
      }
    }

    // add C compiler
    final C c = getC();
    if (c != null) {
      final CompilerDef cCompiler = c.getTestCompiler(type, test.getName());
      if (cCompiler != null) {
        task.addConfiguredCompiler(cCompiler);
      }
    }

    // add Fortran compiler
    final Fortran fortran = getFortran();
    if (fortran != null) {
      final CompilerDef fortranCompiler = getFortran().getTestCompiler(type, test.getName());
      if (fortranCompiler != null) {
        task.addConfiguredCompiler(fortranCompiler);
      }
    }

    // add java include paths
    getJava().addIncludePaths(task, type);

    getMsvc().configureCCTask(task);

    List depLibs = getNarArtifacts();
    
    // add dependency include paths
    for (final Object depLib1 : depLibs) {
      final Artifact artifact = (Artifact) depLib1;

      // check if it exists in the normal unpack directory
      File include = getLayout()
          .getIncludeDirectory(getUnpackDirectory(), artifact.getArtifactId(), artifact.getBaseVersion());
      if (!include.exists()) {
        // otherwise try the test unpack directory
        include = getLayout()
            .getIncludeDirectory(getTestUnpackDirectory(), artifact.getArtifactId(), artifact.getBaseVersion());
      }
      if (include.exists()) {
        task.createIncludePath().setPath(include.getPath());
      }
    }

    // add javah generated include path
    final File jniIncludeDir = getJavah().getJniDirectory();
    if (jniIncludeDir.exists()) {
      task.createIncludePath().setPath(jniIncludeDir.getPath());
    }

    // add linker
    final LinkerDef linkerDefinition = getLinker().getTestLinker(this, task, getOS(), getAOL().getKey() + ".linker.",
        type);
    task.addConfiguredLinker(linkerDefinition);

    final File includeDir = getLayout().getIncludeDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
        getMavenProject().getVersion());

    String linkType = test.getLink( getLibraries() );
    final File libDir = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
        getMavenProject().getVersion(), getAOL().toString(), linkType);

    // copy shared library
    // FIXME why do we do this ?
    /*
     * Removed in alpha-10 if (test.getLink().equals(Library.SHARED)) { try { //
     * defaults are Unix String libPrefix
     * = NarUtil.getDefaults().getProperty( getAOLKey() + "shared.prefix",
     * "lib"); String libExt =
     * NarUtil.getDefaults().getProperty( getAOLKey() + "shared.extension",
     * "so"); File copyDir = new
     * File(getTargetDirectory(), (getOS().equals( "Windows") ? "bin" : "lib") +
     * "/" + getAOL() + "/" +
     * test.getLink()); FileUtils.copyFileToDirectory(new File(libDir, libPrefix
     * + libName + "." + libExt),
     * copyDir); if (!getOS().equals(OS.WINDOWS)) { libDir = copyDir; } } catch
     * (IOException e) { throw new
     * MojoExecutionException( "NAR: Could not copy shared library", e); } }
     */
    // FIXME what about copying the other shared libs?

    // add include of this package
    if (includeDir.exists()) {
      task.createIncludePath().setLocation(includeDir);
    }

    // add library of this package
    if (libDir.exists()) {
      final LibrarySet libSet = new LibrarySet();
      libSet.setProject(antProject);

      // String libs = getNarInfo().getLibs( getAOL() );
      // using getNarInfo().getLibs( getAOL() ); forces to execute the goal
      // nar-prepare-package prior to
      // nar-testCompile in order to set the "output" property in narInfo with
      // the call :
      // narInfo.setOutput( null, mojo.getOutput(true) ); (set in
      // NarLayout21.prepareNarInfo(...))

      // narInfo.getLibs(aol) call in fact narInfo.getProperty( aol,
      // "libs.names", getOutput( aol, artifactId + "-" + version ) );
      // where getOutput is the getOutput method in narInfo (which needs the
      // "output" property).
      // We call then directly narInfo.getProperty( aol, "libs.names", <output
      // value>); but we set <output value>
      // with AbstractCompileMojo.getOutput( boolean versioned ) as it is done
      // during nar-prepare-package
      final String libs = getNarInfo().getProperty(getAOL(), "libs.names", getOutput(true));

      getLog().debug("Searching for parent to link with " + libs);
      libSet.setLibs(new CUtil.StringArrayBuilder(libs));
      final LibraryTypeEnum libType = new LibraryTypeEnum();
      libType.setValue(linkType);
      libSet.setType(libType);
      libSet.setDir(libDir);
      task.addLibset(libSet);
    }

    // add dependency libraries
    final List depLibOrder = getDependencyLibOrder();

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

      final String binding = getBinding(test, dependency);
      getLog().debug("Using Binding: " + binding);
      AOL aol = getAOL();
      aol = dependency.getNarInfo().getAOL(getAOL());
      getLog().debug("Using Library AOL: " + aol.toString());

      // We dont link against the following library types :
      // - JNI, they are Java libraries
      // - executable, they are not libraries
      // - none, they are not libraries ... I gess
      // Static libraries should be linked. Even though the libraries
      // themselves will have been tested already, the test code could
      // use methods or classes defined in them.
      if (!binding.equals(Library.JNI) && !binding.equals(Library.NONE) && !binding.equals(Library.EXECUTABLE)) {
        // check if it exists in the normal unpack directory
        File dir = getLayout()
            .getLibDirectory(getUnpackDirectory(), dependency.getArtifactId(), dependency.getBaseVersion(),
                aol.toString(), binding);
        getLog().debug("Looking for Library Directory: " + dir);
        if (!dir.exists()) {
          getLog().debug("Library Directory " + dir + " does NOT exist.");

          // otherwise try the test unpack directory
          dir = getLayout()
              .getLibDirectory(getTestUnpackDirectory(), dependency.getArtifactId(), dependency.getBaseVersion(),
                  aol.toString(), binding);
          getLog().debug("Looking for Library Directory: " + dir);
        }
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

    // Add JVM to linker
    getJava().addRuntime(task, getJavaHome(getAOL()), getOS(), getAOL().getKey() + ".java.");

    // execute
    try {
      task.execute();
    } catch (final BuildException e) {
      throw new MojoExecutionException("NAR: Test-Compile failed", e);
    }
  }

  /**
   * List the dependencies needed for tests compilations, those dependencies are
   * used to get the include paths needed
   * for compilation and to get the libraries paths and names needed for
   * linking.
   */
  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    // Was Artifact.SCOPE_TEST  - runtime??
    return new ScopeFilter( Artifact.SCOPE_TEST, null );
  }

  @Override
  protected File getUnpackDirectory() {
    return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
  }

  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {
    if (this.skipTests) {
      getLog().info("Not compiling test sources");
    } else {

      // make sure destination is there
      getTestTargetDirectory().mkdirs();

      for (final Object o : getTests()) {
        createTest(getAntProject(), (Test) o);
      }
    }
  }

}
