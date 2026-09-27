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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/** NAR-specific classpath and native-loader environment preparation. */
final class NarIntegrationTestSupport {
  private final NarIntegrationTestMojo mojo;
  private final List<NarArtifact> dependencies;
  private final boolean ownLibrary;

  NarIntegrationTestSupport(NarIntegrationTestMojo mojo) throws MojoExecutionException {
    this.mojo = mojo;
    this.dependencies = mojo.getNarArtifacts();
    boolean own = false;
    for (Library library : mojo.getLibraries()) {
      own |= Library.JNI.equals(library.getType()) || Library.SHARED.equals(library.getType());
    }
    this.ownLibrary = own;
  }

  boolean requiresIsolation() {
    return "nar".equals(mojo.getMavenProject().getPackaging()) || !dependencies.isEmpty();
  }

  void addProjectArtifact(List<String> classpath) {
    if (ownLibrary) {
      MavenProject project = mojo.getMavenProject();
      File artifact = project.getArtifact().getFile();
      if (artifact == null) {
        artifact = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".jar");
      }
      classpath.add(artifact.getAbsolutePath());
    }
  }

  Map<String, String> environment(Map<String, String> configured, Map<String, String> inherited)
      throws MojoExecutionException, MojoFailureException {
    List<String> paths = new ArrayList<>();
    MavenProject project = mojo.getMavenProject();
    NarLayout layout = mojo.getLayout();
    String aol = mojo.getAOL().toString();
    if (ownLibrary) {
      for (String binding : new String[] {Library.JNI, Library.SHARED}) {
        addExisting(paths, layout.getLibDirectory(mojo.getTargetDirectory(), project.getArtifactId(),
            project.getVersion(), aol, binding));
      }
    }
    for (NarArtifact dependency : dependencies) {
      for (String binding : new String[] {Library.SHARED, Library.JNI}) {
        addExisting(paths, layout.getLibDirectory(mojo.getUnpackDirectory(), dependency.getArtifactId(),
            dependency.getBaseVersion(), aol, binding));
      }
    }
    return loaderEnvironment(mojo.getOS(), paths, configured, inherited, System.getProperties());
  }

  private static void addExisting(List<String> paths, File directory) {
    if (directory.exists()) {
      paths.add(directory.getAbsolutePath());
    }
  }

  static Map<String, String> loaderEnvironment(String os, List<String> paths,
      Map<String, String> configured, Map<String, String> inherited, Properties systemProperties) {
    boolean windows = OS.WINDOWS.equals(os);
    Map<String, String> result = windows ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER) : new LinkedHashMap<>();
    if (configured != null) {
      result.putAll(configured);
    }
    String name = windows ? "PATH" : OS.MACOSX.equals(os) ? "DYLD_LIBRARY_PATH"
        : OS.AIX.equals(os) ? "LIBPATH" : "LD_LIBRARY_PATH";
    String key = key(result, name, windows);
    if (!paths.isEmpty()) {
      String tail = result.get(key);
      if (tail == null) {
        tail = inherited.get(key(inherited, name, windows));
        if (tail == null) {
          tail = systemProperties.getProperty(name);
        }
      }
      String separator = windows ? ";" : ":";
      String value = String.join(separator, paths);
      result.put(key, tail == null || tail.isEmpty() ? value : value + separator + tail);
    }
    if (windows && !result.containsKey(key(result, "SystemRoot", true))) {
      String systemRoot = inherited.get(key(inherited, "SystemRoot", true));
      if (systemRoot == null) {
        systemRoot = systemProperties.getProperty("SystemRoot");
      }
      result.put("SystemRoot", systemRoot == null ? "C:\\Windows" : systemRoot);
    }
    return result;
  }

  private static String key(Map<String, String> values, String name, boolean ignoreCase) {
    if (ignoreCase) {
      for (String candidate : values.keySet()) {
        if (name.equalsIgnoreCase(candidate)) {
          return candidate;
        }
      }
    }
    return name;
  }
}
