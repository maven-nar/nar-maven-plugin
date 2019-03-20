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
import java.util.List;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarManager {

  private final Log log;

  private final MavenProject project;

  private final ArtifactRepository repository;

  private final AOL defaultAOL;

  private final String linkerName;

  private final String[] narTypes = {
      NarConstants.NAR_NO_ARCH, Library.STATIC, Library.SHARED, Library.JNI, Library.PLUGIN
  };

  public NarManager(final Log log, final ArtifactRepository repository, final MavenProject project,
      final String architecture, final String os, final Linker linker)
      throws MojoFailureException, MojoExecutionException {
    this.log = log;
    this.repository = repository;
    this.project = project;
    this.defaultAOL = NarUtil.getAOL(project, architecture, os, linker, null, log);
    this.linkerName = NarUtil.getLinkerName(project, architecture, os, linker, log);
  }

  public final void downloadAttachedNars(final List/* <NarArtifacts> */narArtifacts, final List remoteRepositories,
      final ArtifactResolver resolver, final String classifier) throws MojoExecutionException, MojoFailureException {
    // FIXME this may not be the right way to do this.... -U ignored and
    // also SNAPSHOT not used
    final List dependencies = getAttachedNarDependencies(narArtifacts, classifier);

    this.log.debug("Download called with classifier: " + classifier + " for NarDependencies {");
    for (final Object dependency2 : dependencies) {
      this.log.debug("  - " + dependency2);
    }
    this.log.debug("}");

    for (final Object dependency1 : dependencies) {
      final Artifact dependency = (Artifact) dependency1;
      try {
        this.log.debug("Resolving " + dependency);
        resolver.resolve(dependency, remoteRepositories, this.repository);
      } catch (final ArtifactNotFoundException e) {
        final String message = "nar not found " + dependency.getId();
        throw new MojoExecutionException(message, e);
      } catch (final ArtifactResolutionException e) {
        final String message = "nar cannot resolve " + dependency.getId();
        throw new MojoExecutionException(message, e);
      }
    }
  }

  private List/* <AttachedNarArtifact> */getAttachedNarDependencies(final Artifact dependency, final AOL archOsLinker,
      final String type) throws MojoExecutionException, MojoFailureException {
    AOL aol = archOsLinker;
    this.log.debug("GetNarDependencies for " + dependency + ", aol: " + aol + ", type: " + type);
    final List artifactList = new ArrayList();
    final NarInfo narInfo = getNarInfo(dependency);
    final String[] nars = narInfo.getAttachedNars(aol, type);
    // FIXME Move this to NarInfo....
    if (nars != null) {
      for (final String nar2 : nars) {
        this.log.debug("    Checking: " + nar2);
        if (nar2.equals("")) {
          continue;
        }
        final String[] nar = nar2.split(":", 5);
        if (nar.length >= 4) {
          try {
            final String groupId = nar[0].trim();
            final String artifactId = nar[1].trim();
            final String ext = nar[2].trim();
            String classifier = nar[3].trim();
            // translate for instance g++ to gcc...
            aol = narInfo.getAOL(aol);
            if (aol != null) {
              classifier = NarUtil.replace("${aol}", aol.toString(), classifier);
            }
            final String version = nar.length >= 5 ? nar[4].trim() : dependency.getBaseVersion();
            artifactList.add(new AttachedNarArtifact(groupId, artifactId, version, dependency.getScope(), ext,
                classifier, dependency.isOptional(), dependency.getFile()));
          } catch (final InvalidVersionSpecificationException e) {
            throw new MojoExecutionException("Error while reading nar file for dependency " + dependency, e);
          }
        } else {
          this.log.warn("nars property in " + dependency.getArtifactId() + " contains invalid field: '" + nar2
              + "' for type: " + type);
        }
      }
    }
    return artifactList;
  }

  public final List/* <AttachedNarArtifact> */getAttachedNarDependencies(final List/*
                                                                                    * <
                                                                                    * NarArtifacts
                                                                                    * >
                                                                                    */narArtifacts)
      throws MojoExecutionException, MojoFailureException {
    return getAttachedNarDependencies(narArtifacts, (String) null);
  }

  /**
   * Returns a list of all attached nar dependencies for a specific binding and
   * "noarch", but not where "local" is
   * specified
   * 
   * @param scope
   *          compile, test, runtime, ....
   * @param aol
   *          either a valid aol, noarch or null. In case of null both the
   *          default getAOL() and noarch dependencies
   *          are returned.
   * @param type
   *          noarch, static, shared, jni, or null. In case of null the default
   *          binding found in narInfo is used.
   * @return
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  public final List/* <AttachedNarArtifact> */getAttachedNarDependencies(final List/*
                                                                                    * <
                                                                                    * NarArtifacts
                                                                                    * >
                                                                                    */narArtifacts,
      final AOL archOsLinker, final String type) throws MojoExecutionException, MojoFailureException {
    boolean noarch = false;
    AOL aol = archOsLinker;
    if (aol == null) {
      noarch = true;
      aol = this.defaultAOL;
    }

    final List artifactList = new ArrayList();
    for (final Object narArtifact : narArtifacts) {
      final Artifact dependency = (Artifact) narArtifact;
      final NarInfo narInfo = getNarInfo(dependency);
      if (noarch) {
        artifactList.addAll(getAttachedNarDependencies(dependency, null, NarConstants.NAR_NO_ARCH));
      }

      // use preferred binding, unless non existing.
      final String binding = narInfo.getBinding(aol, type != null ? type : Library.STATIC);

      // FIXME kludge, but does not work anymore since AOL is now a class
      if (aol.equals(NarConstants.NAR_NO_ARCH)) {
        // FIXME no handling of local
        artifactList.addAll(getAttachedNarDependencies(dependency, null, NarConstants.NAR_NO_ARCH));
      } else {
        artifactList.addAll(getAttachedNarDependencies(dependency, aol, binding));
      }
    }
    return artifactList;
  }

  public final List/* <AttachedNarArtifact> */getAttachedNarDependencies(final List/*
                                                                                    * <
                                                                                    * NarArtifacts
                                                                                    * >
                                                                                    */narArtifacts,
      final String classifier) throws MojoExecutionException, MojoFailureException {
    AOL aol = null;
    String type = null;
    if (classifier != null) {
      final int dash = classifier.lastIndexOf('-');
      if (dash < 0) {
        aol = new AOL(classifier);
      } else {
        aol = new AOL(classifier.substring(0, dash));
        type = classifier.substring(dash + 1);
      }
    }
    return getAttachedNarDependencies(narArtifacts, aol, type);
  }

  public final List/* <AttachedNarArtifact> */getAttachedNarDependencies(final List/*
                                                                                    * <
                                                                                    * NarArtifacts
                                                                                    * >
                                                                                    */narArtifacts,
      final String[] classifiers) throws MojoExecutionException, MojoFailureException {

    final List artifactList = new ArrayList();

    if (classifiers != null && classifiers.length > 0) {

      for (final String classifier : classifiers) {
        artifactList.addAll(getAttachedNarDependencies(narArtifacts, classifier));
      }
    } else {
      artifactList.addAll(getAttachedNarDependencies(narArtifacts, (String) null));
    }

    return artifactList;
  }

  public final File getNarFile(final Artifact dependency) throws MojoFailureException {
    // FIXME reported to maven developer list, isSnapshot changes behaviour
    // of getBaseVersion, called in pathOf.
    dependency.isSnapshot();
    return new File(this.repository.getBasedir(), NarUtil.replace("${aol}", this.defaultAOL.toString(),
        this.repository.pathOf(dependency)));
  }

  public final NarInfo getNarInfo(final Artifact dependency) throws MojoExecutionException {
    // FIXME reported to maven developer list, isSnapshot changes behaviour
    // of getBaseVersion, called in pathOf.
    dependency.isSnapshot();

    final File file = new File(this.repository.getBasedir(), this.repository.pathOf(dependency));
    if (!file.exists()) {
      return null;
    }

    JarFile jar = null;
    try {
      jar = new JarFile(file);
      final NarInfo info = new NarInfo(dependency.getGroupId(), dependency.getArtifactId(),
          dependency.getBaseVersion(), this.log);
      if (!info.exists(jar)) {
        return null;
      }
      info.read(jar);
      return info;
    } catch (final IOException e) {
      throw new MojoExecutionException("Error while reading " + file, e);
    } finally {
      if (jar != null) {
        try {
          jar.close();
        } catch (final IOException e) {
          // ignore
        }
      }
    }
  }

  public final void unpackAttachedNars(final List/* <NarArtifacts> */narArtifacts,
      final ArchiverManager archiverManager, final String classifier, final String os, final NarLayout layout,
      final File unpackDir, boolean skipRanlib) throws MojoExecutionException, MojoFailureException {
    this.log.debug("Unpack called for OS: " + os + ", classifier: " + classifier + " for NarArtifacts {");
    for (final Object narArtifact : narArtifacts) {
      this.log.debug("  - " + narArtifact);
    }
    this.log.debug("}");
    // FIXME, kludge to get to download the -noarch, based on classifier
    final List dependencies = getAttachedNarDependencies(narArtifacts, classifier);
    for (final Object dependency1 : dependencies) {
      final Artifact dependency = (Artifact) dependency1;
      this.log.debug("Unpack " + dependency + " to " + unpackDir);
      final File file = getNarFile(dependency);

      layout.unpackNar(unpackDir, archiverManager, file, os, this.linkerName, this.defaultAOL, skipRanlib);
    }
  }
}
