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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.lang.model.SourceVersion;

import org.objectweb.asm.ClassReader;

/**
 * Lazy metadata lookup, including the selected compiler's platform and MR-JAR
 * view.
 */
final class JniClassPath implements Closeable {
  private final List<File> entries;
  private final List<File> platform = new ArrayList<File>();
  private final Map<String, JniClass> classes = new HashMap<String, JniClass>();
  private final Map<String, String> sourceNames = new HashMap<String, String>();
  private final Map<String, Boolean> packageTypes = new HashMap<String, Boolean>();
  private Map<String, List<String>> hierarchyHeaders;
  private final int release;
  private final File javaHome;
  private FileSystem runtimeImage;
  private URLClassLoader runtimeProvider;
  private final List<Path> modules = new ArrayList<Path>();

  JniClassPath(List<File> entries, File javaHome, int release) {
    this.entries = entries;
    this.javaHome = javaHome;
    this.release = release;
    File modules = new File(javaHome, "jmods");
    File[] jars = modules.listFiles();
    if (jars != null) {
      Arrays.sort(jars);
      for (File jar : jars) {
        if (jar.getName().endsWith(".jmod")) {
          platform.add(jar);
        }
      }
    } else {
      platform.add(new File(javaHome, "jre/lib/rt.jar"));
      platform.add(new File(javaHome, "lib/rt.jar"));
      platform.add(new File(javaHome, "../Classes/classes.jar"));
    }
  }

  JniClass index(File file) throws IOException {
    try (InputStream in = Files.newInputStream(file.toPath())) {
      JniClass model = read(in, false);
      if (classes.put(model.name, model) != null) {
        throw new IOException("Duplicate class definition: " + model.name);
      }
      return model;
    }
  }

  JniClass resolve(String name) throws IOException {
    JniClass cached = classes.get(name);
    if (cached != null) {
      return cached;
    }
    // Platform classes cannot be replaced with application stubs.
    JniClass found = null;
    if (name.startsWith("java/")) {
      found = findPlatform(name);
    }
    if (found == null) {
      found = find(entries, name, false);
    }
    if (found == null) {
      found = findPlatform(name);
    }
    if (found == null) {
      throw new IOException("Missing class definition: " + name.replace('/', '.'));
    }
    if (!name.equals(found.name)) {
      throw new IOException("Conflicting class definition for " + name + ": " + found.name);
    }
    classes.put(name, found);
    return found;
  }

  /**
   * Probe only a possible package/type collision, without traversing its
   * dependencies.
   */
  boolean hasPackageType(String packageName, String simple) throws IOException {
    String name = packageName.isEmpty() ? simple : packageName + "/" + simple;
    Boolean present = packageTypes.get(name);
    if (present == null) {
      JniClass model = classes.get(name);
      if (model == null) {
        model = find(entries, name, false);
      }
      present = model != null && name.equals(model.name) && model.outer() == null;
      packageTypes.put(name, present);
    }
    return present;
  }

  private JniClass findPlatform(String name) throws IOException {
    JniClass found = find(platform, name, true);
    if (found != null || !new File(javaHome, "lib/modules").isFile()) {
      return found;
    }
    if (runtimeImage == null) {
      // The selected JDK's provider also works when Maven itself runs on JDK 8.
      // Never use the default jrt filesystem: it belongs to Maven's runtime JDK.
      runtimeProvider = new URLClassLoader(new URL[] {
          new File(javaHome, "lib/jrt-fs.jar").toURI().toURL()
      }, null);
      runtimeImage = FileSystems.newFileSystem(URI.create("jrt:/"),
          Collections.singletonMap("java.home", javaHome.getAbsolutePath()), runtimeProvider);
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(runtimeImage.getPath("/modules"))) {
        for (Path module : stream) {
          modules.add(module);
        }
      }
      Collections.sort(modules);
    }
    for (Path module : modules) {
      Path file = module.resolve(name + ".class");
      if (Files.isRegularFile(file)) {
        try (InputStream in = Files.newInputStream(file)) {
          return read(in, true);
        }
      }
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    try {
      if (runtimeImage != null) {
        runtimeImage.close();
      }
    } finally {
      if (runtimeProvider != null) {
        runtimeProvider.close();
      }
    }
  }

  private JniClass find(List<File> paths, String name, boolean isPlatform) throws IOException {
    return find(paths, name, isPlatform, new HashSet<File>());
  }

