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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The small portion of a class file needed for JNI declarations. No class
 * loading.
 */
final class JniClass extends ClassVisitor {
  String name;
  String parent;
  String signature;
  final List<String> interfaces = new ArrayList<String>();
  int access;
  boolean platform;
  boolean local;
  boolean sealed;
  Member nesting;
  final Map<String, Member> members = new LinkedHashMap<String, Member>();
  final List<Field> constants = new ArrayList<Field>();
  final List<Method> natives = new ArrayList<Method>();
  final List<Method> instanceMethods = new ArrayList<Method>();
  final List<Method> constructors = new ArrayList<Method>();

  JniClass() {
    super(Opcodes.ASM9);
  }

  @Override
  public void visit(int version, int flags, String binaryName, String signature, String superName,
      String[] interfaces) {
    name = binaryName;
    parent = superName;
    this.signature = signature;
    if (interfaces != null) {
      java.util.Collections.addAll(this.interfaces, interfaces);
    }
    access = flags;
  }

  @Override
  public void visitOuterClass(String owner, String method, String descriptor) {
    local = true;
  }

  @Override
  public void visitPermittedSubclass(String subclass) {
    sealed = true;
  }

  @Override
  public void visitInnerClass(String binaryName, String outer, String simple, int flags) {
    Member entry = new Member(outer, simple, flags);
    Member old = members.put(binaryName, entry);
    if (old != null && !old.same(entry)) {
      throw new IllegalArgumentException("Conflicting InnerClasses entries for " + binaryName);
    }
    if (name.equals(binaryName)) {
      nesting = entry;
    }
  }

  @Override
  public FieldVisitor visitField(int flags, String field, String descriptor, String signature, Object value) {
    int mask = Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
    if ((flags & mask) == mask && value != null && descriptor.length() == 1) {
      constants.add(new Field(flags, field, descriptor, value));
    }
    return null;
  }

  @Override
  public MethodVisitor visitMethod(int flags, String method, String descriptor, String signature, String[] exceptions) {
    Method entry = new Method(flags, method, descriptor, signature, null,
        exceptions == null ? java.util.Collections.<String> emptyList() : java.util.Arrays.asList(exceptions));
    if ((flags & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0 && !method.startsWith("<")) {
      instanceMethods.add(entry);
    }
    if ((flags & Opcodes.ACC_NATIVE) != 0) {
      natives.add(entry);
    }
    if ("<init>".equals(method)) {
      constructors.add(entry);
    }
    return null;
  }

  String outer() {
    return nesting == null ? null : nesting.outer;
  }

  String simple() {
    return nesting == null ? name.substring(name.lastIndexOf('/') + 1) : nesting.simple;
  }

  String packageName() {
    int end = name.lastIndexOf('/');
    return end < 0 ? "" : name.substring(0, end);
  }

  boolean isInterface() {
    return (access & Opcodes.ACC_INTERFACE) != 0;
  }

  boolean isEnum() {
    return (access & Opcodes.ACC_ENUM) != 0;
  }

  boolean isRecord() {
    return (access & Opcodes.ACC_RECORD) != 0;
  }

  boolean innerInstance() {
    return outer() != null && (nesting.access & Opcodes.ACC_STATIC) == 0;
  }

  static final class Member {
    final String outer;
    final String simple;
    final int access;

    Member(String outer, String simple, int access) {
      this.outer = outer;
      this.simple = simple;
      this.access = access;
    }

    boolean same(Member other) {
      return java.util.Objects.equals(outer, other.outer) && java.util.Objects.equals(simple, other.simple)
          && access == other.access;
    }
  }

  static class Method {
    final int access;
    final String name;
    final String descriptor;
    final String signature;
    // Specialized source types retain bounds from enclosing generic declarations.
    final JniSignature source;
    final List<String> exceptions;

    Method(int access, String name, String descriptor) {
      this(access, name, descriptor, null);
    }

    Method(int access, String name, String descriptor, String signature) {
      this(access, name, descriptor, signature, null);
    }

    Method(int access, String name, String descriptor, String signature, JniSignature source) {
      this(access, name, descriptor, signature, source, java.util.Collections.<String> emptyList());
    }

    Method(int access, String name, String descriptor, String signature, JniSignature source, List<String> exceptions) {
      this.access = access;
      this.name = name;
      this.descriptor = descriptor;
      this.signature = signature;
      this.exceptions = exceptions;
      this.source = source;
    }
  }

  static final class Field extends Method {
    final Object value;

    Field(int access, String name, String descriptor, Object value) {
      super(access, name, descriptor);
      this.value = value;
    }
  }
}
