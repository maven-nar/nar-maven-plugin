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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Tests NAR files. Runs Native Tests and executables if produced.
 *
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-test", defaultPhase = LifecyclePhase.TEST, requiresProject = true,
  requiresDependencyResolution = ResolutionScope.TEST)
public class NarTestMojo extends AbstractCompileMojo {
  /**
   * The classpath elements of the project being tested.
   */
  @Parameter(defaultValue = "${project.testClasspathElements}", required = true, readonly = true)
  private List classpathElements;

  /**
   * Directory for test resources. Defaults to src/test/resources
   */
  @Parameter(defaultValue = "${basedir}/src/test/resources", required = true)
  private File testResourceDirectory;

  private String[] generateEnvironment() throws MojoExecutionException, MojoFailureException {
    final List env = new ArrayList();

    final Set/* <File> */sharedPaths = new HashSet();

    // add all shared libraries of this package
    for (final Object element : getLibraries()) {
      final Library lib = (Library) element;
      if (lib.getType().equals(Library.SHARED)) {
        final File path = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
            getMavenProject().getVersion(), getAOL().toString(), lib.getType());
        getLog().debug("Adding path to shared library: " + path);
        sharedPaths.add(path);
      }
    }

    // add dependent shared libraries
    final String classifier = getAOL() + "-shared";
    final List narArtifacts = getNarArtifacts();
    final List dependencies = getNarManager().getAttachedNarDependencies(narArtifacts, classifier);
    for (final Object dependency1 : dependencies) {
      final Artifact dependency = (Artifact) dependency1;
      getLog().debug("Looking for dependency " + dependency);

      // FIXME reported to maven developer list, isSnapshot
      // changes behaviour
      // of getBaseVersion, called in pathOf.
      dependency.isSnapshot();

      final File libDirectory = getLayout()
          .getLibDirectory(getUnpackDirectory(), dependency.getArtifactId(), dependency.getBaseVersion(),
              getAOL().toString(), Library.SHARED);
      sharedPaths.add(libDirectory);
    }

    // set environment
    if (sharedPaths.size() > 0) {
      String sharedPath = "";
      for (final Iterator i = sharedPaths.iterator(); i.hasNext();) {
        sharedPath += ((File) i.next()).getPath();
        if (i.hasNext()) {
          sharedPath += File.pathSeparator;
        }
      }

      final String sharedEnv = NarUtil.addLibraryPathToEnv(sharedPath, null, getOS());
      env.add(sharedEnv);
    }

    // necessary to find WinSxS
    if (getOS().equals(OS.WINDOWS)) {
      env.add("SystemRoot=" + NarUtil.getEnv("SystemRoot", "SystemRoot", "C:\\Windows"));
    }

    // add CLASSPATH
    env.add("CLASSPATH=" + StringUtils.join(this.classpathElements.iterator(), File.pathSeparator));

    return env.size() > 0 ? (String[]) env.toArray(new String[env.size()]) : null;
  }

  /**
   * List the dependencies needed for tests executions and for executables
   * executions, those dependencies are used
   * to declare the paths of shared libraries for execution.
   */
  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    return new ScopeFilter( Artifact.SCOPE_TEST, null );
  }

  @Override
  protected File getUnpackDirectory() {
    return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
  }

  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {
    if (this.skipTests) {
      getLog().info("Tests are skipped");
    } else {

      // run all tests
      for (final Object o : getTests()) {
        runTest((Test) o);
      }

      for (final Object element : getLibraries()) {
        runExecutable((Library) element);
      }
    }
  }

  private void runExecutable(final Library library) throws MojoExecutionException, MojoFailureException {
    if (library.getType().equals(Library.EXECUTABLE) && library.shouldRun()) {
      final MavenProject project = getMavenProject();
      // FIXME NAR-90, we could make sure we get the final name from layout
      final String extension = getOS().equals(OS.WINDOWS) ? ".exe" : "";
      final File executable = new File(getLayout().getBinDirectory(getTargetDirectory(),
          getMavenProject().getArtifactId(), getMavenProject().getVersion(), getAOL().toString()),
          project.getArtifactId() + extension);
      if (!executable.exists()) {
        getLog().warn("Skipping non-existing executable " + executable);
        return;
      }
      getLog().info("Running executable " + executable);
      final List args = library.getArgs();
      final int result = NarUtil.runCommand(executable.getPath(), (String[]) args.toArray(new String[args.size()]),
          null, generateEnvironment(), getLog());
      if (result != 0) {
        throw new MojoFailureException("Test " + executable + " failed with exit code: " + result + " 0x"
            + Integer.toHexString(result));
      }
    }
  }

  private void runTest(final Test test) throws MojoExecutionException, MojoFailureException {
    // run if requested
    if (test.shouldRun()) {
      // NOTE should we use layout here ?
      final String name = test.getName() + (getOS().equals(OS.WINDOWS) ? ".exe" : "");
      File path = new File(getTestTargetDirectory(), "bin");
      path = new File(path, getAOL().toString());
      path = new File(path, name);
      if (!path.exists()) {
        getLog().warn("Skipping non-existing test " + path);
        return;
      }

      final File workingDir = new File(getTestTargetDirectory(), "test-reports");
      workingDir.mkdirs();

      // Copy test resources
      try {
        int copied = 0;
        if (this.testResourceDirectory.exists()) {
          copied += NarUtil.copyDirectoryStructure(this.testResourceDirectory, workingDir, null,
              NarUtil.DEFAULT_EXCLUDES);
        }
        getLog().info("Copied " + copied + " test resources");
      } catch (final IOException e) {
        throw new MojoExecutionException("NAR: Could not copy test resources", e);
      }

      getLog().info("Running test " + name + " in " + workingDir);

      final List args = test.getArgs();
      final int result = NarUtil.runCommand(path.toString(), (String[]) args.toArray(new String[args.size()]),
          workingDir, generateEnvironment(), getLog());
      if (result != 0) {
        throw new MojoFailureException("Test " + name + " failed with exit code: " + result + " 0x"
            + Integer.toHexString(result));
      }
    }
  }
}
