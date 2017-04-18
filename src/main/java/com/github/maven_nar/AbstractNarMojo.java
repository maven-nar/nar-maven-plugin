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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractNarMojo extends AbstractMojo implements NarConstants {

  /**
   * Skip running of NAR plugins (any) altogether.
   */
  @Parameter(property = "nar.skip", defaultValue = "false")
  protected boolean skip;

  /**
   * Skip the tests. Listens to Maven's general 'maven.skip.test'.
   */
  @Parameter(property = "maven.test.skip")
  boolean skipTests;

  /**
   * Ignore errors and failures.
   */
  @Parameter(property = "nar.ignore", defaultValue = "false")
  private boolean ignore;

  /**
   * The Architecture for the nar, Some choices are: "x86", "i386", "amd64",
   * "ppc", "sparc", ... Defaults to a derived
   * value from ${os.arch}
   */
  @Parameter(property = "nar.arch")
  private String architecture;

  /**
   * The Operating System for the nar. Some choices are: "Windows", "Linux",
   * "MacOSX", "SunOS","AIX" ... Defaults to a
   * derived value from ${os.name} FIXME table missing
   */
  @Parameter(property = "nar.os")
  private String os;

  /**
   * Architecture-OS-Linker name. Defaults to: arch-os-linker.
   */
  @Parameter(defaultValue = "")
  private String aol;

  /**
   * Linker
   */
  @Parameter
  private Linker linker;

  // these could be obtained from an injected project model.

  @Parameter(property = "project.build.directory", readonly = true)
  private File outputDirectory;

  @Parameter(property = "project.build.outputDirectory", readonly = true)
  protected File classesDirectory;

  /**
   * Name of the output
   * - for jni default-value="${project.artifactId}-${project.version}"
   * - for libs default-value="${project.artifactId}-${project.version}"
   * - for exe default-value="${project.artifactId}"
   * -- for tests default-value="${test.name}"
   * 
   */
  @Parameter
  private String output;

  @Parameter(property = "project.basedir", readonly = true)
  private File baseDir;

  /**
   * Target directory for Nar file construction. Defaults to
   * "${project.build.directory}/nar" for "nar-compile" goal
   */
  @Parameter
  private File targetDirectory;

  /**
   * Target directory for Nar test construction. Defaults to
   * "${project.build.directory}/test-nar" for "nar-testCompile" goal
   */
  @Parameter
  private File testTargetDirectory;

  /**
   * Target directory for Nar file unpacking. Defaults to "${targetDirectory}"
   */
  @Parameter
  private File unpackDirectory;

  /**
   * Target directory for Nar test unpacking. Defaults to
   * "${testTargetDirectory}"
   */
  @Parameter
  private File testUnpackDirectory;

    /**
     * NARVersionInfo for Windows binaries
     *
     */
    @Parameter
    private NARVersionInfo versionInfo;

  /**
   * List of classifiers which you want download/unpack/assemble
   * Example ppc-MacOSX-g++, x86-Windows-msvc, i386-Linux-g++.
   * Not setting means all.
   */
  @Parameter
  protected List<String> classifiers;

  /**
   * List of libraries to create
   */
  @Parameter
  protected List<Library> libraries;

  /**
   * Name of the libraries included
   */
  @Parameter
  private String libsName;

  /**
   * Layout to be used for building and unpacking artifacts
   */
  @Parameter(property = "nar.layout", defaultValue = "com.github.maven_nar.NarLayout21", required = true)
  private String layout;

  private NarLayout narLayout;

  @Component
  private MavenProject mavenProject;

  private AOL aolId;

  private NarInfo narInfo;

  /**
   * Javah info
   */
  @Parameter
  private Javah javah;

  /**
   * The home of the Java system. Defaults to a derived value from ${java.home}
   * which is OS specific.
   */
  @Parameter(readonly = true)
  private File javaHome;

  @Parameter
  private Msvc msvc = new Msvc();

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (this.skip) {
      getLog().info(getClass().getName() + " skipped");
      return;
    }

    try {
      validate();
      narExecute();
    } catch (final MojoFailureException | MojoExecutionException mfe) {
      if (this.ignore) {
        getLog().warn("IGNORED: " + mfe.getMessage());
      } else {
        throw mfe;
      }
    }
  }

  protected final AOL getAOL() throws MojoFailureException, MojoExecutionException {
    return this.aolId;
  }

  protected final String getArchitecture() {
    return this.architecture;
  }

  protected final File getBasedir() {
    return this.baseDir;
  }

  protected final Javah getJavah() {
    if (this.javah == null) {
      this.javah = new Javah();
    }
    this.javah.setAbstractCompileMojo(this);
    return this.javah;
  }

  protected final File getJavaHome(final AOL aol) throws MojoExecutionException {
    // FIXME should be easier by specifying default...
    return getNarInfo().getProperty(aol, "javaHome", NarUtil.getJavaHome(this.javaHome, getOS()));
  }

  protected final NarLayout getLayout() throws MojoExecutionException {
    if (this.narLayout == null) {
      this.narLayout = AbstractNarLayout.getLayout(this.layout, getLog());
    }
    return this.narLayout;
  }

  protected final List<Library> getLibraries() {
    if (this.libraries == null) {
      this.libraries = Collections.emptyList();
    }
    return this.libraries;
  }

  protected final Linker getLinker() {
    return this.linker;
  }

    protected final NARVersionInfo getNARVersionInfo()
    {
        return versionInfo;
    }

  protected final MavenProject getMavenProject() {
    return this.mavenProject;
  }

  public Msvc getMsvc() throws MojoFailureException, MojoExecutionException {
    this.msvc.setMojo(this);
    return this.msvc;
  }

  protected NarInfo getNarInfo() throws MojoExecutionException {
    if (this.narInfo == null) {
      final String groupId = getMavenProject().getGroupId();
      final String artifactId = getMavenProject().getArtifactId();
      final String path = "META-INF/nar/" + groupId + "/" + artifactId + "/" + NarInfo.NAR_PROPERTIES;
      File propertiesFile = new File(this.classesDirectory, path);
      // should not need to try and read from source.
      if (!propertiesFile.exists()) {
        propertiesFile = new File(getMavenProject().getBasedir(), "src/main/resources/" + path);
      }

      this.narInfo = new NarInfo(groupId, artifactId, getMavenProject().getVersion(), getLog(), propertiesFile);
    }
    return this.narInfo;
  }

  protected final String getOS() {
    return this.os;
  }

  protected final String getOutput(final boolean versioned) throws MojoExecutionException {
    if (this.output != null && !this.output.trim().isEmpty()) {
      return this.output;
    } else {
      if (versioned) {
        return getMavenProject().getArtifactId() + "-" + getMavenProject().getVersion();
      } else {
        return getMavenProject().getArtifactId();
      }
    }
  }

  protected final String getLibsName() throws MojoExecutionException {
    if (this.libsName != null && !this.libsName.trim().isEmpty()) {
      return this.libsName;
    } else {
      return null;
    }
  }

  protected final File getOutputDirectory() {
    return this.outputDirectory;
  }

  protected final File getTargetDirectory() {
    return this.targetDirectory;
  }

  protected final File getTestTargetDirectory() {
    return this.testTargetDirectory;
  }

  protected final File getTestUnpackDirectory() {
    return this.testUnpackDirectory;
  }

  protected File getUnpackDirectory() {
    return this.unpackDirectory;
  }

  public abstract void narExecute() throws MojoFailureException, MojoExecutionException;

  protected final void validate() throws MojoFailureException, MojoExecutionException {

    this.architecture = NarUtil.getArchitecture(this.architecture);
    this.os = NarUtil.getOS(this.os);
    this.linker = NarUtil.getLinker(this.linker, getLog()); // linker name set in NarUtil.getAOL if not configured
    this.aolId = NarUtil.getAOL(this.mavenProject, this.architecture, this.os, this.linker, this.aol, getLog());
    this.msvc.setMojo(this);

    final Model model = this.mavenProject.getModel();
    final Properties properties = model.getProperties();
    properties.setProperty("nar.arch", getArchitecture());
    properties.setProperty("nar.os", getOS());
    properties.setProperty("nar.linker", getLinker().getName());
    properties.setProperty("nar.aol", this.aolId.toString());
    properties.setProperty("nar.aol.key", this.aolId.getKey());
    model.setProperties(properties);

    if (this.targetDirectory == null) {
      this.targetDirectory = new File(this.mavenProject.getBuild().getDirectory(), "nar");
    }
    if (this.testTargetDirectory == null) {
      this.testTargetDirectory = new File(this.mavenProject.getBuild().getDirectory(), "test-nar");
    }

    if (this.unpackDirectory == null) {
      this.unpackDirectory = this.targetDirectory;
    }
    if (this.testUnpackDirectory == null) {
      this.testUnpackDirectory = this.testTargetDirectory;
    }
  }
}
