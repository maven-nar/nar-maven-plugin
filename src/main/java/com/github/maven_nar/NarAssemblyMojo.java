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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.codehaus.plexus.util.FileUtils;

/**
 * Assemble libraries of NAR files.
 *
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-assembly", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true,
  requiresDependencyResolution = ResolutionScope.TEST)
public class NarAssemblyMojo extends AbstractDependencyMojo {
  /**
   * List the dependencies we want to assemble
   */
  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    // Was Artifact.SCOPE_RUNTIME  + provided?
    // Think Provided isn't appropriate in Assembly - otherwise it isn't provided.
    return new ScopeFilter( Artifact.SCOPE_RUNTIME, null );
  }

  /**
   * Copies the unpacked nar libraries and files into the projects target area
   */
  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {
    // download the dependencies if needed in local maven repository.
    List<AttachedNarArtifact> attachedNarArtifacts = getAttachedNarArtifacts(libraries);
    downloadAttachedNars(attachedNarArtifacts);

    // Warning, for SNAPSHOT artifacts that were not in the local maven
    // repository, the downloadAttachedNars
    // method above modified the version in the AttachedNarArtifact object to
    // set the timestamp version from
    // the web repository.
    // In order to unpack the files with the correct names we need to get back
    // the AttachedNarArtifact objects with
    // -SNAPSHOT versions, so we call again getAttachedNarArtifacts() to get the
    // unmodified AttachedNarArtifact
    // objects
    attachedNarArtifacts = getAttachedNarArtifacts(libraries);
    unpackAttachedNars(attachedNarArtifacts);

    // this may make some extra copies...
    for (final Object element : attachedNarArtifacts) {
      final Artifact dependency = (Artifact) element;
      getLog().debug("Assemble from " + dependency);

      // FIXME reported to maven developer list, isSnapshot
      // changes behaviour
      // of getBaseVersion, called in pathOf.
      dependency.isSnapshot();

      final File srcDir = getLayout().getNarUnpackDirectory(getUnpackDirectory(),
          getNarManager().getNarFile(dependency));
      // File srcDir = new File( getLocalRepository().pathOf( dependency ) );
      // srcDir = new File( getLocalRepository().getBasedir(),
      // srcDir.getParent() );
      // srcDir = new File( srcDir, "nar/" );

      final File dstDir = getTargetDirectory();
      try {
        FileUtils.mkdir(dstDir.getPath());
        getLog().debug("SrcDir: " + srcDir);
        if (srcDir.exists()) {
          FileUtils.copyDirectoryStructureIfModified(srcDir, dstDir);
        }
      } catch (final IOException ioe) {
        throw new MojoExecutionException("Failed to copy directory for dependency " + dependency + " from " + srcDir
            + " to " + dstDir, ioe);
      }
    }
  }
}