  private JniClass find(List<File> paths, String name, boolean isPlatform, Set<File> visited) throws IOException {
    for (File path : paths) {
      if (!visited.add(path.getCanonicalFile())) {
        continue;
      }
      if (path.isDirectory()) {
        File file = new File(path, name + ".class");
        if (file.isFile()) {
          try (InputStream in = Files.newInputStream(file.toPath())) {
            return read(in, isPlatform);
          }
        }
      } else if (path.isFile()) {
        Manifest attributes = null;
        try (ZipFile zip = new ZipFile(path)) {
          String resource = (path.getName().endsWith(".jmod") ? "classes/" : "") + name + ".class";
          ZipEntry entry = zip.getEntry(resource);
          ZipEntry manifest = zip.getEntry("META-INF/MANIFEST.MF");
          if (!isPlatform && manifest != null) {
            try (InputStream in = zip.getInputStream(manifest)) {
              attributes = new Manifest(in);
            }
            if (release >= 9 && "true".equalsIgnoreCase(attributes.getMainAttributes().getValue("Multi-Release"))) {
              for (int version = release; version >= 9; version--) {
                ZipEntry candidate = zip.getEntry("META-INF/versions/" + version + "/" + resource);
                if (candidate != null) {
                  entry = candidate;
                  break;
                }
              }
            }
          }
          if (entry != null) {
            try (InputStream in = zip.getInputStream(entry)) {
              return read(in, isPlatform);
            }
          }
        }
        // Manifest entries immediately follow their containing JAR, including
        // transitive entries. Share visited paths across the complete search.
        JniClass found = find(manifestPaths(path, attributes), name, false, visited);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private List<File> manifestPaths(File path, Manifest attributes) throws IOException {
    List<File> dependencies = new ArrayList<File>();
    String classPath = attributes == null ? null : attributes.getMainAttributes().getValue("Class-Path");
    if (classPath == null) {
      return dependencies;
    }
    StringTokenizer tokens = new StringTokenizer(classPath);
    while (tokens.hasMoreTokens()) {
      String token = tokens.nextToken();
      // JDK 8-10 javac treats manifest entries as file names, not URLs.
      if (release < 11) {
        File entry = new File(token);
        dependencies.add(release == 8 || !entry.isAbsolute() ? new File(path.getParentFile(), token) : entry);
        continue;
      }
      try {
        URI entry = new URL(path.toURI().toURL(), token).toURI();
        if (!entry.isOpaque() && "file".equalsIgnoreCase(entry.getScheme()) && entry.getAuthority() == null
            && entry.getQuery() == null && entry.getFragment() == null) {
          dependencies.add(new File(entry));
        }
      } catch (MalformedURLException | URISyntaxException ex) {
        // Invalid or remote manifest URLs are not application metadata sources.
      }
    }
    return dependencies;
  }

  /**
   * Candidate qualifiers only: full member/access checks are done by the caller.
   */
  Set<String> subtypes(String name, boolean discover) throws IOException {
    if (discover && hierarchyHeaders == null) {
      hierarchyHeaders = new HashMap<String, List<String>>();
      indexHierarchy(entries, new HashSet<File>());
    }
    Map<String, List<String>> headers = new HashMap<String, List<String>>();
    if (hierarchyHeaders != null) {
      headers.putAll(hierarchyHeaders);
    }
    // Project definitions and normally resolved classes retain lookup precedence.
    for (JniClass model : classes.values()) {
      List<String> parents = new ArrayList<String>(model.interfaces);
      if (model.parent != null) {
        parents.add(model.parent);
      }
      headers.put(model.name, parents);
    }
    Map<String, Set<String>> children = new HashMap<String, Set<String>>();
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      for (String parent : entry.getValue()) {
        if (!children.containsKey(parent)) {
          children.put(parent, new TreeSet<String>());
        }
        children.get(parent).add(entry.getKey());
      }
    }
    Set<String> result = new TreeSet<String>();
    List<String> pending = new ArrayList<String>();
    pending.add(name);
    for (int i = 0; i < pending.size(); i++) {
      Set<String> found = children.get(pending.get(i));
      if (found == null) {
        continue;
      }
      for (String child : found) {
        if (!child.equals(name) && result.add(child)) {
          pending.add(child);
        }
      }
    }
    return result;
  }

  private void indexHierarchy(List<File> paths, Set<File> visited) throws IOException {
    for (File path : paths) {
      if (!visited.add(path.getCanonicalFile())) {
        continue;
      }
      if (path.isDirectory()) {
        indexDirectory(path, path, new HashSet<File>());
      } else if (path.isFile()) {
        Manifest manifest = null;
        try (ZipFile zip = new ZipFile(path)) {
          ZipEntry attributes = zip.getEntry("META-INF/MANIFEST.MF");
          if (attributes != null) {
            try (InputStream in = zip.getInputStream(attributes)) {
              manifest = new Manifest(in);
            }
          }
          boolean multiRelease = release >= 9 && manifest != null
              && "true".equalsIgnoreCase(manifest.getMainAttributes().getValue("Multi-Release"));
          Map<String, ZipEntry> selected = new TreeMap<String, ZipEntry>();
          Map<String, Integer> versions = new HashMap<String, Integer>();
          for (Enumeration<? extends ZipEntry> all = zip.entries(); all.hasMoreElements();) {
            ZipEntry entry = all.nextElement();
            String resource = entry.getName();
            int version = 0;
            if (path.getName().endsWith(".jmod") && resource.startsWith("classes/")) {
              resource = resource.substring(8);
            }
            if (resource.startsWith("META-INF/versions/")) {
              if (!multiRelease) {
                continue;
              }
              int slash = resource.indexOf('/', 18);
              if (slash < 0) {
                continue;
              }
              try {
                version = Integer.parseInt(resource.substring(18, slash));
              } catch (NumberFormatException invalid) {
                continue;
              }
              if (version < 9 || version > release) {
                continue;
              }
              resource = resource.substring(slash + 1);
            }
            if (!resource.endsWith(".class") || resource.startsWith("META-INF/")) {
              continue;
            }
            String name = resource.substring(0, resource.length() - 6);
            if (!versions.containsKey(name) || versions.get(name) < version) {
              versions.put(name, version);
              selected.put(name, entry);
            }
          }
          for (Map.Entry<String, ZipEntry> entry : selected.entrySet()) {
            if (hierarchyHeaders.containsKey(entry.getKey())) {
              continue;
            }
            try (InputStream in = zip.getInputStream(entry.getValue())) {
              indexHeader(entry.getKey(), in);
            }
          }
        }
        indexHierarchy(manifestPaths(path, manifest), visited);
      }
    }
  }

  private void indexDirectory(File root, File directory, Set<File> visiting) throws IOException {
    File canonical = directory.getCanonicalFile();
    if (!visiting.add(canonical)) {
      return;
    }
    try {
      File[] files = directory.listFiles();
      if (files == null) {
        return;
      }
      Arrays.sort(files);
      for (File file : files) {
        if (file.isDirectory()) {
          indexDirectory(root, file, visiting);
        } else if (file.getName().endsWith(".class")) {
          String resource = root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
          String name = resource.substring(0, resource.length() - 6);
          if (hierarchyHeaders.containsKey(name)) {
            continue;
          }
          try (InputStream in = Files.newInputStream(file.toPath())) {
            indexHeader(name, in);
          }
        }
      }
    } finally {
      visiting.remove(canonical);
    }
  }

  private void indexHeader(String name, InputStream in) throws IOException {
    // This fallback reads only hierarchy headers. It does not resolve methods,
    // fields, generic signatures, or any dependencies of unrelated classes.
    List<String> parents = new ArrayList<String>();
    hierarchyHeaders.put(name, parents);
    try {
      ClassReader reader = new ClassReader(in);
      if (!name.equals(reader.getClassName())) {
        return;
      }
      if (reader.getSuperName() != null) {
        parents.add(reader.getSuperName());
      }
      Collections.addAll(parents, reader.getInterfaces());
    } catch (IllegalArgumentException invalid) {
      // Unrelated unsupported class versions are not required declarations.
    }
  }

  private JniClass read(InputStream in, boolean platformClass) throws IOException {
    JniClass model = new JniClass();
    try {
      new ClassReader(in).accept(model, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    } catch (IllegalArgumentException ex) {
      throw new IOException("Cannot inspect JNI class metadata: " + ex.getMessage(), ex);
    }
    model.platform = platformClass;
    return model;
  }

  String sourceName(String name) throws IOException {
    return sourceName(name, new HashSet<String>());
  }

  private String sourceName(String name, Set<String> visiting) throws IOException {
    String cached = sourceNames.get(name);
    if (cached != null) {
      return cached;
    }
    if (!visiting.add(name)) {
      throw new IOException("Containment cycle involving " + name);
    }
    JniClass model = resolve(name);
    if (model.local || (model.nesting != null && (model.outer() == null || model.simple() == null))) {
      throw new IOException("Unsupported JNI target or required declaration: local/anonymous class " + name);
    }
    identifier(model.simple(), name);
    String simple = model.simple();
    if ((release >= 10 && "var".equals(simple)) || (release >= 14 && "yield".equals(simple))
        || (release >= 16 && "record".equals(simple))
        || (release >= 17 && ("sealed".equals(simple) || "permits".equals(simple)))) {
      throw new IOException("Source-inexpressible type name '" + simple + "' for JDK " + release + " in " + name);
    }
    String result;
    if (model.outer() != null) {
      JniClass parent = resolve(model.outer());
      String parentSource = sourceName(parent.name, visiting);
      JniClass.Member counterpart = parent.members.get(name);
      if (!model.packageName().equals(parent.packageName()) || !name.equals(parent.name + "$" + model.simple())
          || (counterpart != null && !counterpart.same(model.nesting))) {
        throw new IOException("Conflicting containment metadata for " + name);
      }
      result = parentSource + "." + model.simple();
    } else {
      for (String part : model.packageName().split("/")) {
        if (!part.isEmpty()) {
          identifier(part, name);
        }
      }
      result = name.replace('/', '.');
    }
    visiting.remove(name);
    sourceNames.put(name, result);
    return result;
  }

  void identifier(String identifier, String owner) throws IOException {
    boolean keyword = "_".equals(identifier) ? release >= 9 : identifier != null && SourceVersion.isKeyword(identifier);
    if (identifier == null || !SourceVersion.isIdentifier(identifier) || keyword) {
      throw new IOException("Source-inexpressible name '" + identifier + "' in " + owner);
    }
  }
}
