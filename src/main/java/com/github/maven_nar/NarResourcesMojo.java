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
import org.codehaus.plexus.util.SelectorUtils;

/**
 * Copies any resources, including AOL specific distributions, to the target
 * area for packaging
 * 
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-resources", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true)
public class NarResourcesMojo extends AbstractResourcesMojo {
  /**
   * Use given AOL only. If false, copy for all available AOLs.
   */
  @Parameter(property = "nar.resources.copy.aol", defaultValue = "true", required = true)
  private boolean resourcesCopyAOL;

  /**
   * Directory for nar resources. Defaults to src/nar/resources
   */
  @Parameter(defaultValue = "${basedir}/src/nar/resources", required = true)
  private File resourceDirectory;

  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {
    // noarch resources
    try {
      int copied = 0;
      final File noarchDir = new File(this.resourceDirectory, NarConstants.NAR_NO_ARCH);
      if (noarchDir.exists()) {
        final File noarchDstDir = getLayout().getNoArchDirectory(getTargetDirectory(),
            getMavenProject().getArtifactId(), getMavenProject().getVersion());
        getLog().debug("Copying noarch from " + noarchDir + " to " + noarchDstDir);
        copied += NarUtil.copyDirectoryStructure(noarchDir, noarchDstDir, null, NarUtil.DEFAULT_EXCLUDES);
      }
      getLog().info("Copied " + copied + " resources");
    } catch (final IOException e) {
      throw new MojoExecutionException("NAR: Could not copy resources", e);
    }

    // scan resourceDirectory for AOLs
    final File aolDir = new File(this.resourceDirectory, NarConstants.NAR_AOL);
    if (aolDir.exists()) {
      final String[] aol = aolDir.list();
      for (final String anAol : aol) {
        // copy only resources of current AOL
        if (this.resourcesCopyAOL && !anAol.equals(getAOL().toString())) {
          continue;
        }

        boolean ignore = false;
        for (final Object element : FileUtils.getDefaultExcludesAsList()) {
          final String exclude = (String) element;
          if (SelectorUtils.matchPath(exclude.replace('/', File.separatorChar), anAol)) {
            ignore = true;
            break;
          }
        }
        if (!ignore) {
          final File aolFile = new File(aolDir, anAol);
          copyResources(aolFile, aolFile.getName());
        }
      }
    }
  }
}
