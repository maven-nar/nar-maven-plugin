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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Runs make on the GNU style generated Makefile
 * 
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-gnu-make", requiresProject = true, defaultPhase = LifecyclePhase.COMPILE)
public class NarGnuMakeMojo extends AbstractGnuMojo {
  /**
   * Space delimited list of arguments to pass to make
   */
  @Parameter(defaultValue = "")
  private String gnuMakeArgs;

  /**
   * Comma delimited list of environment variables to setup before running make
   */
  @Parameter(defaultValue = "")
  private String gnuMakeEnv;
  
  /**
   * Skip running of make.
   * Useful if you just want to run the configure step for generating source.
   */
  @Parameter(property = "nar.gnu.make.skip")
  private boolean gnuMakeSkip;

  /**
   * Boolean to control if we should skip 'make install' after the make
   */
  @Parameter
  private boolean gnuMakeInstallSkip;

  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {
    if (!useGnu() || gnuMakeSkip) {
      return;
    }

    final File srcDir = getGnuAOLSourceDirectory();
    if (srcDir.exists()) {
      String[] args = null;
      String[] env = null;

      if (this.gnuMakeArgs != null) {
        args = this.gnuMakeArgs.split(" ");
      }
      if (this.gnuMakeEnv != null) {
        env = this.gnuMakeEnv.split(",");
      }

      getLog().info("Running GNU make");
      int result = NarUtil.runCommand("make", args, srcDir, env, getLog());
      if (result != 0) {
        throw new MojoExecutionException("'make' errorcode: " + result);
      }

      if (!this.gnuMakeInstallSkip) {
        getLog().info("Running make install");
        if (args != null) {
          this.gnuMakeArgs = this.gnuMakeArgs + " install";
          args = this.gnuMakeArgs.split(" ");
        } else {
          args = new String[] {
            "install"
          };
        }
        result = NarUtil.runCommand("make", args, srcDir, null, getLog());
        if (result != 0) {
          throw new MojoExecutionException("'make install' errorcode: " + result);
        }
      }
    }
  }
}
