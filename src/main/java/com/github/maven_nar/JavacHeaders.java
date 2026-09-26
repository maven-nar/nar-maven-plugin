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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Rebuilds only JNI declarations; javac remains responsible for the JNI ABI.
 */
final class JavacHeaders {
  private final File javac;
  private final File work;
  private final Log log;
  private final List<File> classPath;
  private final List<File> bootClassPath;
  private final JniClassPath metadata;
  private final Set<String> targets = new TreeSet<String>();
  private final Map<String, JniClass> declarations = new TreeMap<String, JniClass>();
  private final Set<String> genericDeclarations = new HashSet<String>();
  private final Map<String, Map<String, String>> classVariables = new HashMap<String, Map<String, String>>();
  private final Map<String, Constructor> constructors = new HashMap<String, Constructor>();
  private final Map<String, List<JniClass.Method>> interfaceMethods = new HashMap<String, List<JniClass.Method>>();
  private final Map<String, JniSignature> signatures = new HashMap<String, JniSignature>();
  private final Map<String, JniSourceNames> sourceUnits = new HashMap<String, JniSourceNames>();
  private JniSourceNames sourceNames;
  private boolean planningDeclarations;
  private Map<String, String> sourceVariables = Collections.emptyMap();
  private JniSignature sourceFormals = JniSignature.read(null);
  private final Set<String> references = new HashSet<String>();

  JavacHeaders(File javac, File work, List<File> classPath, List<File> bootClassPath, Log log) throws IOException {
    this.javac = javac;
    this.work = work;
    this.log = log;
    this.classPath = classPath;
    this.bootClassPath = bootClassPath;
    String version = run(Arrays.asList(javac.getAbsolutePath(), "-version"));
    Matcher matcher = Pattern.compile("javac (?:1\\.)?(\\d+)").matcher(version);
    if (!matcher.find()) {
      throw new IOException("Cannot determine compiler JDK version: " + version);
    }
    int release = Integer.parseInt(matcher.group(1));
    if (release < 8) {
      throw new IOException("javac header generation requires JDK 8 or newer: " + version);
    }
    File javaHome = javac.getCanonicalFile().getParentFile().getParentFile();
    List<File> paths = new ArrayList<File>(bootClassPath);
    paths.addAll(classPath);
    metadata = new JniClassPath(paths, javaHome, release);
  }

