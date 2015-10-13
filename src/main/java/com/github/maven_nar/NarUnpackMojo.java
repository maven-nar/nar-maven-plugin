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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;

/**
 * Unpacks NAR files needed for compilation. Unpacking happens in the project
 * target folder, and also sets flags on
 * binaries and corrects static libraries.
 *
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-unpack", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = true,
  requiresDependencyResolution = ResolutionScope.COMPILE)
public class NarUnpackMojo extends AbstractDependencyMojo {

  /**
   * List of tests to create
   */
  @Parameter
  private List tests;
  
  /**
   * List the dependencies needed for compilation.
   */
  @Override
  protected ScopeFilter getArtifactScopeFilter() {
    return new ScopeFilter( Artifact.SCOPE_COMPILE, null );
  }

  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {
    final List<AttachedNarArtifact> attachedNarArtifacts = getAttachedNarArtifacts(libraries);
    unpackAttachedNars(attachedNarArtifacts);
  }
}
