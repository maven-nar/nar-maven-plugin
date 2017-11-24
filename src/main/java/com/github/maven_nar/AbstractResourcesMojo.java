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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Keeps track of resources
 *
 * @author Mark Donszelmann
 */
public abstract class AbstractResourcesMojo extends AbstractNarMojo {
  /**
   * Binary directory
   */
  @Parameter(defaultValue = "bin", required = true)
  private String resourceBinDir;

  /**
   * Include directory
   */
  @Parameter(defaultValue = "include", required = true)
  private String resourceIncludeDir;

  /**
   * Library directory
   */
  @Parameter(defaultValue = "lib", required = true)
  private String resourceLibDir;

  /**
   * To look up Archiver/UnArchiver implementations
   */
  @Component(role = org.codehaus.plexus.archiver.manager.ArchiverManager.class)
  private ArchiverManager archiverManager;

  protected final int copyBinaries(final File srcDir, final String aol)
      throws IOException, MojoExecutionException, MojoFailureException {
    int copied = 0;

    // copy binaries
    final File binDir = new File(srcDir, this.resourceBinDir);
    if (binDir.exists()) {
      final File binDstDir = getLayout().getBinDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
          getMavenProject().getVersion(), aol);
      getLog().debug("Copying binaries from " + binDir + " to " + binDstDir);
      copied += NarUtil.copyDirectoryStructure(binDir, binDstDir, null, NarUtil.DEFAULT_EXCLUDES);
    }

    return copied;
  }

  protected final int copyIncludes(final File srcDir) throws IOException, MojoExecutionException, MojoFailureException {
    int copied = 0;

    // copy includes
    final File includeDir = new File(srcDir, this.resourceIncludeDir);
    if (includeDir.exists()) {
      final File includeDstDir = getLayout().getIncludeDirectory(getTargetDirectory(),
          getMavenProject().getArtifactId(), getMavenProject().getVersion());
      getLog().debug("Copying includes from " + includeDir + " to " + includeDstDir);
      copied += NarUtil.copyDirectoryStructure(includeDir, includeDstDir, null, NarUtil.DEFAULT_EXCLUDES);
    }

    return copied;
  }

  protected final int copyLibraries(final File srcDir, final String aol)
      throws MojoFailureException, IOException, MojoExecutionException {
    int copied = 0;

    // copy libraries
    File libDir = new File(srcDir, this.resourceLibDir);
    if (libDir.exists()) {
      // TODO: copyLibraries is used on more than just this artifact - this
      // check needs to be placed elsewhere
      if (getLibraries().isEmpty()) {
        getLog().warn("Appear to have library resources, but not Libraries are defined");
      }
      // create all types of libs
      for (final Object element : getLibraries()) {
        final Library library = (Library) element;
        final String type = library.getType();

        final File typedLibDir = new File(libDir, type);
        if (typedLibDir.exists()) {
          libDir = typedLibDir;
        }

        final File libDstDir = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
            getMavenProject().getVersion(), aol, type);
        getLog().debug("Copying libraries from " + libDir + " to " + libDstDir);

        // filter files for lib
        String includes = "**/*."
            + NarProperties.getInstance(getMavenProject()).getProperty(
                NarUtil.getAOLKey(aol) + "." + type + ".extension");

        // add import lib for Windows shared libraries
        if (new AOL(aol).getOS().equals(OS.WINDOWS) && type.equals(Library.SHARED)) {
          includes += ",**/*.lib";
        }
        copied += NarUtil.copyDirectoryStructure(libDir, libDstDir, includes, NarUtil.DEFAULT_EXCLUDES);
      }
    }

    return copied;
  }

  protected final void copyResources(final File srcDir, final String aol)
      throws MojoExecutionException, MojoFailureException {
    int copied = 0;
    try {
      copied += copyIncludes(srcDir);

      copied += copyBinaries(srcDir, aol);

      copied += copyLibraries(srcDir, aol);

      // unpack jar files
      final File classesDirectory = new File(getOutputDirectory(), "classes");
      classesDirectory.mkdirs();
      final List<File> jars = FileUtils.getFiles(srcDir, "**/*.jar", null);
      for (final File jar : jars) {
        getLog().debug("Unpacking jar " + jar);
        UnArchiver unArchiver;
        unArchiver = this.archiverManager.getUnArchiver(NarConstants.NAR_ROLE_HINT);
        unArchiver.setSourceFile(jar);
        unArchiver.setDestDirectory(classesDirectory);
        unArchiver.extract();
      }
    } catch (final IOException e) {
      throw new MojoExecutionException("NAR: Could not copy resources for " + aol, e);
    } catch (final NoSuchArchiverException e) {
      throw new MojoExecutionException("NAR: Could not find archiver for " + aol, e);
    } catch (final ArchiverException e) {
      throw new MojoExecutionException("NAR: Could not unarchive jar file for " + aol, e);
    }
    getLog().info("Copied " + copied + " resources for " + aol);
  }

}
