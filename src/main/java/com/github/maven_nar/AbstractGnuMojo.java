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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Abstract GNU Mojo keeps configuration
 *
 * @author Mark Donszelmann
 */
public abstract class AbstractGnuMojo extends AbstractResourcesMojo {
  /**
   * Use GNU goals on Windows
   */
  @Parameter(defaultValue = "nar.gnu.useonwindows", required = true)
  private boolean gnuUseOnWindows;

  /**
   * Source directory for GNU style project
   */
  @Parameter(defaultValue = "${basedir}/src/gnu")
  private File gnuSourceDirectory;

  /**
   * Directory in which gnu sources are copied and "configured"
   */
  @Parameter(defaultValue = "${project.build.directory}/nar/gnu")
  private File gnuTargetDirectory;

  /**
   * @return
   * @throws MojoFailureException
   * @throws MojoExecutionException
   */
  private File getGnuAOLDirectory() throws MojoFailureException, MojoExecutionException {
    return new File(this.gnuTargetDirectory, getAOL().toString());
  }

  /**
   * @return
   * @throws MojoFailureException
   * @throws MojoExecutionException
   */
  protected final File getGnuAOLSourceDirectory() throws MojoFailureException, MojoExecutionException {
    return new File(getGnuAOLDirectory(), "src");
  }

  /**
   * @return
   * @throws MojoFailureException
   * @throws MojoExecutionException
   */
  protected final File getGnuAOLTargetDirectory() throws MojoFailureException, MojoExecutionException {
    return new File(getGnuAOLDirectory(), "target");
  }

  protected final File getGnuSourceDirectory() {
    return this.gnuSourceDirectory;
  }

  /**
   * Returns true if we do not want to use GNU on Windows
   * 
   * @return
   */
  protected final boolean useGnu() {
    return this.gnuUseOnWindows || !NarUtil.isWindows();
  }
}
