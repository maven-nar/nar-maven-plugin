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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class NarResolverCompatibilityTest {
  @Rule
  public TemporaryFolder temporary = new TemporaryFolder();

  private Artifact artifact(final File file) {
    final Artifact artifact = new DefaultArtifact("example", "native-lib", "1.0-SNAPSHOT", Artifact.SCOPE_COMPILE,
        "nar", null, new DefaultArtifactHandler("nar"));
    artifact.setFile(file);
    artifact.setResolved(true);
    return artifact;
  }

  @Test
  public void attachmentCoordinatesDoNotInventARepositoryLocation() throws Exception {
    final AttachedNarArtifact attachment = new AttachedNarArtifact("example", "native-lib", "1.0-SNAPSHOT",
        Artifact.SCOPE_COMPILE, "nar", "noarch", false, temporary.newFile("parent.nar"));
    assertNull("Only artifact resolution can determine the attachment file", attachment.getFile());
    assertEquals("1.0-SNAPSHOT", attachment.getBaseVersion());
    assertEquals("noarch", attachment.getClassifier());
    assertEquals("nar", attachment.getArtifactHandler().getExtension());
  }

  @Test
  public void readsMetadataFromTheResolvedArchiveOutsideTheRepository() throws Exception {
    final File archive = temporary.newFile("workspace artifact.nar");
    try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(archive.toPath()))) {
      jar.putNextEntry(new JarEntry("META-INF/nar/example/native-lib/nar.properties"));
      jar.write("nar.noarch=example:native-lib:nar:noarch\n".getBytes(StandardCharsets.UTF_8));
      jar.closeEntry();
    }
    final Artifact artifact = artifact(archive);
    final NarInfo info = new NarUnpackDependenciesMojo().getNarInfo(artifact);
    assertNotNull(info);
    assertArrayEquals(new String[] {
        "example:native-lib:nar:noarch"
    }, info.getAttachedNars(null, "noarch"));
    assertEquals("1.0-SNAPSHOT", artifact.getBaseVersion());
  }

  @Test
  public void resolvesVersionRangesBeforeLocatingTheAttachment() throws Exception {
    final File resolved = temporary.newFile("workspace-attachment.nar");
    final List<String> calls = new java.util.ArrayList<>();
    final RepositorySystem system = system((proxy, method, args) -> {
      calls.add(method.getName());
      if ("resolveVersionRange".equals(method.getName())) {
        final VersionRangeRequest request = (VersionRangeRequest) args[1];
        assertEquals("[1.0,2.0)", request.getArtifact().getVersion());
        return new VersionRangeResult(request).addVersion(new GenericVersionScheme().parseVersion("1.5"));
      }
      assertEquals("resolveArtifact", method.getName());
      final ArtifactRequest request = (ArtifactRequest) args[1];
      assertEquals("1.5", request.getArtifact().getVersion());
      return new ArtifactResult(request).setArtifact(request.getArtifact().setFile(resolved));
    });
    final AttachedNarArtifact artifact = new AttachedNarArtifact("example", "native-lib", "[1.0,2.0)",
        Artifact.SCOPE_COMPILE, "nar", "noarch", false);
    assertEquals(resolved,
        new NarArtifactResolver(system, new DefaultRepositorySystemSession(), Collections.emptyList())
            .resolve(artifact));
    assertEquals(Arrays.asList("resolveVersionRange", "resolveArtifact"), calls);
    assertEquals("native-lib-1.5-noarch.nar", NarArtifactResolver.fileName(artifact));
  }

  @Test
  public void retainsMetadataFromReactorClassesDirectories() throws Exception {
    final File classes = temporary.newFolder("reactor classes");
    final File metadata = new File(classes, "META-INF/nar/example/native-lib/nar.properties");
    assertTrue(metadata.getParentFile().mkdirs());
    Files.write(metadata.toPath(), "nar.noarch=example:native-lib:nar:noarch\n".getBytes(StandardCharsets.UTF_8));
    final NarInfo info = new NarUnpackDependenciesMojo().getNarInfo(artifact(classes));
    assertArrayEquals(new String[] {
        "example:native-lib:nar:noarch"
    }, info.getAttachedNars(null, "noarch"));
  }

  private RepositorySystem system(final InvocationHandler handler) {
    return (RepositorySystem) Proxy.newProxyInstance(RepositorySystem.class.getClassLoader(), new Class<?>[] {
        RepositorySystem.class
    }, handler);
  }

  @Test
  public void unpacksTheResolvedArchiveUnderItsLogicalBaseVersion() throws Exception {
    final File resolved = temporary.newFile("native-lib-1.0-20260927.120000-1-noarch.nar");
    final File output = temporary.newFolder("unpacked");
    final NarLayout21 layout = new NarLayout21(new SystemStreamLog()) {
      @Override
      protected void unpackNarAndProcess(final ArchiverManager manager, final File archive, final File directory,
          final String os, final String linker, final AOL aol, final boolean skipRanlib) {
        assertEquals(resolved, archive);
        assertEquals(new File(output, "native-lib-1.0-SNAPSHOT-noarch"), directory);
        assertTrue(directory.mkdir());
      }
    };
    layout.unpackNar(output, null, resolved, "Linux", "g++", new AOL("amd64-Linux-gpp"), true,
        "native-lib-1.0-SNAPSHOT-noarch.nar");
    assertTrue(new File(output, "native-lib-1.0-SNAPSHOT-noarch").isDirectory());
  }

  @Test
  public void usesTheHostSessionAndReturnedFileWithoutChangingSnapshotCoordinates() throws Exception {
    final File stale = temporary.newFile("stale.nar");
    final File resolved = temporary.newFile("native-lib-1.0-20260927.120000-1-noarch.nar");
    final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
    final List<RemoteRepository> repositories = Collections
        .singletonList(new RemoteRepository.Builder("fixture", "default", "file:/fixture").build());
    final RepositorySystem system = system((proxy, method, args) -> {
      assertEquals("resolveArtifact", method.getName());
      assertSame(session, args[0]);
      final ArtifactRequest request = (ArtifactRequest) args[1];
      assertEquals(repositories, request.getRepositories());
      assertEquals("example:native-lib:nar:noarch:1.0-SNAPSHOT", request.getArtifact().toString());
      return new ArtifactResult(request)
          .setArtifact(request.getArtifact().setVersion("1.0-20260927.120000-1").setFile(resolved));
    });
    final AttachedNarArtifact artifact = new AttachedNarArtifact("example", "native-lib", "1.0-SNAPSHOT",
        Artifact.SCOPE_COMPILE, "nar", "noarch", false);
    artifact.setFile(stale);
    artifact.setResolved(true);
    assertEquals(resolved, new NarArtifactResolver(system, session, repositories).resolve(artifact));
    assertEquals(resolved, artifact.getFile());
    assertEquals("1.0-SNAPSHOT", artifact.getVersion());
    assertEquals("native-lib-1.0-SNAPSHOT-noarch.nar", NarArtifactResolver.fileName(artifact));
  }
}
