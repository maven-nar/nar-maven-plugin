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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

/**
 * Copies the GNU style source files to a target area, autogens and configures
 * them.
 * 
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-gnu-configure", requiresProject = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class NarGnuConfigureMojo extends AbstractGnuMojo {

  private static final String AUTOGEN = "autogen.sh";

  private static final String BUILDCONF = "buildconf";

  private static final String CONFIGURE = "configure";

  // JDK 1.4 compatibility
  private static String arraysToString(final Object[] a) {
    if (a == null) {
      return "null";
    }
    final int iMax = a.length - 1;
    if (iMax == -1) {
      return "[]";
    }

    final StringBuilder b = new StringBuilder();
    b.append('[');
    for (int i = 0;; i++) {
      b.append(String.valueOf(a[i]));
      if (i == iMax) {
        return b.append(']').toString();
      }
      b.append(", ");
    }
  }

  /**
   * If true, we run <code>./configure</code> in the source directory instead of
   * copying the
   * source code to the <code>target/</code> directory first (this saves disk
   * space but
   * violates Maven's paradigm of keeping generated files inside the
   * <code>target/</code> directory structure.
   */
  @Parameter(property = "nar.gnu.configure.in-place")
  private boolean gnuConfigureInPlace;

  /**
   * Skip running of autogen.sh (aka buildconf).
   */
  @Parameter(property = "nar.gnu.autogen.skip")
  private boolean gnuAutogenSkip;

  /**
   * Skip running of configure and therefore also autogen.sh
   */
  @Parameter(property = "nar.gnu.configure.skip")
  private boolean gnuConfigureSkip;

  /**
   * Arguments to pass to GNU configure.
   */
  @Parameter(property = "nar.gnu.configure.args", defaultValue = "")
  private String gnuConfigureArgs;

  /**
   * Arguments to pass to GNU buildconf.
   */
  @Parameter(property = "nar.gnu.buildconf.args", defaultValue = "")
  private String gnuBuildconfArgs;

  public NarGnuConfigureMojo() {
  }

  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {

    if (!useGnu()) {
      return;
    }

    final File sourceDir = getGnuSourceDirectory();
    if (sourceDir.exists()) {
      File targetDir;

      if (!this.gnuConfigureInPlace) {
        targetDir = getGnuAOLSourceDirectory();

        getLog().info("Copying GNU sources");

        try {
          FileUtils.mkdir(targetDir.getPath());
          NarUtil.copyDirectoryStructure(sourceDir, targetDir, null, null);
        } catch (final IOException e) {
          throw new MojoExecutionException("Failed to copy GNU sources", e);
        }

        if (!this.gnuConfigureSkip && !this.gnuAutogenSkip) {
          final File autogen = new File(targetDir, AUTOGEN);
          final File buildconf = new File(targetDir, BUILDCONF);
          if (autogen.exists()) {
            getLog().info("Running GNU " + AUTOGEN);
            runAutogen(autogen, targetDir, null);
          } else if (buildconf.exists()) {
            getLog().info("Running GNU " + BUILDCONF);
            String gnuBuildconfArgsArray[] = null;
            if (this.gnuBuildconfArgs != null) {
              gnuBuildconfArgsArray = this.gnuBuildconfArgs.split("\\s");
            }
            runAutogen(buildconf, targetDir, gnuBuildconfArgsArray);
          }
        }
      } else {
        targetDir = sourceDir;
      }

      final File configure = new File(targetDir, CONFIGURE);
      if (!this.gnuConfigureSkip && configure.exists()) {
        getLog().info("Running GNU " + CONFIGURE);

        NarUtil.makeExecutable(configure, getLog());
        String[] args = null;

        // create the array to hold constant and additional args
        if (this.gnuConfigureArgs != null) {
          final String[] a = this.gnuConfigureArgs.split(" ");
          args = new String[a.length + 2];

          System.arraycopy(a, 0, args, 2, a.length);
        } else {
          args = new String[2];
        }

        // first 2 args are constant
        args[0] = configure.getAbsolutePath();
        args[1] = "--prefix=" + getGnuAOLTargetDirectory().getAbsolutePath();

        final File buildDir = getGnuAOLSourceDirectory();
        FileUtils.mkdir(buildDir.getPath());

        getLog().info("args: " + arraysToString(args));
        final int result = NarUtil.runCommand("sh", args, buildDir, null, getLog());
        if (result != 0) {
          throw new MojoExecutionException("'" + CONFIGURE + "' errorcode: " + result);
        }
      }
    }
  }

  private void runAutogen(final File autogen, final File targetDir, final String args[])
      throws MojoExecutionException, MojoFailureException {
    // fix missing config directory
    final File configDir = new File(targetDir, "config");
    if (!configDir.exists()) {
      configDir.mkdirs();
    }

    NarUtil.makeExecutable(autogen, getLog());
    getLog().debug("running sh ./" + autogen.getName());

    String arguments[] = null;
    if (args != null) {
      arguments = new String[1 + args.length];
      System.arraycopy(args, 0, arguments, 1, args.length);
    } else {
      arguments = new String[1];
    }
    arguments[0] = "./" + autogen.getName();

    getLog().info("args: " + arraysToString(arguments));

    final int result = NarUtil.runCommand("sh", arguments, targetDir, null, getLog());
    if (result != 0) {
      throw new MojoExecutionException("'" + autogen.getName() + "' errorcode: " + result);
    }
  }

}