  /**
   * Discover targets without a compiler or platform metadata, including
   * dependency-only requests.
   */
  static boolean hasTargets(File classes, Set<?> includes, Set<?> excludes, Set<?> extraClasses) throws IOException {
    if (extraClasses != null) {
      for (Object name : extraClasses) {
        if (name != null) {
          return true;
        }
      }
    }
    if (!classes.isDirectory()) {
      return false;
    }
    DirectoryScanner selected = new DirectoryScanner();
    selected.setBasedir(classes);
    selected.setIncludes(strings(includes));
    selected.setExcludes(strings(excludes));
    selected.scan();
    for (String path : selected.getIncludedFiles()) {
      if (!path.endsWith(".class")) {
        continue;
      }
      try (InputStream in = Files.newInputStream(new File(classes, path).toPath())) {
        JniClass model = new JniClass();
        new org.objectweb.asm.ClassReader(in).accept(model, org.objectweb.asm.ClassReader.SKIP_CODE
            | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
        if (!model.natives.isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  void generate(File classes, Set<?> includes, Set<?> excludes, Set<?> extraClasses, File output) throws IOException {
    try (JniClassPath ignored = metadata) {
      generateHeaders(classes, includes, excludes, extraClasses, output);
    }
  }

  private void generateHeaders(File classes, Set<?> includes, Set<?> excludes, Set<?> extraClasses, File output)
      throws IOException {
    DirectoryScanner index = new DirectoryScanner();
    index.setBasedir(classes);
    index.setIncludes(new String[] {
        "**/*.class"
    });
    index.scan();
    Map<String, JniClass> local = new HashMap<String, JniClass>();
    for (String path : index.getIncludedFiles()) {
      local.put(path, metadata.index(new File(classes, path)));
    }
    DirectoryScanner selected = new DirectoryScanner();
    selected.setBasedir(classes);
    selected.setIncludes(strings(includes));
    selected.setExcludes(strings(excludes));
    selected.scan();
    for (String path : selected.getIncludedFiles()) {
      JniClass model = local.get(path);
      if (model != null && !model.natives.isEmpty()) {
        targets.add(model.name);
      }
    }
    if (extraClasses != null) {
      for (Object name : extraClasses) {
        if (name != null) {
          targets.add(name.toString().replace('.', '/'));
        }
      }
    }
    if (targets.isEmpty()) {
      return;
    }
    Set<String> filenames = new HashSet<String>();
    for (String name : targets) {
      JniClass model = metadata.resolve(name);
      if (model.platform) {
        throw new IOException("Unsupported JNI header target from the JDK/platform: " + name);
      }
      // Case folding also protects builds on case-insensitive filesystems.
      if (!filenames.add(header(name).toLowerCase(Locale.ROOT))) {
        throw new IOException("JNI header filename collision for " + name + ": " + header(name));
      }
      declare(model);
    }
    prepare();

    File sources = new File(work, "sources");
    File compiled = new File(work, "classes");
    File staged = new File(work, "headers");
    compileHeaders(sources, compiled, staged);
    for (String name : targets) {
      verifyNativeMethods(metadata.resolve(name), new File(compiled, name + ".class"));
      if (!new File(staged, header(name)).isFile()) {
        throw new IOException("javac did not generate the requested JNI header " + header(name) + " for " + name);
      }
    }
    // Support declarations must never cause an unrequested header to be published.
    Files.createDirectories(output.toPath());
    for (String name : targets) {
      Files.copy(new File(staged, header(name)).toPath(), new File(output, header(name)).toPath(),
          StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Check what javac actually bound before any staged header can replace a
   * published one.
   */
  static void verifyNativeMethods(JniClass original, File compiled) throws IOException {
    JniClass generated = new JniClass();
    try (InputStream in = Files.newInputStream(compiled.toPath())) {
      new org.objectweb.asm.ClassReader(in).accept(generated, org.objectweb.asm.ClassReader.SKIP_CODE
          | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
    }
    Set<String> expected = nativeMethods(original);
    Set<String> actual = nativeMethods(generated);
    if (!original.name.equals(generated.name) || !expected.equals(actual)) {
      throw new IOException("Native method ABI changed while generating " + original.name + ": expected " + expected
          + ", compiled " + generated.name + " " + actual);
    }
  }

  private static Set<String> nativeMethods(JniClass model) {
    Set<String> methods = new TreeSet<String>();
    for (JniClass.Method method : model.natives) {
      methods
          .add(((method.access & Opcodes.ACC_STATIC) == 0 ? "instance " : "static ") + method.name + method.descriptor);
    }
    return methods;
  }

  private void compileHeaders(File sources, File compiled, File staged) throws IOException {
    FileUtils.deleteDirectory(work);
    for (File directory : Arrays.asList(sources, compiled, staged)) {
      Files.createDirectories(directory.toPath());
    }
    File arguments = writeSources(sources, compiled, staged);
    log.info("Generating JNI headers with " + javac + " for " + targets.size() + " classes");
    String diagnostics;
    try {
      diagnostics = run(
          Arrays.asList(javac.getAbsolutePath(), "-J-Dfile.encoding=UTF-8", "@" + arguments.getAbsolutePath()));
    } catch (IOException ex) {
      Files.write(new File(work, "javac.log").toPath(), ex.getMessage().getBytes(StandardCharsets.UTF_8));
      throw ex;
    }
    Files.write(new File(work, "javac.log").toPath(), diagnostics.getBytes(StandardCharsets.UTF_8));
    if (!diagnostics.trim().isEmpty()) {
      log.info(diagnostics);
    }
  }

  private File writeSources(File sources, File compiled, File staged) throws IOException {
    List<String> args = new ArrayList<String>();
    Collections.addAll(args, "-encoding", "UTF-8", "-proc:none", "-implicit:none", "-sourcepath",
        sources.getAbsolutePath(), "-classpath", path(classPath), "-d", compiled.getAbsolutePath(), "-h",
        staged.getAbsolutePath());
    if (!bootClassPath.isEmpty()) {
      Collections.addAll(args, "-bootclasspath", path(bootClassPath));
    }
    for (JniClass model : declarations.values()) {
      if (model.outer() != null) {
        continue;
      }
      StringBuilder source = new StringBuilder();
      if (!model.packageName().isEmpty()) {
        source.append("package ").append(model.packageName().replace('/', '.')).append(";\n");
      }
      File file = new File(sources, model.name + ".java").getCanonicalFile();
      StringBuilder body = new StringBuilder();
      emit(model, body, "");
      for (String imported : names(model).imports()) {
        source.append("import ").append(imported).append(";\n");
      }
      source.append(body);
      Files.createDirectories(file.getParentFile().toPath());
      Files.write(file.toPath(), source.toString().getBytes(StandardCharsets.UTF_8));
      args.add(file.getAbsolutePath());
    }
    StringBuilder argumentFile = new StringBuilder();
    for (String arg : args) {
      argumentFile.append('"').append(arg.replace("\\", "\\\\").replace("\"", "\\\"")).append('"').append('\n');
    }
    File arguments = new File(work, "javac.args");
    Files.write(arguments.toPath(), argumentFile.toString().getBytes(StandardCharsets.UTF_8));
    return arguments;
  }

  private void declare(JniClass model) throws IOException {
    metadata.sourceName(model.name); // validates the entire containment chain
    if (declarations.put(model.name, model) == null && model.outer() != null) {
      declare(metadata.resolve(model.outer()));
    }
  }

  private void prepare() throws IOException {
    int previous;
    do {
      previous = declarations.size() + genericDeclarations.size();
      sourceUnits.clear();
      classVariables.clear();
      sourceVariables = Collections.emptyMap();
      constructors.clear();
      interfaceMethods.clear();
      for (JniClass model : new ArrayList<JniClass>(declarations.values())) {
        for (String iface : model.interfaces) {
          reference(Type.getObjectType(iface));
        }
        for (JniSignature.Value iface : emittedInterfaces(declarationView(model))) {
          reference(iface);
        }
        if (genericDeclaration(model)) {
          for (Map.Entry<String, List<JniSignature.Value>> formal : signature(model).bounds.entrySet()) {
            metadata.identifier(formal.getKey(), model.name);
            for (JniSignature.Value bound : formal.getValue()) {
              reference(sourceType(bound));
            }
          }
        }
        for (JniClass.Field field : model.constants) {
          metadata.identifier(field.name, model.name);
        }
        if (!model.isInterface() && !model.isEnum() && !model.isRecord()) {
          hierarchy(model.name, new HashSet<String>());
          if (model.parent != null) {
            reference(emittedSuperclass(declarationView(model)));
          }
        }
        if (targets.contains(model.name)) {
          for (JniClass.Method method : model.natives) {
            metadata.identifier(method.name, model.name);
            reference(Type.getReturnType(method.descriptor));
            for (Type arg : Type.getArgumentTypes(method.descriptor)) {
              reference(arg);
            }
            if (genericDeclaration(model) && method.signature != null) {
              referenceSignature(sourceSignature(JniSignature.read(method.signature)));
            }
          }
        }
      }
      declareReferencedMembers();
      if (declarations.size() + genericDeclarations.size() != previous) {
        continue;
      }
      // Establish which interfaces are source stubs before resolving method
      // contracts: methods omitted from those interfaces need no implementation.
      for (JniClass model : new ArrayList<JniClass>(declarations.values())) {
        if (!model.isInterface() || !model.interfaces.isEmpty()) {
          List<JniClass.Method> methods = interfaceMethods(model);
          interfaceMethods.put(model.name, methods);
          for (JniClass.Method method : methods) {
            metadata.identifier(method.name, model.name);
            reference(Type.getReturnType(method.descriptor));
            for (Type arg : Type.getArgumentTypes(method.descriptor)) {
              reference(arg);
            }
            if (method.signature != null) {
              JniSignature source = JniSignature.read(method.signature);
              for (Map.Entry<String, List<JniSignature.Value>> formal : source.bounds.entrySet()) {
                metadata.identifier(formal.getKey(), model.name + "." + method.name);
                for (JniSignature.Value bound : formal.getValue()) {
                  reference(sourceType(bound));
                }
              }
              reference(sourceType(source.result));
              for (JniSignature.Value arg : source.parameters) {
                reference(sourceType(arg));
              }
            }
          }
        }
      }
      declareReferencedMembers();
      if (declarations.size() + genericDeclarations.size() != previous) {
        continue;
      }
      // Reserve mandatory declarations before choosing synthetic constructors.
      // Candidates may need imports or a different checked-exception spelling;
      // neither may hide a native signature or an inherited method contract.
      planningDeclarations = true;
      try {
        for (JniClass model : declarations.values()) {
          if (model.outer() == null) {
            emit(model, new StringBuilder(), "");
          }
        }
        for (JniSourceNames names : sourceUnits.values()) {
          names.finishCollecting();
        }
        for (JniClass model : declarations.values()) {
          if (model.outer() == null) {
            emit(model, new StringBuilder(), "");
          }
        }
      } finally {
        planningDeclarations = false;
      }
      declareSourceNames();
      if (declarations.size() + genericDeclarations.size() != previous) {
        continue;
      }
      for (JniClass model : new ArrayList<JniClass>(declarations.values())) {
        planConstructor(model);
      }
      declareSourceNames();
    } while (declarations.size() + genericDeclarations.size() != previous);
  }

  private void planConstructor(JniClass model) throws IOException {
    if (model.isInterface() || model.isEnum() || model.isRecord() || constructors.containsKey(model.name)) {
      return;
    }
    // A source superclass has a synthetic no-argument constructor. Plan its
    // actual throws clause first, including any widening needed in its unit.
    if (model.parent != null && declarations.containsKey(model.parent)) {
      planConstructor(declarations.get(model.parent));
    }
    if (!constructors.containsKey(model.name)) {
      constructors.put(model.name, constructor(model));
    }
  }

  private void declareSourceNames() throws IOException {
    for (JniSourceNames names : sourceUnits.values()) {
      genericDeclarations.addAll(names.genericDeclarations());
      for (String name : names.references()) {
        reference(Type.getObjectType(name));
      }
    }
    declareReferencedMembers();
  }

  private void declareReferencedMembers() throws IOException {
    // A source outer hides its classpath members. Retain only members actually
    // referenced by a signature, superclass, interface or selected constructor.
    for (String name : new ArrayList<String>(references)) {
      JniClass model = metadata.resolve(name);
      if (declarations.containsKey(root(model).name)) {
        declare(model);
      }
    }
  }

  private JniClass root(JniClass model) throws IOException {
    metadata.sourceName(model.name);
    while (model.outer() != null) {
      model = metadata.resolve(model.outer());
    }
    return model;
  }

  private void hierarchy(String name, Set<String> visiting) throws IOException {
    if (!visiting.add(name)) {
      throw new IOException("Inheritance cycle involving " + name);
    }
    JniClass model = metadata.resolve(name);
    references.add(name);
    if (model.parent != null) {
      hierarchy(model.parent, visiting);
    }
    for (String iface : model.interfaces) {
      hierarchy(iface, visiting);
    }
    visiting.remove(name);
  }

  private void reference(Type type) throws IOException {
    if (type.getSort() == Type.ARRAY) {
      reference(type.getElementType());
    }
    if (type.getSort() == Type.OBJECT) {
      String name = type.getInternalName();
      metadata.sourceName(name);
      hierarchy(name, new HashSet<String>());
      references.add(name);
    }
  }

  private static String methodKey(JniClass.Method method) {
    return method.name + method.descriptor.substring(0, method.descriptor.indexOf(')') + 1);
  }

  /**
   * The declaring interface matters: a more specific declaration overrides
   * defaults and abstracts alike.
   */
  private static final class Contract {
    final String owner;
    final JniClass.Method method;

    Contract(String owner, JniClass.Method method) {
      this.owner = owner;
      this.method = method;
    }
  }

  /**
   * The effective type as javac sees it, after any generated declaration erases
   * its own generics.
   */
  private static final class TypeView {
    final JniClass model;
    final Map<String, JniSignature.Value> arguments;
    final boolean raw;

    TypeView(JniClass model, Map<String, JniSignature.Value> arguments, boolean raw) {
      this.model = model;
      this.arguments = arguments;
      this.raw = raw;
    }

    String key() {
      return model.name + ":" + raw + arguments;
    }

    JniClass.Method method(JniClass.Method method) {
      // A raw receiver also erases the source signature; its type variables
      // are not in scope in a generated declaration.
      if (raw) {
        return new JniClass.Method(method.access, method.name, method.descriptor, null, null, method.exceptions);
      }
      return method.signature == null ? method : JniSignature.read(method.signature).method(method, arguments);
    }
  }

  private JniSignature signature(JniClass model) {
    JniSignature signature = signatures.get(model.name);
    if (signature == null) {
      signature = JniSignature.read(model.signature);
      signatures.put(model.name, signature);
    }
    return signature;
  }

  private TypeView view(JniSignature.Value value) throws IOException {
    JniClass model = metadata.resolve(value.name);
    JniSignature signature = signature(model);
    Map<String, JniSignature.Value> scope = new java.util.LinkedHashMap<String, JniSignature.Value>();
    boolean raw = false;
    if (model.innerInstance()) {
      TypeView enclosing = value.owner == null ? rawView(model.outer()) : view(value.owner);
      scope.putAll(enclosing.arguments);
      raw = enclosing.raw;
    }
    if (value.arguments.isEmpty()) {
      raw |= !signature.bounds.isEmpty();
    } else {
      if (value.arguments.size() != signature.bounds.size()) {
        throw new IOException("Inconsistent generic type arguments for " + model.name);
      }
      // Member parameters may shadow an enclosing parameter with the same name.
      scope.putAll(signature.bind(value.arguments, scope));
    }
    return new TypeView(model, scope, raw);
  }

  private TypeView rawView(String name) throws IOException {
    return view(JniSignature.Value.object(name));
  }

  private boolean genericDeclaration(JniClass model) {
    return model.isInterface() || genericDeclarations.contains(model.name) || (model.innerInstance()
        && declarations.containsKey(model.outer()) && genericDeclaration(declarations.get(model.outer())));
  }

  private Map<String, JniSignature.Value> declarationVariables(JniClass model) throws IOException {
    Map<String, JniSignature.Value> variables = new HashMap<String, JniSignature.Value>();
    if (model.innerInstance() && genericDeclaration(metadata.resolve(model.outer()))) {
      variables.putAll(declarationVariables(metadata.resolve(model.outer())));
    }
    variables.putAll(signature(model).variables(variables, ""));
    return variables;
  }

  private JniSignature declarationFormals(JniClass model) throws IOException {
    JniSignature result = JniSignature.read(null);
    if (model.innerInstance() && genericDeclaration(metadata.resolve(model.outer()))) {
      result.bounds.putAll(declarationFormals(metadata.resolve(model.outer())).bounds);
    }
    if (genericDeclaration(model)) {
      result.bounds.putAll(sourceSignature(signature(model)).bounds);
    }
    return result;
  }

  private TypeView declarationView(JniClass model) throws IOException {
    return genericDeclaration(model) ? new TypeView(model, declarationVariables(model), false) : rawView(model.name);
  }

  private JniSignature.Value sourceType(JniSignature.Value value) {
    Set<String> raw = new HashSet<String>();
    for (JniClass model : declarations.values()) {
      if (!genericDeclaration(model)) {
        raw.add(model.name);
      }
    }
    return value.eraseArguments(raw);
  }

  private JniSignature.Value emittedSupertype(TypeView type, String name) {
    if (!type.raw) {
      for (JniSignature.Value parent : signature(type.model).parents) {
        if (name.equals(parent.name)) {
          return sourceType(parent.substitute(type.arguments));
        }
      }
    }
    return JniSignature.Value.object(name);
  }

  private JniSignature.Value emittedSuperclass(TypeView type) {
    JniSignature.Value parent = emittedSupertype(type, type.model.parent);
    if (type.model.isEnum()) {
      parent = JniSignature.Value.object(type.model.parent);
      parent.arguments.add(JniSignature.Value.object(type.model.name));
    }
    return parent;
  }

  private List<JniSignature.Value> emittedInterfaces(TypeView type) throws IOException {
    List<JniSignature.Value> result = new ArrayList<JniSignature.Value>();
    Set<String> raw = new HashSet<String>();
    if (type.model.parent != null) {
      rawInterfaces(view(emittedSuperclass(type)), raw, new HashSet<String>());
    }
    for (String name : directInterfaces(type.model)) {
      JniSignature.Value iface = emittedSupertype(type, name);
      // A generated generic superclass has lost its formals. Reconcile only
      // interface paths sharing one of its raw ancestors; unrelated arguments
      // (such as Comparable<Payload>) must remain intact.
      for (String ancestor : raw) {
        if (subtype(name, ancestor)) {
          iface = JniSignature.Value.object(name);
          break;
        }
      }
      result.add(iface);
    }
    return result;
  }

  private void rawInterfaces(TypeView type, Set<String> raw, Set<String> visited) throws IOException {
    if (!visited.add(type.key())) {
      return;
    }
    if (type.raw && type.model.isInterface()) {
      raw.add(type.model.name);
    }
    for (TypeView parent : parents(type)) {
      rawInterfaces(parent, raw, visited);
    }
  }

  private List<TypeView> parents(TypeView type) throws IOException {
    List<TypeView> result = new ArrayList<TypeView>();
    for (JniSignature.Value parent : parentTypes(type)) {
      result.add(view(parent));
    }
    return result;
  }

  private List<JniSignature.Value> parentTypes(TypeView type) throws IOException {
    JniClass model = type.model;
    List<JniSignature.Value> result = new ArrayList<JniSignature.Value>();
    if (declarations.containsKey(model.name)) {
      // Resolve exactly the types emitted in the source, including Enum<Self>.
      if (model.parent != null) {
        result.add(emittedSuperclass(type));
      }
      for (JniSignature.Value iface : emittedInterfaces(type)) {
        result.add(iface);
      }
    } else if (type.raw || model.signature == null) {
      // The supertypes of a raw type are themselves erased.
      if (model.parent != null) {
        result.add(JniSignature.Value.object(model.parent));
      }
      for (String iface : model.interfaces) {
        result.add(JniSignature.Value.object(iface));
      }
    } else {
      for (JniSignature.Value parent : signature(model).parents) {
        result.add(parent.substitute(type.arguments));
      }
    }
    return result;
  }

  private void referenceSignature(JniSignature signature) throws IOException {
    for (List<JniSignature.Value> bounds : signature.bounds.values()) {
      for (JniSignature.Value bound : bounds) {
        reference(bound);
      }
    }
    for (JniSignature.Value value : signature.parameters) {
      reference(value);
    }
    if (signature.result != null) {
      reference(signature.result);
    }
  }

  private void reference(JniSignature.Value value) throws IOException {
    Set<String> names = new HashSet<String>();
    value.classNames(names);
    for (String name : names) {
      reference(Type.getObjectType(name));
    }
  }

  private void interfaceMethods(TypeView type, Map<String, List<Contract>> methods, Set<String> visited)
      throws IOException {
    if (!visited.add(type.key())) {
      return;
    }
    for (TypeView parent : parents(type)) {
      interfaceMethods(parent, methods, visited);
    }
    JniClass model = type.model;
    if (model.isInterface() && !declarations.containsKey(model.name)) {
      for (JniClass.Method original : model.instanceMethods) {
        if ((original.access & Opcodes.ACC_BRIDGE) != 0) {
          continue;
        }
        JniClass.Method method = type.method(original);
        String key = methodKey(method);
        List<Contract> contracts = methods.get(key);
        if (contracts == null) {
          contracts = new ArrayList<Contract>();
          methods.put(key, contracts);
        }
        contracts.add(new Contract(model.name, method));
      }
    }
  }

  private List<JniClass.Method> interfaceMethods(JniClass model) throws IOException {
    Map<String, List<Contract>> methods = new TreeMap<String, List<Contract>>();
    interfaceMethods(declarationView(model), methods, new HashSet<String>());
    Map<String, JniClass.Method> implementations = new HashMap<String, JniClass.Method>();
    for (JniClass.Method original : model.instanceMethods) {
      JniClass.Method method = declarationView(model).method(original);
      // Bridges are bytecode adapters, not source overrides. In particular a
      // bridge may delegate to a narrower (possibly final) superclass method.
      if ((method.access & Opcodes.ACC_BRIDGE) == 0) {
        implementations.put(methodKey(method), method);
      }
    }
    List<JniClass.Method> result = new ArrayList<JniClass.Method>();
    for (Map.Entry<String, List<Contract>> entry : methods.entrySet()) {
      List<Contract> contracts = new ArrayList<Contract>();
      for (Contract candidate : entry.getValue()) {
        boolean overridden = false;
        for (Contract other : entry.getValue()) {
          if (!candidate.owner.equals(other.owner) && subtype(other.owner, candidate.owner)) {
            overridden = true;
            break;
          }
        }
        if (!overridden) {
          contracts.add(candidate);
        }
      }
      boolean hasDefault = false;
      for (Contract contract : contracts) {
        if ((contract.method.access & Opcodes.ACC_ABSTRACT) == 0) {
          hasDefault = true;
        }
      }
      JniClass.Method own = implementations.get(entry.getKey());
      // An abstract declaration may omit an interface implementation, but cannot
      // inherit a superclass member with an incompatible return, access or throws.
      Contract superclass = superclassContract(model, entry.getKey());
      boolean reconcileSuperclass = needsSuperclassOverride(superclass, contracts);
      if (!reconcileSuperclass && contracts.size() == 1 && hasDefault) {
        continue;
      }
      boolean concrete = model.isEnum() || model.isRecord();
      if (!reconcileSuperclass && !concrete && !hasDefault && contracts.size() == 1) {
        continue;
      }
      if (targets.contains(model.name) && own != null && (own.access & Opcodes.ACC_NATIVE) != 0) {
        continue;
      }
      // Do not override retained superclass implementations, notably Enum's final
      // methods.
      if (!reconcileSuperclass && own == null && inheritsImplementation(model, entry.getKey())) {
        continue;
      }
      List<Contract> returns = new ArrayList<Contract>(contracts);
      if (superclass != null) {
        returns.add(superclass);
      }
      JniClass.Method contract = compatibleReturn(returns);
      if (!reconcileSuperclass && !concrete && !hasDefault && contract != null) {
        continue;
      }
      JniClass.Method method = own == null ? contract : own;
      if (method == null) {
        method = inheritedContractOverride(model, entry.getKey(), returns);
      }
      if (method == null) {
        throw new IOException("No compatible interface return type for " + model.name + "." + entry.getKey());
      }
      result.add(method);
    }
    return result;
  }

  private JniClass.Method inheritedContractOverride(JniClass model, String key, List<Contract> contracts)
      throws IOException {
    // An omitted ancestor may have supplied the common subtype required by
    // both the retained superclass and interfaces. Retain it only when necessary.
    TypeView type = declarationView(model);
    while (type.model.parent != null) {
      type = parents(type).get(0);
      for (JniClass.Method original : type.model.instanceMethods) {
        if ((original.access & Opcodes.ACC_BRIDGE) != 0) {
          continue;
        }
        if ((original.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0
            && !type.model.packageName().equals(model.packageName())) {
          continue;
        }
        JniClass.Method method = type.method(original);
        if (!key.equals(methodKey(method))) {
          continue;
        }
        for (Contract contract : contracts) {
          if (!compatibleReturn(method, contract.method)) {
            return null;
          }
        }
        return method;
      }
    }
    return null;
  }

  private Contract superclassContract(JniClass model, String key) throws IOException {
    TypeView type = declarationView(model);
    while (type.model.parent != null) {
      type = parents(type).get(0);
      // Original non-native bodies are omitted from reconstructed ancestors.
      List<JniClass.Method> methods = type.model.instanceMethods;
      if (declarations.containsKey(type.model.name)) {
        methods = new ArrayList<JniClass.Method>(interfaceMethods(type.model));
        if (targets.contains(type.model.name)) {
          methods.addAll(type.model.natives);
        }
      }
      for (JniClass.Method original : methods) {
        if ((original.access & (Opcodes.ACC_BRIDGE | Opcodes.ACC_STATIC)) != 0) {
          continue;
        }
        if ((original.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0
            && !type.model.packageName().equals(model.packageName())) {
          continue;
        }
        JniClass.Method inherited = type.method(original);
        if (!key.equals(methodKey(inherited))) {
          continue;
        }
        return new Contract(type.model.name, inherited);
      }
    }
    return null;
  }

  private boolean needsSuperclassOverride(Contract superclass, List<Contract> contracts) throws IOException {
    if (superclass == null) {
      return false;
    }
    JniClass.Method inherited = superclass.method;
    if ((inherited.access & Opcodes.ACC_PUBLIC) == 0) {
      return true;
    }
    for (Contract contract : contracts) {
      if (!compatibleReturn(inherited, contract.method)) {
        return true;
      }
      for (String exception : inherited.exceptions) {
        if (contract.method.exceptions.contains(exception)) {
          continue;
        }
        if (subtype(exception, "java/lang/RuntimeException") || subtype(exception, "java/lang/Error")) {
          continue;
        }
        boolean allowed = false;
        for (String declared : contract.method.exceptions) {
          if (subtype(exception, declared)) {
            allowed = true;
            break;
          }
        }
        if (!allowed) {
          return true;
        }
      }
    }
    return false;
  }

  private JniClass.Method compatibleReturn(List<Contract> contracts) throws IOException {
    // Pick a return assignable to every contract, regardless of interface order.
    for (Contract candidate : contracts) {
      boolean compatible = true;
      for (Contract other : contracts) {
        if (!compatibleReturn(candidate.method, other.method)) {
          compatible = false;
          break;
        }
      }
      if (compatible) {
        return candidate.method;
      }
    }
    return null;
  }

  private boolean compatibleReturn(JniClass.Method candidate, JniClass.Method other) throws IOException {
    Type child = Type.getReturnType(candidate.descriptor);
    Type parent = Type.getReturnType(other.descriptor);
    if (!subtype(child, parent)) {
      return false;
    }
    JniSignature left = candidate.source == null ? JniSignature.read(candidate.signature) : candidate.source;
    JniSignature right = other.source == null ? JniSignature.read(other.signature) : other.source;
    Map<String, List<JniSignature.Value>> bounds = new HashMap<String, List<JniSignature.Value>>();
    JniSignature.Value from = returnType(left, child, bounds);
    JniSignature.Value to = returnType(right, parent, bounds);
    return sourceSubtype(from, to, bounds, new HashSet<String>());
  }

  private JniSignature.Value returnType(JniSignature method, Type erased,
      Map<String, List<JniSignature.Value>> bounds) {
    if (method.result == null) {
      return JniSignature.Value.type(erased);
    }
    // Adapt method formals by position before comparing <T> List<T> with
    // <U> List<U>. These internal names never appear in generated Java.
    Map<String, String> names = new HashMap<String, String>();
    for (String name : method.bounds.keySet()) {
      names.put(name, "#" + names.size());
    }
    for (Map.Entry<String, List<JniSignature.Value>> formal : method.bounds.entrySet()) {
      List<JniSignature.Value> values = new ArrayList<JniSignature.Value>();
      for (JniSignature.Value bound : formal.getValue()) {
        values.add(sourceType(bound.rename(names)));
      }
      bounds.put(names.get(formal.getKey()), values);
    }
    return sourceType(method.result.rename(names));
  }

  private boolean sourceSubtype(JniSignature.Value child, JniSignature.Value parent,
      Map<String, List<JniSignature.Value>> bounds, Set<String> visiting)
      throws IOException {
    if (child.wildcard != '=') {
      for (JniSignature.Value upper : child.upperBounds()) {
        if (sourceSubtype(sourceType(upper), parent, bounds, visiting)) {
          return true;
        }
      }
      return false;
    }
    if (child.same(parent)) {
      return true;
    }
    String key = child + " <: " + parent;
    if (!visiting.add(key)) {
      return false;
    }
    try {
      if (child.variable != null) {
        List<JniSignature.Value> limits = bounds.get(child.variable);
        if (limits == null) {
          limits = child.limits;
        }
        if (limits != null) {
          for (JniSignature.Value bound : limits) {
            if (sourceSubtype(sourceType(bound), parent, bounds, visiting)) {
              return true;
            }
          }
        }
        return "java/lang/Object".equals(parent.name);
      }
      if (parent.variable != null) {
        return false;
      }
      if (child.component != null && parent.component != null) {
        return sourceSubtype(child.component, parent.component, bounds, visiting);
      }
      if (!subtype(child.erase(), parent.erase())) {
        return false;
      }
      if (parent.name == null || "java/lang/Object".equals(parent.name)) {
        return true;
      }
      if (child.name == null) {
        return parent.arguments.isEmpty();
      }
      if (!child.name.equals(parent.name)) {
        for (JniSignature.Value ancestor : parentTypes(view(child))) {
          if (sourceSubtype(ancestor, parent, bounds, visiting)) {
            return true;
          }
        }
        return false;
      }
      if (child.owner != null && parent.owner != null && !sourceSubtype(child.owner, parent.owner, bounds, visiting)) {
        return false;
      }
      // A genuinely raw return may implement a parameterized contract by
      // unchecked conversion (JLS 8.4.5); parameterized returns are invariant.
      if (child.arguments.isEmpty() || parent.arguments.isEmpty()) {
        return true;
      }
      if (child.arguments.size() != parent.arguments.size()) {
        return false;
      }
      for (int i = 0; i < child.arguments.size(); i++) {
        if (!contains(parent.arguments.get(i), child.arguments.get(i), bounds, visiting)) {
          return false;
        }
      }
      return true;
    } finally {
      visiting.remove(key);
    }
  }

  private boolean contains(JniSignature.Value target, JniSignature.Value value,
      Map<String, List<JniSignature.Value>> bounds, Set<String> visiting)
      throws IOException {
    // JLS 4.5.1: extends bounds are covariant; super bounds reverse the relation.
    if (target.same(value) || target.wildcard == '*') {
      return true;
    }
    if (target.wildcard == '+') {
      for (JniSignature.Value upper : value.upperBounds()) {
        if (sourceSubtype(sourceType(upper), target.withWildcard('='), bounds, visiting)) {
          return true;
        }
      }
      return false;
    }
    if (target.wildcard == '-') {
      return value.wildcard != '+' && value.wildcard != '*'
          && sourceSubtype(target.withWildcard('='), value.withWildcard('='), bounds, visiting);
    }
    return false;
  }

  private boolean subtype(Type child, Type parent) throws IOException {
    if (child.equals(parent)) {
      return true;
    }
    if (child.getSort() == Type.ARRAY) {
      if (parent.getSort() == Type.ARRAY) {
        return subtype(Type.getType(child.getDescriptor().substring(1)),
            Type.getType(parent.getDescriptor().substring(1)));
      }
      return parent.equals(Type.getType(Object.class)) || parent.equals(Type.getType(Cloneable.class))
          || parent.equals(Type.getType(java.io.Serializable.class));
    }
    return child.getSort() == Type.OBJECT && parent.getSort() == Type.OBJECT
        && subtype(child.getInternalName(), parent.getInternalName());
  }

  private boolean subtype(String child, String parent) throws IOException {
    return subtype(child, parent, new HashSet<String>());
  }

  private boolean subtype(String child, String parent, Set<String> visited) throws IOException {
    if (child.equals(parent) || "java/lang/Object".equals(parent)) {
      return true;
    }
    if (!visited.add(child)) {
      return false;
    }
    JniClass model = metadata.resolve(child);
    if (model.parent != null && subtype(model.parent, parent, visited)) {
      return true;
    }
    for (String iface : model.interfaces) {
      if (subtype(iface, parent, visited)) {
        return true;
      }
    }
    return false;
  }

  private boolean inheritsImplementation(JniClass model, String key) throws IOException {
    TypeView type = rawView(model.name);
    while (type.model.parent != null) {
      type = parents(type).get(0);
      for (JniClass.Method original : type.model.instanceMethods) {
        if ((original.access & Opcodes.ACC_BRIDGE) != 0) {
          continue;
        }
        JniClass.Method method = type.method(original);
        if (key.equals(methodKey(method))) {
          if (declarations.containsKey(type.model.name)) {
            return false;
          } // its original bodies are omitted
          return (method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT)) == Opcodes.ACC_PUBLIC;
        }
      }
    }
    return false;
  }

  private List<String> directInterfaces(JniClass model) throws IOException {
    List<String> result = new ArrayList<String>();
    for (String iface : model.interfaces) {
      boolean inherited = model.parent != null && subtype(model.parent, iface);
      for (String other : model.interfaces) {
        if (!iface.equals(other) && subtype(other, iface)) {
          inherited = true;
          break;
        }
      }
      // Keep the inherited instantiation (e.g. Enum<E>'s Comparable<E>),
      // rather than adding an incompatible raw version of the same interface.
      if (!inherited) {
        result.add(iface);
      }
    }
    return result;
  }

  private boolean hasSealedParent(JniClass model) throws IOException {
    List<String> parents = new ArrayList<String>(directInterfaces(model));
    if (model.parent != null) {
      parents.add(model.parent);
    }
    for (String parent : parents) {
      if (!declarations.containsKey(parent) && metadata.resolve(parent).sealed) {
        return true;
      }
    }
    return false;
  }

  private JniSourceNames names(JniClass model) throws IOException {
    return names(model, true);
  }

  private JniSourceNames names(final JniClass model, boolean body) throws IOException {
    JniClass root = root(model);
    JniSourceNames names = sourceUnits.get(root.name);
    if (names == null) {
      names = new JniSourceNames(metadata, root.name);
      sourceUnits.put(root.name, names);
    }
    Map<String, Set<String>> bindings = new HashMap<String, Set<String>>();
    final Map<String, Map<String, JniClass.Member>> members = new HashMap<String, Map<String, JniClass.Member>>();
    if (!body) {
      bindings.put(model.simple(), Collections.singleton(model.name));
    }
    // Members are in scope in the body, not the superclass/interface clauses
    // (JLS 6.3). An inner declaration's header still sees its enclosing body.
    for (JniClass scope = body ? model
        : model.outer() == null ? null : metadata.resolve(model.outer()); scope != null; scope = scope.outer() == null
            ? null
            : metadata.resolve(scope.outer())) {
      Map<String, Set<String>> local = new HashMap<String, Set<String>>();
      for (Map.Entry<String, JniClass.Member> entry : memberTypes(scope, members, new HashSet<String>()).entrySet()) {
        String simple = entry.getValue().simple;
        if (!local.containsKey(simple)) {
          local.put(simple, new TreeSet<String>());
        }
        local.get(simple).add(entry.getKey());
      }
      // A declared/inherited member hides the enclosing class's own simple
      // name too. Multiple inherited declarations remain ambiguous; diamond
      // paths to the same binary class do not introduce ambiguity.
      if (!local.containsKey(scope.simple())) {
        local.put(scope.simple(), Collections.singleton(scope.name));
      }
      for (Map.Entry<String, Set<String>> entry : local.entrySet()) {
        if (!bindings.containsKey(entry.getKey())) {
          bindings.put(entry.getKey(), entry.getValue());
        }
      }
    }
    names.scope(bindings, new JniSourceNames.Access() {
      public boolean visible(String name) throws IOException {
        JniClass type = metadata.resolve(name);
        return accessible(type.nesting == null ? type.access : type.nesting.access, type, model, false);
      }

      public JniSourceNames.Qualifier qualifier(String name, JniSignature.Value owner,
          Map<String, List<JniSignature.Value>> bounds, boolean reconstruct)
          throws IOException {
        return qualifierType(name, owner, bounds, reconstruct);
      }

      public Set<String> qualifiers(String name, boolean discover) throws IOException {
        JniClass member = metadata.resolve(name);
        Set<String> result = new TreeSet<String>();
        for (String candidate : metadata.subtypes(member.outer(), discover)) {
          try {
            Map<String, JniClass.Member> inherited = memberTypes(metadata.resolve(candidate), members,
                new HashSet<String>());
            if (!inherited.containsKey(name)) {
              continue;
            }
            boolean unique = true;
            for (Map.Entry<String, JniClass.Member> entry : inherited.entrySet()) {
              if (member.simple().equals(entry.getValue().simple) && !name.equals(entry.getKey())) {
                unique = false;
                break;
              }
            }
            if (unique) {
              result.add(candidate);
            }
          } catch (IOException unusable) {
            // An unrelated potential qualifier with missing metadata must not
            // prevent another candidate from providing the required member.
          }
        }
        return result;
      }
    });
    return names;
  }

  private JniSourceNames.Qualifier qualifierType(String name, JniSignature.Value owner,
      Map<String, List<JniSignature.Value>> bounds, boolean reconstruct)
      throws IOException {
    boolean parameterized = false;
    for (JniSignature.Value current = owner; current != null; current = current.owner) {
      parameterized |= !current.arguments.isEmpty();
    }
    if (!parameterized) {
      return new JniSourceNames.Qualifier(JniSignature.Value.object(name));
    }
    Set<String> variables = new HashSet<String>();
    JniSignature.Value template = qualifierTemplate(metadata.resolve(name), variables);
    JniSignature.Value projected = projectQualifier(template, owner.name, new HashSet<String>());
    Map<String, JniSignature.Value> arguments = new HashMap<String, JniSignature.Value>();
    if (projected == null) {
      return null;
    }
    if (!matchQualifier(projected, owner, variables, arguments)) {
      if (reconstruct && projected.arguments.isEmpty() && view(projected).raw) {
        Set<String> needed = new HashSet<String>();
        generatedQualifierPath(name, owner.name, needed, new HashSet<String>());
        if (!needed.isEmpty()) {
          // Retain the generated qualifier's generics in the next metadata pass.
          // Erasing this occurrence alone breaks existing dependency contracts.
          return new JniSourceNames.Qualifier(JniSignature.Value.object(name), needed);
        }
      }
      return null;
    }
    for (String variable : variables) {
      if (!arguments.containsKey(variable)) {
        JniSignature.Value wildcard = new JniSignature.Value();
        wildcard.wildcard = '*';
        arguments.put(variable, wildcard);
      }
    }
    JniSignature.Value result = template.substitute(arguments);
    JniSignature.Value actual = projectQualifier(result, owner.name, new HashSet<String>());
    return actual != null && actual.same(owner) && qualifierBounds(result, owner, bounds)
        ? new JniSourceNames.Qualifier(result)
        : null;
  }

  private void generatedQualifierPath(String name, String owner, Set<String> needed, Set<String> visited)
      throws IOException {
    if (!visited.add(name) || !subtype(name, owner)) {
      return;
    }
    JniClass model = metadata.resolve(name);
    for (JniClass scope = model; scope != null; scope = scope.innerInstance() ? metadata.resolve(scope.outer())
        : null) {
      if (declarations.containsKey(scope.name) && !genericDeclaration(scope) && !signature(scope).bounds.isEmpty()) {
        needed.add(scope.name);
      }
    }
    if (model.parent != null) {
      generatedQualifierPath(model.parent, owner, needed, visited);
    }
    for (String iface : model.interfaces) {
      generatedQualifierPath(iface, owner, needed, visited);
    }
  }

  private JniSignature.Value qualifierTemplate(JniClass model, Set<String> variables) throws IOException {
    JniSignature.Value result = JniSignature.Value.object(model.name);
    if (model.innerInstance()) {
      result.owner = qualifierTemplate(metadata.resolve(model.outer()), variables);
    }
    if (!declarations.containsKey(model.name) || genericDeclaration(model)) {
      Map<String, JniSignature.Value> enclosing = result.owner == null
          ? Collections.<String, JniSignature.Value> emptyMap()
          : view(result.owner).arguments;
      for (JniSignature.Value argument : signature(model).variables(enclosing, "#qualifier:" + model.name + ":")
          .values()) {
        result.arguments.add(argument);
        variables.add(argument.variable);
      }
    }
    return result;
  }

  private JniSignature.Value projectQualifier(JniSignature.Value value, String owner, Set<String> visited)
      throws IOException {
    if (value.name.equals(owner)) {
      return value;
    }
    if (!visited.add(value.toString())) {
      return null;
    }
    for (JniSignature.Value parent : parentTypes(view(value))) {
      JniSignature.Value result = projectQualifier(parent, owner, visited);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private boolean matchQualifier(JniSignature.Value pattern, JniSignature.Value actual, Set<String> variables,
      Map<String, JniSignature.Value> arguments) {
    if (pattern == null || actual == null) {
      return pattern == actual;
    }
    if (variables.contains(pattern.variable)) {
      if (pattern.wildcard != '=' && pattern.wildcard != actual.wildcard) {
        return false;
      }
      JniSignature.Value value = pattern.wildcard == '=' ? actual : actual.withWildcard('=');
      JniSignature.Value previous = arguments.get(pattern.variable);
      if (previous != null) {
        return previous.same(value);
      }
      arguments.put(pattern.variable, value);
      return true;
    }
    if (pattern.same(actual)) {
      return true;
    }
    if (pattern.wildcard != actual.wildcard || !java.util.Objects.equals(pattern.name, actual.name)
        || !java.util.Objects.equals(pattern.variable, actual.variable)
        || pattern.arguments.size() != actual.arguments.size()
        || !matchQualifier(pattern.owner, actual.owner, variables, arguments)
        || !matchQualifier(pattern.component, actual.component, variables, arguments)) {
      return false;
    }
    if (pattern.name == null && pattern.variable == null && pattern.component == null) {
      return false;
    }
    for (int i = 0; i < pattern.arguments.size(); i++) {
      if (!matchQualifier(pattern.arguments.get(i), actual.arguments.get(i), variables, arguments)) {
        return false;
      }
    }
    return true;
  }

  private boolean qualifierBounds(JniSignature.Value qualifier, JniSignature.Value owner,
      Map<String, List<JniSignature.Value>> scope)
      throws IOException {
    Map<String, List<JniSignature.Value>> bounds = new HashMap<String, List<JniSignature.Value>>();
    for (String variable : scope.keySet()) {
      bounds.put(variable, new ArrayList<JniSignature.Value>(scope.get(variable)));
    }
    // A valid owner instantiation supplies bounds for its symbolic arguments.
    for (JniSignature.Value value = owner; value != null; value = value.owner) {
      TypeView type = view(value);
      for (String formal : signature(type.model).bounds.keySet()) {
        JniSignature.Value argument = type.arguments.get(formal);
        if (argument == null || argument.variable == null) {
          continue;
        }
        List<JniSignature.Value> limits = bounds.get(argument.variable);
        if (limits == null) {
          limits = new ArrayList<JniSignature.Value>();
          bounds.put(argument.variable, limits);
        }
        limits.addAll(argument.limits);
        for (JniSignature.Value limit : signature(type.model).bounds.get(formal)) {
          limits.add(limit.substitute(type.arguments));
        }
      }
    }
    for (JniSignature.Value value = qualifier; value != null; value = value.owner) {
      TypeView type = view(value);
      for (String formal : signature(type.model).bounds.keySet()) {
        JniSignature.Value argument = type.arguments.get(formal);
        if (argument == null || argument.wildcard == '*') {
          continue;
        }
        for (JniSignature.Value limit : signature(type.model).bounds.get(formal)) {
          JniSignature.Value bound = limit.substitute(type.arguments);
          JniSignature.Value valueBound = argument.withWildcard('=');
          // An extends wildcard needs a possible intersection with each declared
          // upper bound; a super wildcard's lower bound must satisfy that bound.
          // Reuse the constructor solver's generic-ancestor overlap checks.
          if (argument.wildcard == '+') {
            if (!constructorResolver().overlappingBounds(valueBound, bound, bounds)) {
              return false;
            }
          } else if (!sourceSubtype(valueBound, bound, bounds, new HashSet<String>())) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private Map<String, JniClass.Member> memberTypes(JniClass model, Map<String, Map<String, JniClass.Member>> cache,
      Set<String> visiting)
      throws IOException {
    if (cache.containsKey(model.name)) {
      return cache.get(model.name);
    }
    if (!visiting.add(model.name)) {
      throw new IOException("Inheritance cycle involving " + model.name);
    }
    Map<String, JniClass.Member> result = new TreeMap<String, JniClass.Member>();
    if (declarations.containsKey(model.name)) {
      // Omitted class-file members cannot hide names in a reconstructed type.
      for (JniClass declaration : declarations.values()) {
        if (model.name.equals(declaration.outer())) {
          result.put(declaration.name, declaration.nesting);
        }
      }
    } else {
      for (Map.Entry<String, JniClass.Member> entry : model.members.entrySet()) {
        if (model.name.equals(entry.getValue().outer) && entry.getValue().simple != null) {
          result.put(entry.getKey(), entry.getValue());
        }
      }
    }
    Set<String> declared = new HashSet<String>();
    for (JniClass.Member member : result.values()) {
      declared.add(member.simple);
    }
    List<String> parents = new ArrayList<String>(model.interfaces);
    if (model.parent != null) {
      parents.add(model.parent);
    }
    for (String parent : parents) {
      for (Map.Entry<String, JniClass.Member> entry : memberTypes(metadata.resolve(parent), cache, visiting)
          .entrySet()) {
        JniClass.Member member = entry.getValue();
        int slash = entry.getKey().lastIndexOf('/');
        String packageName = slash < 0 ? "" : entry.getKey().substring(0, slash);
        if (!declared.contains(member.simple) && (member.access & Opcodes.ACC_PRIVATE) == 0
            && ((member.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0
                || model.packageName().equals(packageName))) {
          result.put(entry.getKey(), member);
        }
      }
    }
    visiting.remove(model.name);
    cache.put(model.name, result);
    // InnerClasses already supplies names and access flags. Do not resolve the
    // member class files: unrelated members may deliberately be absent.
    return result;
  }

  private JniSignature sourceSignature(JniSignature original) {
    JniSignature result = JniSignature.read(null);
    for (Map.Entry<String, List<JniSignature.Value>> formal : original.bounds.entrySet()) {
      List<JniSignature.Value> bounds = new ArrayList<JniSignature.Value>();
      for (JniSignature.Value bound : formal.getValue()) {
        bounds.add(sourceType(bound));
      }
      result.bounds.put(formal.getKey(), bounds);
    }
    for (JniSignature.Value parent : original.parents) {
      result.parents.add(sourceType(parent));
    }
    for (JniSignature.Value parameter : original.parameters) {
      result.parameters.add(sourceType(parameter));
    }
    result.result = original.result == null ? null : sourceType(original.result);
    return result;
  }

  private void sourceScope() {
    sourceNames.typeVariables(sourceVariables.values());
    JniSignature bounds = JniSignature.read(null);
    // Preflight uses original formal names; emission uses their fresh names.
    bounds.bounds.putAll(sourceFormals.bounds);
    bounds.bounds.putAll(sourceFormals.rename(sourceVariables).bounds);
    sourceNames.formals(bounds);
  }

  private String source(JniSignature.Value value) throws IOException {
    sourceScope();
    return value.rename(sourceVariables).source(metadata, sourceNames);
  }

  private String type(Type type) throws IOException {
    sourceScope();
    if (type.getSort() == Type.ARRAY) {
      StringBuilder result = new StringBuilder(type(type.getElementType()));
      for (int i = 0; i < type.getDimensions(); i++) {
        result.append("[]");
      }
      return result.toString();
    }
    return type.getSort() == Type.OBJECT ? sourceNames.name(type.getInternalName()) : type.getClassName();
  }

  private static final class Constructor {
    final JniSignature formals;
    final JniSignature.Value owner;
    final List<JniSignature.Value> arguments;
    final List<String> exceptions;

    Constructor(JniSignature formals, JniSignature.Value owner, List<JniSignature.Value> arguments,
        List<String> exceptions) {
      this.formals = formals;
      this.owner = owner;
      this.arguments = arguments;
      this.exceptions = exceptions;
    }
  }

  private Constructor constructor(JniClass model) throws IOException {
    if (model.parent == null) {
      return new Constructor(JniSignature.read(null), null, Collections.<JniSignature.Value> emptyList(),
          Collections.<String> emptyList());
    }
    JniSignature.Value parentType = emittedSuperclass(declarationView(model));
    TypeView parentView = view(parentType);
    JniClass parent = parentView.model;
    JniSignature.Value owner = null;
    int skip = 0;
    if (parent.innerInstance()) {
      reference(Type.getObjectType(parent.outer()));
      owner = parentType.owner == null ? JniSignature.Value.object(parent.outer()) : parentType.owner;
      reference(owner);
      skip = 1;
    }
    if (declarations.containsKey(parent.name)) {
      return checkedConstructor(model, JniSignature.read(null), owner, Collections.<JniSignature.Value> emptyList(),
          constructors.get(parent.name).exceptions, new HashSet<String>());
    }
    List<JniClass.Method> candidates = new ArrayList<JniClass.Method>(parent.constructors);
    Collections.sort(candidates, new Comparator<JniClass.Method>() {
      public int compare(JniClass.Method left, JniClass.Method right) {
        int result = Type.getArgumentTypes(left.descriptor).length - Type.getArgumentTypes(right.descriptor).length;
        return result == 0 ? left.descriptor.compareTo(right.descriptor) : result;
      }
    });
    List<String> rejected = new ArrayList<String>();
    // Try fully typed calls, then inferred calls with casts for every argument,
    // and finally calls needing bare nulls. A null can match unrelated overloads,
    // so it must not hide a later candidate that a raw cast can disambiguate.
    for (int attempt = 0; attempt < 3; attempt++) {
      boolean inferred = attempt != 0;
      for (JniClass.Method candidate : candidates) {
        if (!accessible(candidate.access, parent, model, true)) {
          if (!inferred) {
            rejected.add(candidate.descriptor + " (inaccessible)");
          }
          continue;
        }
        try {
          Type[] originalArguments = Type.getArgumentTypes(candidate.descriptor);
          JniClass.Method specialized = parentView.method(candidate);
          JniSignature source = specialized.source;
          if (source != null) {
            source = constructorSignature(source, parentType, model);
          }
          Type[] arguments = Type.getArgumentTypes(specialized.descriptor);
          int sourceOffset = source == null ? 0 : originalArguments.length - source.parameters.size();
          // Inner-class signatures omit the synthetic enclosing-instance parameter.
          if (skip == 1 && arguments.length + 1 == originalArguments.length) {
            Type[] withOwner = new Type[originalArguments.length];
            withOwner[0] = originalArguments[0];
            System.arraycopy(arguments, 0, withOwner, 1, arguments.length);
            arguments = withOwner;
          }
          if (skip == 1 && (arguments.length == 0 || !Type.getObjectType(parent.outer()).equals(arguments[0]))) {
            throw new IOException("missing enclosing-instance parameter");
          }
          List<JniSignature.Value> values = new ArrayList<JniSignature.Value>();
          for (int i = skip; i < arguments.length; i++) {
            values.add(source == null ? JniSignature.Value.type(arguments[i])
                : sourceType(source.parameters.get(i - sourceOffset)));
          }
          if (inferred) {
            inferConstructorArguments(source, values, model);
          }
          if (attempt == 1 && values.contains(null)) {
            continue;
          }
          JniSignature formals = source == null ? JniSignature.read(null) : source.constructorFormals(values);
          Set<String> names = new HashSet<String>();
          for (List<JniSignature.Value> bounds : formals.bounds.values()) {
            for (JniSignature.Value bound : bounds) {
              sourceType(bound).classNames(names);
            }
          }
          for (JniSignature.Value argument : values) {
            if (argument != null) {
              argument.classNames(names);
            }
          }
          // Resolve required definitions now; access is checked on complete
          // source types in checkedConstructor, not on an owner in isolation.
          for (String name : names) {
            metadata.sourceName(name);
          }
          JniConstructorResolver.Candidate selected = constructorResolver().resolve(values, formals.bounds,
              constructorOverloads(parentView, model, values.size()));
          List<String> exceptions = new ArrayList<String>();
          for (String exception : constructorResolver().exceptions(values, formals.bounds, selected)) {
            if (!subtype(exception, "java/lang/RuntimeException") && !subtype(exception, "java/lang/Error")) {
              exceptions.add(exception);
            }
          }
          return checkedConstructor(model, formals, owner, values, exceptions, names);
        } catch (IOException ex) {
          rejected.add(candidate.descriptor + (inferred ? " (inferred: " : " (") + ex.getMessage() + ")");
        }
      }
    }
    throw new IOException("Cannot construct superclass " + parent.name + " for " + model.name
        + ": no accessible, source-expressible constructor " + rejected);
  }

  private String constructorException(String name, JniClass model, JniSourceNames names) throws IOException {
    String original = name;
    Set<String> visited = new HashSet<String>();
    while (name != null && !"java/lang/Object".equals(name) && visited.add(name)) {
      JniClass exception = metadata.resolve(name);
      try {
        if (constructorTypeAccessible(name, model)) {
          JniSourceNames trial = names.copy();
          trial.name(name);
          names.use(trial);
          return name;
        }
      } catch (IOException unexpressible) {
        // A checked exception may be private or hidden by a source name. Its
        // nearest expressible superclass still covers the selected super call.
      }
      name = exception.parent;
    }
    throw new IOException("Cannot express constructor exception " + original + " for " + model.name);
  }

  private Constructor checkedConstructor(JniClass model, JniSignature formals, JniSignature.Value owner,
      List<JniSignature.Value> arguments, List<String> thrown, Set<String> types)
      throws IOException {
    JniSourceNames trial = names(model).copy();
    if (owner != null) {
      owner.classNames(types);
    }
    trial.reserve(types);
    List<String> exceptions = new ArrayList<String>();
    for (String exception : thrown) {
      String visible = constructorException(exception, model, trial);
      if (!exceptions.contains(visible)) {
        exceptions.add(visible);
      }
      types.add(visible);
    }
    Constructor result = new Constructor(formals, owner, arguments, exceptions);
    JniSourceNames previousNames = sourceNames;
    Map<String, String> previousVariables = sourceVariables;
    JniSignature previousFormals = sourceFormals;
    Map<String, JniSourceNames> previousUnits = new HashMap<String, JniSourceNames>(sourceUnits);
    Map<String, Constructor> previousConstructors = new HashMap<String, Constructor>(constructors);
    Set<String> previousReferences = new HashSet<String>(references);
    boolean retained = false;
    try {
      sourceNames = trial;
      sourceVariables = classVariables.get(model.name);
      sourceFormals = declarationFormals(model);
      emitConstructor(model, result, new StringBuilder(), "");
      for (String type : types) {
        reference(Type.getObjectType(type));
      }
      sourceUnits.put(root(model).name, trial);
      constructors.put(model.name, result);
      // A locally valid choice may impose an unspellable throws clause on a
      // generated descendant. Validate the whole inheritance subtree before
      // retaining this candidate, including exception widening in intermediates.
      for (JniClass child : declarations.values()) {
        if (model.name.equals(child.parent)) {
          constructors.put(child.name, constructor(child));
        }
      }
      retained = true;
      return result;
    } finally {
      if (!retained) {
        // Roll back sibling/descendant imports and references too, so the next
        // parent candidate is evaluated against the same declaration context.
        sourceUnits.clear();
        sourceUnits.putAll(previousUnits);
        constructors.clear();
        constructors.putAll(previousConstructors);
        references.clear();
        references.addAll(previousReferences);
      }
      sourceNames = previousNames;
      sourceVariables = previousVariables;
      sourceFormals = previousFormals;
    }
  }

  private void emitConstructor(JniClass model, Constructor constructor, StringBuilder out, String indent)
      throws IOException {
    List<String> exceptions = constructor.exceptions;
    // Constructor formals must not capture exception names introduced after
    // overload selection, including a widened exception from a source parent.
    JniSignature used = JniSignature.read(null);
    used.bounds.putAll(constructor.formals.bounds);
    if (constructor.owner != null) {
      used.parents.add(constructor.owner);
    }
    for (JniSignature.Value argument : constructor.arguments) {
      if (argument != null) {
        used.parameters.add(argument);
      }
    }
    Map<String, String> enclosingVariables = sourceVariables;
    JniSignature enclosingFormals = sourceFormals;
    sourceFormals = JniSignature.read(null);
    sourceFormals.bounds.putAll(enclosingFormals.bounds);
    sourceFormals.bounds.putAll(used.bounds);
    sourceScope();
    sourceVariables = new HashMap<String, String>(enclosingVariables);
    sourceVariables.putAll(sourceNames.variables(sourceSignature(used), "_NarConstructor", exceptions));
    sourceScope();
    out.append(indent).append("  protected ");
    if (!constructor.formals.bounds.isEmpty()) {
      emitFormals(constructor.formals, out);
      out.append(' ');
    }
    out.append(model.simple()).append("()");
    for (int i = 0; i < exceptions.size(); i++) {
      out.append(i == 0 ? " throws " : ", ").append(sourceNames.name(exceptions.get(i)));
    }
    out.append(" { ");
    if (constructor.owner != null) {
      out.append("((").append(source(constructor.owner)).append(") null).");
    }
    out.append("super(");
    for (int i = 0; i < constructor.arguments.size(); i++) {
      if (i != 0) {
        out.append(", ");
      }
      JniSignature.Value value = constructor.arguments.get(i);
      if (value == null) {
        out.append("null");
      } else if (value.erase().getSort() == Type.OBJECT || value.erase().getSort() == Type.ARRAY) {
        out.append('(').append(source(value)).append(") null");
      } else {
        out.append(defaultValue(value.erase()));
      }
    }
    out.append("); }\n");
    sourceVariables = enclosingVariables;
    sourceFormals = enclosingFormals;
    sourceScope();
  }

  private JniConstructorResolver constructorResolver() {
    return new JniConstructorResolver(new JniConstructorResolver.Types() {
      public List<JniSignature.Value> parents(JniSignature.Value type) throws IOException {
        return parentTypes(view(type));
      }

      public List<List<JniSignature.Value>> parameterBounds(JniSignature.Value type) throws IOException {
        JniClass model = metadata.resolve(type.name);
        Map<String, JniSignature.Value> enclosing = enclosingVariables(model);
        Map<String, JniSignature.Value> scope = new HashMap<String, JniSignature.Value>(enclosing);
        scope.putAll(signature(model).bind(type.arguments, enclosing));
        List<List<JniSignature.Value>> result = new ArrayList<List<JniSignature.Value>>();
        for (List<JniSignature.Value> bounds : signature(model).bounds.values()) {
          List<JniSignature.Value> values = new ArrayList<JniSignature.Value>();
          for (JniSignature.Value bound : bounds) {
            values.add(sourceType(bound.substitute(scope)));
          }
          result.add(values);
        }
        return result;
      }

      private Map<String, JniSignature.Value> enclosingVariables(JniClass model) throws IOException {
        Map<String, JniSignature.Value> scope = new HashMap<String, JniSignature.Value>();
        if (model.innerInstance()) {
          JniClass owner = metadata.resolve(model.outer());
          scope.putAll(enclosingVariables(owner));
          // javac's capture bounds substitute local formals, but retain the
          // enclosing declaration's variables, distinct from owner captures.
          scope.putAll(signature(owner).variables(scope, "#owner:" + owner.name + ":"));
        }
        return scope;
      }

      public boolean isInterface(String name) throws IOException {
        return metadata.resolve(name).isInterface();
      }

      public boolean isFinal(String name) throws IOException {
        JniClass model = metadata.resolve(name);
        return declarations.containsKey(name) ? model.isEnum() || model.isRecord()
            : (model.access & Opcodes.ACC_FINAL) != 0;
      }
    });
  }

  private List<JniConstructorResolver.Candidate> constructorOverloads(TypeView parent, JniClass model, int arity)
      throws IOException {
    List<JniConstructorResolver.Candidate> result = new ArrayList<JniConstructorResolver.Candidate>();
    int skip = parent.model.innerInstance() ? 1 : 0;
    for (JniClass.Method original : parent.model.constructors) {
      Type[] descriptor = Type.getArgumentTypes(original.descriptor);
      if (descriptor.length - skip != arity || !accessible(original.access, parent.model, model, true)) {
        continue;
      }
      JniClass.Method method = parent.method(original);
      JniSignature source = method.source;
      List<JniSignature.Value> parameters = new ArrayList<JniSignature.Value>();
      int offset = source == null ? 0 : descriptor.length - source.parameters.size();
      for (int i = skip; i < descriptor.length; i++) {
        parameters.add(
            source == null ? JniSignature.Value.type(descriptor[i]) : sourceType(source.parameters.get(i - offset)));
      }
      Map<String, List<JniSignature.Value>> bounds = new java.util.LinkedHashMap<String, List<JniSignature.Value>>();
      if (source != null) {
        for (Map.Entry<String, List<JniSignature.Value>> formal : source.bounds.entrySet()) {
          List<JniSignature.Value> values = new ArrayList<JniSignature.Value>();
          for (JniSignature.Value bound : formal.getValue()) {
            values.add(sourceType(bound));
          }
          bounds.put(formal.getKey(), values);
        }
      }
      List<JniSignature.Value> exceptions = new ArrayList<JniSignature.Value>();
      if (source != null && !source.exceptions.isEmpty()) {
        exceptions.addAll(source.exceptions);
      } else {
        for (String exception : method.exceptions) {
          exceptions.add(JniSignature.Value.object(exception));
        }
      }
      result.add(new JniConstructorResolver.Candidate(original.descriptor, parameters, bounds, exceptions));
    }
    return result;
  }

  private void inferConstructorArguments(JniSignature source, List<JniSignature.Value> arguments, JniClass model)
      throws IOException {
    Map<String, List<JniSignature.Value>> bounds = source == null ? JniSignature.read(null).bounds
        : source.constructorFormals().bounds;
    Set<String> hidden = new HashSet<String>();
    int previous;
    do {
      previous = hidden.size();
      for (Map.Entry<String, List<JniSignature.Value>> formal : bounds.entrySet()) {
        for (JniSignature.Value bound : formal.getValue()) {
          Set<String> variables = new HashSet<String>();
          bound.variables(variables);
          if (!Collections.disjoint(hidden, variables)
              || constructorTypeNames(sourceType(bound), null, model, names(model)) == null) {
            hidden.add(formal.getKey());
          }
        }
      }
    } while (hidden.size() != previous);
    JniSourceNames spellings = names(model).copy();
    for (int i = 0; i < arguments.size(); i++) {
      JniSignature.Value argument = arguments.get(i);
      Set<String> variables = new HashSet<String>();
      argument.variables(variables);
      JniSourceNames trial = Collections.disjoint(hidden, variables)
          ? constructorTypeNames(argument, source, model, spellings)
          : null;
      if (trial != null) {
        spellings = trial;
        continue;
      }
      JniSignature.Value element = argument;
      while (element.component != null) {
        element = element.component;
      }
      JniSignature.Value erased = JniSignature.Value.type(argument.erase());
      // A raw List cast can hide List<Private>, but a variable's erasure cannot
      // express its intersection bounds. Leave that argument to null inference.
      trial = element.variable == null ? constructorTypeNames(erased, null, model, spellings) : null;
      arguments.set(i, trial == null ? null : erased);
      if (trial != null) {
        spellings = trial;
      }
    }
  }

  private JniSourceNames constructorTypeNames(JniSignature.Value type, JniSignature source, JniClass model,
      JniSourceNames names)
      throws IOException {
    Set<String> required = new TreeSet<String>();
    type.classNames(required);
    if (source != null) {
      for (List<JniSignature.Value> bounds : source.constructorFormals(Collections.singletonList(type)).bounds
          .values()) {
        for (JniSignature.Value bound : bounds) {
          sourceType(bound).classNames(required);
        }
      }
    }
    for (String name : required) {
      metadata.sourceName(name);
    }
    JniSourceNames trial = names.copy();
    try {
      trial.reserve(required);
      if (source != null) {
        trial.formals(source);
      }
      type.source(metadata, trial);
      if (source != null) {
        for (List<JniSignature.Value> bounds : source.constructorFormals(Collections.singletonList(type)).bounds
            .values()) {
          for (JniSignature.Value bound : bounds) {
            sourceType(bound).source(metadata, trial);
          }
        }
      }
      return trial;
    } catch (IOException unexpressible) {
      // Accessibility is insufficient: a mandatory import or a lexical type can
      // hide an otherwise public parameter. Try a raw cast or uncast null; the
      // overload resolver still has to prove that the resulting call is unique.
      return null;
    }
  }

  private JniSignature constructorSignature(JniSignature source, JniSignature.Value parent, JniClass model)
      throws IOException {
    Set<String> types = new HashSet<String>();
    parent.classNames(types);
    for (List<JniSignature.Value> bounds : source.constructorFormals().bounds.values()) {
      for (JniSignature.Value bound : bounds) {
        sourceType(bound).classNames(types);
      }
    }
    for (JniSignature.Value parameter : source.parameters) {
      sourceType(parameter).classNames(types);
    }
    Set<String> reserved = new HashSet<String>();
    reserved.add("java"); // Qualified platform types.
    reserved.add(model.simple());
    // Specialized parameters still use the declaration's original variables,
    // including enclosing formals. An intermediate alias must not capture them.
    reserved.addAll(declarationFormals(model).bounds.keySet());
    for (String name : types) {
      Collections.addAll(reserved, metadata.sourceName(name).split("\\."));
    }
    // Constructor formals have a new scope: their original names may capture a
    // specialized class name or even a package prefix in a qualified source name.
    return source.renameConstructor(reserved);
  }

  private boolean constructorTypeAccessible(String name, JniClass model) throws IOException {
    // Test the same spelling rules used for declarations, including inherited
    // bindings and aliases. A rejected candidate must not retain its imports.
    // Missing definitions are not inaccessible types: keep them as candidate
    // diagnostics rather than allowing an untyped null to reach javac.
    metadata.sourceName(name);
    try {
      names(model).copy().name(name);
      return true;
    } catch (IOException inaccessible) {
      return false;
    }
  }

  private boolean accessible(int access, JniClass owner, JniClass context, boolean constructor) throws IOException {
    if ((access & Opcodes.ACC_PUBLIC) != 0) {
      return true;
    }
    if ((access & Opcodes.ACC_PRIVATE) != 0) {
      return root(owner).name.equals(root(context).name);
    }
    if (owner.packageName().equals(context.packageName())) {
      return true;
    }
    if ((access & Opcodes.ACC_PROTECTED) == 0) {
      return false;
    }
    if (constructor) {
      return true;
    }
    // A protected member type is accessible in a subclass of its declaring outer.
    for (JniClass scope = context; scope != null; scope = scope.outer() == null ? null
        : metadata.resolve(scope.outer())) {
      for (JniClass ancestor = scope; ancestor != null; ancestor = ancestor.parent == null ? null
          : metadata.resolve(ancestor.parent)) {
        if (ancestor.name.equals(owner.outer())) {
          return true;
        }
      }
    }
    return false;
  }

  private String defaultValue(Type type) throws IOException {
    if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
      return "(" + type(type) + ") null";
    }
    if (type.getSort() == Type.BOOLEAN) {
      return "false";
    }
    return "(" + type(type) + ") 0";
  }

  private void emitFormals(JniSignature signature, StringBuilder out) throws IOException {
    if (signature.bounds.isEmpty()) {
      return;
    }
    out.append('<');
    int parameter = 0;
    for (Map.Entry<String, List<JniSignature.Value>> formal : signature.bounds.entrySet()) {
      if (parameter++ != 0) {
        out.append(", ");
      }
      out.append(sourceVariables.containsKey(formal.getKey()) ? sourceVariables.get(formal.getKey()) : formal.getKey());
      for (int i = 0; i < formal.getValue().size(); i++) {
        out.append(i == 0 ? " extends " : " & ").append(source(sourceType(formal.getValue().get(i))));
      }
    }
    out.append('>');
  }

  private void emit(JniClass model, StringBuilder out, String indent) throws IOException {
    // Formals occur in declaration clauses and the body. Reserve spellings in
    // each lexical scope before choosing names, including supporting methods.
    sourceVariables = model.innerInstance() ? new HashMap<String, String>(sourceVariables)
        : new HashMap<String, String>();
    sourceFormals = declarationFormals(model);
    sourceNames = names(model);
    sourceScope();
    Set<String> bodyNames = sourceNames.boundNames();
    if (genericDeclaration(model)) {
      List<JniClass.Method> methods = new ArrayList<JniClass.Method>();
      if (interfaceMethods.containsKey(model.name)) {
        methods.addAll(interfaceMethods.get(model.name));
      }
      if (targets.contains(model.name)) {
        methods.addAll(model.natives);
      }
      for (JniClass.Method method : methods) {
        JniSignature used = JniSignature.read(method.signature == null ? method.descriptor : method.signature);
        bodyNames.addAll(sourceNames.signatureNames(sourceSignature(used), Collections.<String> emptyList()));
      }
    }
    sourceNames = names(model, false);
    // Changing lexical scope clears allocator reservations. Retain enclosing
    // fresh names before allocating this declaration's own type parameters.
    sourceScope();
    if (genericDeclaration(model)) {
      JniSignature used = sourceSignature(signature(model));
      used.parents.clear();
      if (model.parent != null) {
        used.parents.add(emittedSuperclass(declarationView(model)));
      }
      used.parents.addAll(emittedInterfaces(declarationView(model)));
      sourceVariables.putAll(sourceNames.variables(used, "_NarType", Collections.<String> emptyList(), bodyNames));
    }
    sourceScope();
    classVariables.put(model.name, new HashMap<String, String>(sourceVariables));
    out.append(indent).append(access(model.nesting == null ? model.access : model.nesting.access));
    if (model.outer() != null && (model.nesting.access & Opcodes.ACC_STATIC) != 0) {
      out.append("static ");
    }
    if (!model.isEnum() && !model.isRecord() && hasSealedParent(model)) {
      out.append("non-sealed ");
    }
    if (model.isInterface()) {
      out.append("interface ");
    } else if (model.isEnum()) {
      out.append("enum ");
    } else if (model.isRecord()) {
      out.append("record ");
    } else {
      out.append("abstract class ");
    }
    out.append(model.simple());
    if (genericDeclaration(model)) {
      emitFormals(signature(model), out);
    }
    if (model.isRecord()) {
      out.append("()");
    }
    if (!model.isInterface() && !model.isEnum() && !model.isRecord() && model.parent != null
        && !"java/lang/Object".equals(model.parent)) {
      out.append(" extends ").append(source(emittedSuperclass(declarationView(model))));
    }
    List<JniSignature.Value> interfaces = emittedInterfaces(declarationView(model));
    for (int i = 0; i < interfaces.size(); i++) {
      out.append(i == 0 ? (model.isInterface() ? " extends " : " implements ") : ", ")
          .append(source(interfaces.get(i)));
    }
    out.append(" {\n");
    sourceNames = names(model);
    sourceScope();
    if (model.isEnum()) {
      out.append(indent).append("  ;\n");
    }
    for (JniClass.Field field : model.constants) {
      out.append(indent).append("  ").append(access(field.access)).append("static final ")
          .append(type(Type.getType(field.descriptor))).append(' ').append(field.name).append(" = ")
          .append(literal(field)).append(";\n");
    }
    if (!planningDeclarations && !model.isInterface() && !model.isEnum() && !model.isRecord()) {
      emitConstructor(model, constructors.get(model.name), out, indent);
    }
    if (interfaceMethods.containsKey(model.name)) {
      for (JniClass.Method method : interfaceMethods.get(model.name)) {
        emitMethod(model, method, false, out, indent);
      }
    }
    if (targets.contains(model.name)) {
      for (JniClass.Method method : model.natives) {
        emitMethod(model, method, true, out, indent);
      }
      if (model.natives.isEmpty()) {
        String marker = "__nar_header";
        Set<String> fields = new HashSet<String>();
        for (JniClass.Field field : model.constants) {
          fields.add(field.name);
        }
        while (fields.contains(marker)) {
          marker += "_";
        }
        out.append(indent).append("  @").append(sourceNames.name("java/lang/annotation/Native")).append(' ');
        if (model.isInterface()) {
          out.append("int ").append(marker).append(" = new ").append(sourceNames.name("java/lang/Object"))
              .append("().hashCode()");
        } else {
          if (model.isRecord()) {
            out.append("static ");
          }
          out.append("int ").append(marker);
        }
        out.append(";\n");
      }
    }
    Map<String, String> enclosingVariables = sourceVariables;
    for (JniClass child : declarations.values()) {
      if (model.name.equals(child.outer())) {
        sourceVariables = enclosingVariables;
        emit(child, out, indent + "  ");
      }
    }
    out.append(indent).append("}\n");
  }

  private void emitMethod(JniClass model, JniClass.Method method, boolean nativeMethod, StringBuilder out,
      String indent)
      throws IOException {
    boolean concrete = !nativeMethod && (model.isEnum() || model.isRecord());
    out.append(indent).append("  ").append(nativeMethod ? access(method.access) : "public ");
    if (nativeMethod) {
      if ((method.access & Opcodes.ACC_STATIC) != 0) {
        out.append("static ");
      }
      out.append("native ");
    } else if (!concrete) {
      out.append("abstract ");
    }
    JniSignature signature = method.signature == null || (nativeMethod && !genericDeclaration(model)) ? null
        : JniSignature.read(method.signature);
    Map<String, String> enclosingVariables = sourceVariables;
    JniSignature enclosingFormals = sourceFormals;
    sourceFormals = JniSignature.read(null);
    sourceFormals.bounds.putAll(enclosingFormals.bounds);
    if (signature != null) {
      sourceFormals.bounds.putAll(sourceSignature(signature).bounds);
    }
    sourceVariables = new HashMap<String, String>(enclosingVariables);
    sourceScope();
    if (signature != null) {
      sourceVariables.putAll(sourceNames.variables(sourceSignature(signature), "_NarMethod"));
    }
    sourceScope();
    if (signature != null && !signature.bounds.isEmpty()) {
      emitFormals(signature, out);
      out.append(' ');
    }
    out.append(signature == null ? type(Type.getReturnType(method.descriptor)) : source(sourceType(signature.result)))
        .append(' ').append(method.name).append('(');
    Type[] arguments = Type.getArgumentTypes(method.descriptor);
    for (int i = 0; i < arguments.length; i++) {
      if (i > 0) {
        out.append(", ");
      }
      out.append(signature == null ? type(arguments[i]) : source(sourceType(signature.parameters.get(i)))).append(" p")
          .append(i);
    }
    if (concrete) {
      out.append(") { throw new ").append(sourceNames.name("java/lang/AssertionError")).append("(); }\n");
    } else {
      out.append(");\n");
    }
    sourceVariables = enclosingVariables;
    sourceFormals = enclosingFormals;
    sourceScope();
  }

  private static String access(int flags) {
    if ((flags & Opcodes.ACC_PUBLIC) != 0) {
      return "public ";
    }
    if ((flags & Opcodes.ACC_PROTECTED) != 0) {
      return "protected ";
    }
    if ((flags & Opcodes.ACC_PRIVATE) != 0) {
      return "private ";
    }
    return "";
  }

  private static String literal(JniClass.Field field) {
    Object value = field.value;
    switch (field.descriptor.charAt(0)) {
      case 'Z':
        return ((Integer) value) == 0 ? "false" : "true";
      case 'C':
        return "(char) " + value;
      case 'J':
        return value + "L";
      case 'F': {
        float number = (Float) value;
        if (Float.isNaN(number)) {
          return "(0.0f / 0.0f)";
        }
        if (Float.isInfinite(number)) {
          return number > 0 ? "(1.0f / 0.0f)" : "(-1.0f / 0.0f)";
        }
        return Float.toHexString(number) + "f";
      }
      case 'D': {
        double number = (Double) value;
        if (Double.isNaN(number)) {
          return "(0.0d / 0.0d)";
        }
        if (Double.isInfinite(number)) {
          return number > 0 ? "(1.0d / 0.0d)" : "(-1.0d / 0.0d)";
        }
        return Double.toHexString(number) + "d";
      }
      default:
        return value.toString();
    }
  }

  static String header(String name) {
    return name.replace('/', '_').replace('$', '_') + ".h";
  }

  private static String[] strings(Set<?> values) {
    List<String> result = new ArrayList<String>();
    if (values != null) {
      for (Object value : values) {
        if (value != null) {
          result.add(value.toString());
        }
      }
    }
    return result.toArray(new String[result.size()]);
  }

  private static String path(List<File> entries) {
    StringBuilder result = new StringBuilder();
    for (File entry : entries) {
      if (result.length() != 0) {
        result.append(File.pathSeparator);
      }
      result.append(entry.getAbsolutePath());
    }
    return result.toString();
  }

  private static String run(List<String> command) throws IOException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (InputStream stream = process.getInputStream()) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = stream.read(buffer)) != -1) {
        output.write(buffer, 0, count);
      }
    }
    try {
      int status = process.waitFor();
      String diagnostics = new String(output.toByteArray(), StandardCharsets.UTF_8);
      if (status != 0) {
        throw new IOException(command.get(0) + " failed (exit " + status + "):\n" + diagnostics);
      }
      return diagnostics;
    } catch (InterruptedException ex) {
      process.destroy();
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while running " + command.get(0), ex);
    }
  }
}
