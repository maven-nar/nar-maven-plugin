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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.Version;

/** Resolves NAR attachments using the repository session supplied by Maven. */
public final class NarArtifactResolver {
  static String fileName(final Artifact artifact) {
    final String classifier = artifact.getClassifier();
    return artifact.getArtifactId() + "-" + artifact.getBaseVersion()
        + (classifier == null || classifier.isEmpty() ? "" : "-" + classifier) + "."
        + artifact.getArtifactHandler().getExtension();
  }

  static NarInfo readNarInfo(final Artifact dependency, final Log log) throws MojoExecutionException {
    final File file = dependency.getFile();
    if (file == null || !file.exists()) {
      throw new MojoExecutionException("NAR dependency has no resolved file: " + dependency.getId());
    }
    if (file.isDirectory()) {
      log.debug("Dependency is not packaged: " + dependency.getFile());

      return new NarInfo(dependency.getGroupId(), dependency.getArtifactId(), dependency.getBaseVersion(), log,
          dependency.getFile());
    }

    ZipInputStream zipStream = null;
    try {
      zipStream = new ZipInputStream(new FileInputStream(file));
      if (zipStream.getNextEntry() == null) {
        log.debug("Skipping unreadable artifact: " + file);
        return null;
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Error while testing for zip file " + file, e);
    } finally {
      IOUtils.closeQuietly(zipStream);
    }

    JarFile jar = null;
    try {
      jar = new JarFile(file);
      final NarInfo info = new NarInfo(dependency.getGroupId(), dependency.getArtifactId(), dependency.getBaseVersion(),
          log);
      if (!info.exists(jar)) {
        log.debug("Dependency nar file does not contain this artifact: " + file);
        return null;
      }
      info.read(jar);
      return info;
    } catch (final IOException e) {
      throw new MojoExecutionException("Error while reading " + file, e);
    } finally {
      IOUtils.closeQuietly(jar);
    }
  }

  private final RepositorySystem system;

  private final RepositorySystemSession session;

  private final List<RemoteRepository> repositories;

  public NarArtifactResolver(final RepositorySystem system, final RepositorySystemSession session,
      final List<RemoteRepository> repositories) {
    this.system = system;
    this.session = session;
    this.repositories = repositories;
  }

  public File resolve(final Artifact artifact) throws MojoExecutionException {
    final boolean range = artifact.getVersion() == null;
    final String version = range ? artifact.getVersionRange().toString() : artifact.getVersion();
    org.eclipse.aether.artifact.Artifact requestArtifact = new org.eclipse.aether.artifact.DefaultArtifact(
        artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
        artifact.getArtifactHandler().getExtension(), version);
    try {
      if (range) {
        final Version selected = system
            .resolveVersionRange(session, new VersionRangeRequest(requestArtifact, repositories, null))
            .getHighestVersion();
        if (selected == null) {
          throw new MojoExecutionException("nar not found " + artifact.getId());
        }
        requestArtifact = requestArtifact.setVersion(selected.toString());
        artifact.selectVersion(selected.toString());
      }
      // Always ask Resolver: file existence alone does not establish repository
      // provenance, snapshot freshness, or availability in the reactor workspace.
      final ArtifactResult result = system.resolveArtifact(session,
          new ArtifactRequest(requestArtifact, repositories, null));
      final File file = result.getArtifact().getFile();
      if (file == null || !file.isFile()) {
        throw new MojoExecutionException("nar cannot resolve " + artifact.getId() + ": no archive file");
      }
      artifact.setFile(file);
      artifact.setResolved(true);
      // Keep the logical base version. Resolver's timestamped version is only
      // needed to locate the archive, not to name NAR's extraction directories.
      return file;
    } catch (final ArtifactResolutionException e) {
      throw new MojoExecutionException(
          (e.getResult().isMissing() ? "nar not found " : "nar cannot resolve ") + artifact.getId(), e);
    } catch (final VersionRangeResolutionException e) {
      throw new MojoExecutionException("nar cannot resolve " + artifact.getId(), e);
    }
  }
}
