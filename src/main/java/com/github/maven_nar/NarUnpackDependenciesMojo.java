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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * List all the dependencies of the project and downloads the NAR files in local
 * maven repository if needed, this
 * includes the noarch and aol type NAR files, and then unpack the files in the
 * project target folder. This also sets
 * flags on binaries and corrects static libraries.
 *
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-unpack-dependencies", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
  requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true)
public class NarUnpackDependenciesMojo extends NarDownloadDependenciesMojo {

  /**
   * List of tests to create
   */
  @Parameter
  private List tests;
  
  @Override
  public void narExecute() throws MojoFailureException, MojoExecutionException {
    // download the dependencies if needed in local maven repository using
    // NarDownloadDependenciesMojo
    super.narExecute();

    // unpack the nar files
    final List<AttachedNarArtifact> attachedNarArtifacts = getAttachedNarArtifacts(tests);
    unpackAttachedNars(attachedNarArtifacts);
  }

}
