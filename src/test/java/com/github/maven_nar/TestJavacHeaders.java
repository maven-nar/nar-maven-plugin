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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.ToolProvider;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.FileUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import junit.framework.TestCase;

/**
 * Compares regenerated headers with javac's headers from the original sources.
 */
public class TestJavacHeaders extends TestCase {
  private File work;
  private File classes;
  private File expected;
  private File actual;

  @Override
  protected void setUp() throws Exception {
    work = Files.createTempDirectory("nar header parity ").toFile();
    classes = directory("classes");
    expected = directory("expected");
    actual = new File(work, "actual");
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtils.deleteDirectory(work);
  }

  public void testInheritedOverrideMustMeetSuperclassAndInterface() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { public CharSequence value(){return null;} }",
        "Middle.java",
        "public class Middle extends Base { public String value(){return null;} public native void middle(); }",
        "Contract.java", "public interface Contract { java.io.Serializable value(); }", "Api.java",
        "public class Api extends Middle implements Contract { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedOverrideMustMeetSuperclassAndDefault() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { public CharSequence value(){return null;} }",
        "Middle.java",
        "public class Middle extends Base { public String value(){return null;} public native void middle(); }",
        "Contract.java", "public interface Contract { default java.io.Serializable value(){return null;} }", "Api.java",
        "public class Api extends Middle implements Contract { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUncheckedConstructorDoesNotNeedThrowable() throws Exception {
    compile(classes, expected, "Throwable.java", "public class Throwable {}", "Base.java",
        "public class Base { protected Base() throws RuntimeException {} }", "java.java",
        "public class java extends Base { public native void call(Throwable t); public native void call(int i); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCheckedConstructorCanUseNarrowException() throws Exception {
    compile(classes, expected, "Throwable.java", "public class Throwable {}", "Base.java",
        "import java.io.IOException; public class Base { protected Base() throws IOException {} }", "java.java",
        "import java.io.IOException; public class java extends Base { public java() throws IOException {} public native void call(Throwable t); public native void call(int i); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericUncheckedConstructorDoesNotNeedThrowable() throws Exception {
    compile(classes, expected, "Throwable.java", "public class Throwable {}", "Base.java",
        "public class Base<E extends Exception> { protected Base() throws E {} }", "java.java",
        "public class java extends Base<RuntimeException> { public native void call(Throwable t); public native void call(int i); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericSuperclassExceptionNarrowingControl() throws Exception {
    compile(classes, expected, "Base.java", "public class Base<E extends Exception> { protected Base() throws E {} }",
        "Api.java", "public class Api extends Base<RuntimeException> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMultipleRetainedInterfaceReturnsControl() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { public CharSequence value(){return null;} }",
        "Middle.java",
        "public class Middle extends Base { public String value(){return null;} public native void middle(); }",
        "Left.java", "public interface Left { CharSequence value(); }", "Right.java",
        "public interface Right { java.io.Serializable value(); }", "Api.java",
        "public class Api extends Middle implements Left, Right { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testReviewGenericCheckedConstructor() throws Exception {
    compile(classes, expected, "Throwable.java", "public class Throwable {}", "Base.java",
        "public class Base<E extends Exception> { protected Base() throws E {} }", "java.java",
        "import java.io.IOException; public class java extends Base<IOException> {"
            + " public java() throws IOException {} public native void call(Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testReviewPrivateCheckedConstructor() throws Exception {
    compile(classes, expected, "Throwable.java", "public class Throwable {}", "Base.java",
        "import java.io.IOException; public class Base {"
            + " private static class Hidden extends IOException {} protected Base() throws Hidden {} }",
        "java.java", "import java.io.IOException; public class java extends Base {"
            + " public java() throws IOException {} public native void call(Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testReviewConstructorExceptionWideningPropagates() throws Exception {
    compile(classes, expected, "Throwable.java", "public class Throwable {}", "IOException.java",
        "public class IOException {}", "Base.java",
        "import java.io.IOException; public class Base { protected Base() throws IOException {} }", "java.java",
        "public class java extends Base { public java() throws Exception {}"
            + " public native void call(Throwable t, IOException e); }",
        "Api.java", "public class Api extends java { public Api() throws Exception {} public native void api(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testReviewConstructorFormalDoesNotHideException() throws Exception {
    compile(classes, expected, "_NarConstructor0.java", "public class _NarConstructor0 extends Exception {}",
        "Base.java", "public class Base { protected <T> Base(java.util.List<T> values) throws _NarConstructor0 {} }",
        "Api.java", "public class Api extends Base { public Api() throws _NarConstructor0 { super(null); }"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testReviewGenericSuperclassReturnContract() throws Exception {
    compile(classes, expected, "Base.java", "public class Base<T> { public T value(){return null;} }", "Middle.java",
        "public class Middle extends Base<CharSequence> { public String value(){return null;} public native void middle(); }",
        "Contract.java", "public interface Contract { java.io.Serializable value(); }", "Api.java",
        "public class Api extends Middle implements Contract { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorFormalDoesNotHideQualifiedSuperOwner() throws Exception {
    compile(classes, expected, "_NarConstructor0.java",
        "public class _NarConstructor0 { public class Base {" + " protected <T> Base(java.util.List<T> values) {} } }",
        "Api.java", "public class Api extends _NarConstructor0.Base {"
            + " public Api(_NarConstructor0 owner) { owner.super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testOverrideLostInGeneratedSuperclass() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { public Object value(){return null;} }",
        "p/Contract.java", "package p; public interface Contract { String value(); }", "p/Middle.java",
        "package p; public class Middle extends Base { public String value(){return null;} public native void middle(); }",
        "p/Api.java", "package p; public class Api extends Middle implements Contract { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testOverrideLostInGeneratedSuperclassDefault() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { protected String value(){return null;} }",
        "p/Contract.java", "package p; public interface Contract { default String value(){return null;} }",
        "p/Middle.java",
        "package p; public class Middle extends Base { public String value(){return null;} public native void middle(); }",
        "p/Api.java", "package p; public class Api extends Middle implements Contract { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSpecializedUncheckedExceptionDoesNotNeedOverride() throws Exception {
    compile(classes, expected, "p/Missing.java", "package p; public class Missing {}", "p/Base.java",
        "package p; public class Base<E extends Exception> { public Object value() throws E {return null;} }",
        "p/Contract.java", "package p; public interface Contract { Object value(); }", "p/Api.java",
        "package p; public class Api extends Base<RuntimeException> implements Contract { public Missing value(){return null;} public native void call(); }");
    Files.delete(new File(classes, "p/Missing.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSpecializedCheckedExceptionDoesNotNeedOverride() throws Exception {
    compile(classes, expected, "p/Missing.java", "package p; public class Missing {}", "p/Base.java",
        "package p; public class Base<E extends Exception> { public Object value() throws E {return null;} }",
        "p/Contract.java", "package p; public interface Contract { Object value() throws java.io.IOException; }",
        "p/Api.java",
        "package p; public class Api extends Base<java.io.IOException> implements Contract { public Missing value(){return null;} public native void call(); }");
    Files.delete(new File(classes, "p/Missing.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPackageShadowByPeerClass() throws Exception {
    compile(classes, expected, "p/java.java", "package p; public class java {}", "p/Api.java",
        "package p; public class Api { public native String call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericOverrideSignatureWithInheritedExceptions() throws Exception {
    compile(classes, expected, "p/Base.java",
        "package p; public class Base<E extends Exception> { public Object value() throws E {return null;} }",
        "p/Contract.java", "package p; public interface Contract { String value(); }", "p/Api.java",
        "package p; public class Api extends Base<RuntimeException> implements Contract { public String value(){return null;} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testReconstructedSuperclassStillNeedsSubclassOverride() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { public native Object value(); }",
        "p/Contract.java", "package p; public interface Contract { String value(); }", "p/Api.java",
        "package p; public class Api extends Base implements Contract { public String value(){return null;} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testImportedThrowableDoesNotChangeNativeDescriptor() throws Exception {
    compile(classes, expected, "Throwable.java", "public class Throwable {}", "java.java",
        "public class java { public native void call(Throwable value); public native void call(int value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPackageShadowByPeerInDependencyJar() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "p/java.java", "package p; public class java {}");
    File jar = jar(dependency, null, false);
    compileWithPath(classes, expected, Arrays.asList(jar), "p/Api.java",
        "package p; public class Api { public native String call(); }");
    generate(classes, Arrays.asList(classes, jar), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedIntersectionReturn() throws Exception {
    compile(classes, expected, "p/Left.java", "package p; public interface Left {}", "p/Right.java",
        "package p; public interface Right {}", "p/Both.java", "package p; public class Both implements Left, Right {}",
        "p/A.java", "package p; public interface A { Left value(); }", "p/B.java",
        "package p; public interface B { Right value(); }", "p/Base.java",
        "package p; public class Base { public Object value(){return null;} }", "p/Middle.java",
        "package p; public class Middle extends Base { public Both value(){return null;} public native void middle(); }",
        "p/Api.java", "package p; public class Api extends Middle implements A, B { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testNativeOverloadedOnlyByNonNativeMethod() throws Exception {
    compile(classes, expected, "Api.java",
        "public class Api { public native void call(int value); public void call(String value) {} }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUnicodeClassAndNativeMethod() throws Exception {
    compile(classes, expected, "Api\u00e9.java",
        "public class Api\u00e9 { public native void m\u00e9thode(int value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPublicMemberInheritedFromHiddenDeclaringClass() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase extends HiddenBase {}",
        "q/Api.java", "package q; public class Api extends p.PublicBase { public native Arg call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMemberOfPrivateDeclaringClassThroughPublicParent() throws Exception {
    compile(classes, expected, "p/Container.java",
        "package p; public class Container { private static class HiddenBase { public static class Arg {} } public static class PublicBase extends HiddenBase {} }",
        "q/Api.java",
        "package q; public class Api extends p.Container.PublicBase { public native Arg call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMemberInheritedFromPublicDeclaringClassControl() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { public static class Arg {} }",
        "p/PublicBase.java", "package p; public class PublicBase extends Base {}", "q/Api.java",
        "package q; public class Api extends p.PublicBase { public native Arg call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMemberInheritanceThroughPackageBoundaryControl() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { static class Arg {} }", "q/Middle.java",
        "package q; public class Middle extends p.Base {}", "p/Arg.java", "package p; public class Arg {}",
        "p/Api.java", "package p; public class Api extends q.Middle { public native Arg call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testHiddenGrandparentMemberControl() throws Exception {
    compile(classes, expected, "Types.java", "public interface Types { class Arg {} }", "MoreTypes.java",
        "public interface MoreTypes extends Types { class Arg {} }", "Base.java",
        "public class Base implements MoreTypes {}", "Api.java",
        "public class Api extends Base { public native Arg call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedMemberDiamondControl() throws Exception {
    compile(classes, expected, "Types.java", "public interface Types { class Arg {} }", "Left.java",
        "public interface Left extends Types {}", "Right.java", "public interface Right extends Types {}", "Api.java",
        "public class Api implements Left, Right { public native Arg call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifiedInheritedPublicMember() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase extends HiddenBase {}",
        "q/Api.java", "package q; public class Api { public native p.PublicBase.Arg call(p.PublicBase.Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifiedInheritedMemberWithOwnShadow() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase extends HiddenBase {}",
        "q/Api.java",
        "package q; public class Api extends p.PublicBase { public static class Arg {} public native Arg local(Arg value); public native p.PublicBase.Arg call(p.PublicBase.Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedPublicMemberInClassHeader() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase extends HiddenBase {}",
        "q/Api.java",
        "package q; public class Api extends p.PublicBase implements java.util.function.Supplier<Api.Arg> { public Arg get() { return null; } public native Arg call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedPublicConstructorArgument() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} public static class Other {} } public class PublicBase extends HiddenBase { protected PublicBase(Arg x) {} protected PublicBase(Other x) {} }",
        "q/Api.java",
        "package q; public class Api extends p.PublicBase { public Api() { super((Arg) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateDeclaringClassConstructorArgument() throws Exception {
    compile(classes, expected, "p/Container.java",
        "package p; public class Container { private static class HiddenBase { public static class Arg {} public static class Other {} } public static class PublicBase extends HiddenBase { protected PublicBase(Arg x) {} protected PublicBase(Other x) {} } }",
        "q/Api.java",
        "package q; public class Api extends p.Container.PublicBase { public Api() { super((Arg) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPublicDeclaringClassConstructorControl() throws Exception {
    compile(classes, expected, "p/Base.java",
        "package p; public class Base { public static class Arg {} public static class Other {} }", "p/PublicBase.java",
        "package p; public class PublicBase extends Base { protected PublicBase(Arg x) {} protected PublicBase(Other x) {} }",
        "q/Api.java",
        "package q; public class Api extends p.PublicBase { public Api() { super((Arg) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedNestedMemberLexicalControl() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Types { public static class Arg {} } } public class PublicBase extends HiddenBase {}",
        "q/Api.java",
        "package q; public class Api extends p.PublicBase { public native Types.Arg call(Types.Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifiedPublicDeclaringClassControl() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { public static class Arg {} }",
        "p/PublicBase.java", "package p; public class PublicBase extends Base {}", "q/Api.java",
        "package q; public class Api { public native p.PublicBase.Arg call(p.PublicBase.Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifierFromDependencyDirectoryIgnoresUnrelatedTypes() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase extends HiddenBase {}",
        "Missing.java", "class Missing {}", "Unrelated.java",
        "class Unrelated extends Missing { Missing value; Missing call() { return null; } }");
    Files.delete(new File(dependency, "Missing.class").toPath());
    compileWithPath(classes, expected, Arrays.asList(dependency), "q/Api.java",
        "package q; public class Api { public native p.PublicBase.Arg call(p.PublicBase.Arg value); }");
    generate(classes, Arrays.asList(classes, dependency), Collections.<String> emptySet(),
        Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifierFromDependencyJarOutsideDeclaringPackage() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "p/Container.java",
        "package p; public class Container { protected static class HiddenBase { public static class Arg {} } }",
        "q/PublicBase.java",
        "package q; public class PublicBase extends p.Container { public static class Exposed extends HiddenBase {} }");
    File jar = jar(dependency, null, false);
    compileWithPath(classes, expected, Arrays.asList(jar), "r/Api.java",
        "package r; public class Api { public native q.PublicBase.Exposed.Arg call(q.PublicBase.Exposed.Arg value); }");
    generate(classes, Arrays.asList(classes, jar), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifierFromManifestDependency() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase extends HiddenBase {}");
    jar("dependency.jar", dependency, "library.jar");
    File library = jar("library.jar", directory("empty"), "dependency.jar");
    compileWithPath(classes, expected, Arrays.asList(library), "q/Api.java",
        "package q; public class Api { public native p.PublicBase.Arg call(p.PublicBase.Arg value); }");
    generate(classes, Arrays.asList(classes, library), Collections.<String> emptySet(),
        Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifierFromMultiReleaseJar() throws Exception {
    File base = directory("base");
    File versioned = directory("versioned");
    String hidden = "package p; class HiddenBase { public static class Arg {} }";
    compile(base, null, "p/PublicBase.java", hidden + " public class PublicBase extends HiddenBase {}",
        "p/Visible.java", "package p; public class Visible {}");
    compile(versioned, null, "p/PublicBase.java", hidden + " public class PublicBase {}", "p/Visible.java",
        "package p; public class Visible extends HiddenBase {}");
    File jar = jar(base, versioned, true);
    String qualifier = System.getProperty("java.specification.version").startsWith("1.") ? "PublicBase" : "Visible";
    compileWithPath(classes, expected, Arrays.asList(jar), "q/Api.java",
        "package q; public class Api { public native p." + qualifier + ".Arg call(); }");
    generate(classes, Arrays.asList(classes, jar), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifierRejectsShadowedAndAmbiguousMembers() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "p/ZPublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class ZPublicBase extends HiddenBase {}",
        "p/Other.java", "package p; public interface Other { class Arg {} }", "p/AAmbiguous.java",
        "package p; public class AAmbiguous extends HiddenBase implements Other {}", "p/BShadow.java",
        "package p; public class BShadow extends HiddenBase { public static class Arg {} }");
    File jar = jar(dependency, null, false);
    compileWithPath(classes, expected, Arrays.asList(jar), "q/Api.java",
        "package q; public class Api { public native p.ZPublicBase.Arg call(); }");
    generate(classes, Arrays.asList(classes, jar), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedQualifierWithShadowedPackagePrefix() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase extends HiddenBase {}",
        "q/Api.java", "package q; import p.PublicBase; public class Api { public static class p {}"
            + " public native p local(); public native PublicBase.Arg call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedMemberInGenericConstructorBound() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public interface Arg {} public interface Other {} }"
            + " public class PublicBase extends HiddenBase { protected <T extends Arg & Runnable> PublicBase(T value) {}"
            + " protected PublicBase(Other value) {} }",
        "q/Api.java", "package q; public class Api extends p.PublicBase {"
            + " public <T extends Arg & Runnable> Api(T value) { super(value); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifierRetainsRequiredMemberOfGeneratedRoot() throws Exception {
    compile(classes, expected, "p/Owner.java",
        "package p; class HiddenBase { public static class Arg {} }"
            + " public class Owner { public native void own(); public static class PublicBase extends HiddenBase {} }",
        "q/Api.java", "package q; public class Api { public native p.Owner.PublicBase.Arg call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedParameterizedMemberInClassHeader() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase<T> { public class Arg {} } public class PublicBase<T> extends HiddenBase<T> {}",
        "q/Api.java",
        "package q; public class Api extends p.PublicBase<String> implements java.util.function.Supplier<p.PublicBase<String>.Arg> { public Arg get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPublicParameterizedMemberControl() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base<T> { public class Arg {} }",
        "p/PublicBase.java", "package p; public class PublicBase<T> extends Base<T> {}", "q/Api.java",
        "package q; public class Api extends p.PublicBase<String> implements java.util.function.Supplier<p.PublicBase<String>.Arg> { public Arg get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedMemberInSuperclassArgument() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase<T> extends HiddenBase {}",
        "q/Api.java",
        "package q; public class Api extends p.PublicBase<p.PublicBase.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAliasIntroducesMethodVariableShadow() throws Exception {
    compile(classes, expected, "p/Exposed.java",
        "package p; class Hidden { public static class Arg {} } public class Exposed extends Hidden {}",
        "_NarMethod0.java", "public class _NarMethod0 extends p.Exposed {}", "p/Left.java",
        "package p; public interface Left { default <T> Exposed.Arg call(T a) { return null; } }", "p/Right.java",
        "package p; public interface Right { default <T> Exposed.Arg call(T a) { return null; } }", "Api.java",
        "public class Api implements p.Left, p.Right { public <T> _NarMethod0.Arg call(T a) { return null; } public native void nativeCall(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedMemberConstructorArgument() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase<T> { public class Arg {} } public class PublicBase<T> extends HiddenBase<T> {}",
        "p/Parent.java", "package p; public class Parent { protected Parent(PublicBase<String>.Arg value) {} }",
        "q/Api.java",
        "package q; public class Api extends p.Parent { public Api(p.PublicBase<String>.Arg value) { super(value); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedMemberWithGenericAlias() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase<T> extends HiddenBase {}",
        "q/Api.java", "package q; public class Api { public native p.PublicBase.Arg call(p.PublicBase.Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAliasIntroducesConstructorVariableShadow() throws Exception {
    compile(classes, expected, "p/Exposed.java",
        "package p; class Hidden { public interface Arg {} } public class Exposed extends Hidden {}",
        "_NarConstructor1.java", "public class _NarConstructor1 extends p.Exposed {}", "p/Parent.java",
        "package p; public class Parent { protected <T extends Exposed.Arg & Runnable> Parent(T a) {} }", "Api.java",
        "public class Api extends p.Parent { public <T extends _NarConstructor1.Arg & Runnable> Api(T a) { super(a); } public native void nativeCall(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAliasIntroducesInterfaceVariableShadow() throws Exception {
    compile(classes, expected, "p/Exposed.java",
        "package p; class Hidden { public interface Arg {} } public class Exposed extends Hidden {}", "_NarType0.java",
        "public class _NarType0 extends p.Exposed {}", "Outer.java",
        "public interface Outer<T extends _NarType0.Arg> { class Api { public native void nativeCall(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAliasGenericMethodControl() throws Exception {
    compile(classes, expected, "p/Exposed.java",
        "package p; class Hidden { public static class Arg {} } public class Exposed extends Hidden {}", "Alias.java",
        "public class Alias extends p.Exposed {}", "p/Left.java",
        "package p; public interface Left { default <T> Exposed.Arg call(T a) { return null; } }", "p/Right.java",
        "package p; public interface Right { default <T> Exposed.Arg call(T a) { return null; } }", "Api.java",
        "public class Api implements p.Left, p.Right { public <T> Alias.Arg call(T a) { return null; } public native void nativeCall(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testQualifierDiscoverySkipsUnsupportedUnrelatedClass() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "p/PublicBase.java",
        "package p; class HiddenBase { public static class Arg {} } public class PublicBase extends HiddenBase {}",
        "Unrelated.java", "public class Unrelated {}");
    File future = new File(dependency, "Unrelated.class");
    byte[] bytes = Files.readAllBytes(future.toPath());
    bytes[6] = 0x7f;
    bytes[7] = (byte) 0xff;
    Files.write(future.toPath(), bytes);
    File jar = jar(dependency, null, false);
    compileWithPath(classes, expected, Arrays.asList(jar), "q/Api.java",
        "package q; public class Api { public native p.PublicBase.Arg call(); }");
    generate(classes, Arrays.asList(classes, jar), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardExtendsRejectsIncompatibleQualifierBound() throws Exception {
    compile(classes, expected, "p/ZBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class ZBase<T> extends Hidden<T> {}",
        "p/ABounded.java", "package p; public class ABounded<T extends Number> extends Hidden<T> {}", "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.ZBase<? extends String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardSuperRejectsIncompatibleQualifierBound() throws Exception {
    compile(classes, expected, "p/ZBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class ZBase<T> extends Hidden<T> {}",
        "p/ABounded.java", "package p; public class ABounded<T extends Number> extends Hidden<T> {}", "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.ZBase<? super String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardRejectsConflictingGenericQualifierBound() throws Exception {
    compile(classes, expected, "p/ZBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class ZBase<T> extends Hidden<T> {}",
        "p/ABounded.java", "package p; public class ABounded<T extends Comparable<Integer>> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.ZBase<? extends String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardCompatibleQualifierBoundControl() throws Exception {
    compile(classes, expected, "p/ZBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class ZBase<T> extends Hidden<T> {}",
        "p/ABounded.java", "package p; public class ABounded<T extends Number> extends Hidden<T> {}", "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.ZBase<? extends Integer>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUnboundedWildcardQualifierControl() throws Exception {
    compile(classes, expected, "p/ZBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class ZBase<T> extends Hidden<T> {}",
        "p/ABounded.java", "package p; public class ABounded<T extends Number> extends Hidden<T> {}", "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.ZBase<?>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierIsAlsoNativeTarget() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T> extends Hidden<T> { public native void base(); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testFixedQualifierIsAlsoNativeTargetControl() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase extends Hidden<String> { public native void base(); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testArrayArgumentInParameterizedQualifierControl() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T> extends Hidden<T[]> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardQualifierWithBroaderUpperBound() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T extends Number> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<? extends Object>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardQualifierWithClassInterfaceIntersection() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T extends Number> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<? extends Runnable>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardQualifierWithCompatibleLowerBound() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T extends Number> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<? super Integer>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardQualifierWithArrayUpperBound() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T extends Cloneable> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<? extends Integer[]>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardQualifierWithGenericInterfaceIntersection() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T extends Comparable<Integer>> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<? extends Number>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardQualifierRejectsNonfinalGenericConflict() throws Exception {
    compile(classes, expected, "p/ZBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class ZBase<T> extends Hidden<T> {}",
        "p/ABounded.java", "package p; public class ABounded<T extends java.util.List<Integer>> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.ZBase<? extends java.util.AbstractList<String>>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedQualifierWithGenericMember() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg<U> {} } public class PublicBase<T> extends Hidden<T> { public native void base(); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<String>.Arg<Integer>> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedQualifierReconcilesSupportingMethods() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T> extends Hidden<T> { public native void base(); }",
        "p/Left.java",
        "package p; public interface Left<T> { default java.util.List<PublicBase<T>.Arg> get() { return null; } }",
        "p/Right.java",
        "package p; public interface Right<T> { default java.util.List<PublicBase<T>.Arg> get() { return null; } }",
        "q/Api.java", "package q; public class Api implements p.Left<String>, p.Right<String> {"
            + " public java.util.List<p.PublicBase<String>.Arg> get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedQualifierReconcilesConstructorArgument() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T> extends Hidden<T> { public native void base(); }",
        "p/Parent.java", "package p; public class Parent { protected Parent(PublicBase<String>.Arg a) {} }",
        "q/Api.java",
        "package q; public class Api extends p.Parent { public Api(p.PublicBase<String>.Arg a) { super(a); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testClasspathQualifierInheritsGeneratedType() throws Exception {
    compile(classes, expected, "p/Base.java",
        "package p; class Hidden<T> { public class Arg {} } public class Base<T> extends Hidden<T> { public native void base(); }",
        "p/PublicBase.java", "package p; public class PublicBase<T> extends Base<T> {}", "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testExactQualifierAvoidsOwnerReconstruction() throws Exception {
    compile(classes, expected, "p/AGenerated.java",
        "package p; class Hidden<T> { public class Arg {} } public class AGenerated<T> extends Hidden<T> { public native void base(); }",
        "p/ZBase.java", "package p; public class ZBase<T> extends Hidden<T> {}", "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.ZBase<String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
    assertFalse(new File(work, "generated/sources/p/Hidden.java").exists());
    assertTrue(text(new File(work, "generated/sources/p/AGenerated.java")).contains("class AGenerated extends"));
  }

  public void testGeneratedNestedQualifierReconcilesOwner() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<T> { public class Arg {} } public class Outer {"
            + " public static class PublicBase<T> extends Hidden<T> { public native void base(); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer.PublicBase<String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedQualifierRetainsNativeGenericOverride() throws Exception {
    compile(classes, expected, "p/PublicBase.java", "package p; class Hidden<T> { public class Arg {} }"
        + " public class PublicBase<T> extends Hidden<T> implements java.util.function.Supplier<T> { public native T get(); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<String>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedQualifierRetainsNativeThrowableBounds() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} }"
            + " public class PublicBase<T extends Throwable> extends Hidden<T> {"
            + " public native T call(T value); public native <U extends T> U convert(U value); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<Exception>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedQualifierRetainsConstructorClassVariable() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T extends Number> { protected Hidden(T value) {} public class Arg {} }"
            + " public class PublicBase<T extends Number> extends Hidden<T> { public PublicBase(T value) { super(value); } public native void base(); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<Integer>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedQualifierRetainsEnclosingClassVariables() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A, B> { public class Arg {} } public class Outer<X> {"
            + " public native X outer(X value); public class PublicBase<Y> extends Hidden<X, Y> { public native Y member(Y value); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<String>.PublicBase<Integer>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedQualifierOmitsUnrelatedMissingMemberType() throws Exception {
    compile(classes, expected, "p/Missing.java", "package p; public class Missing {}", "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T> extends Hidden<T> {"
            + " public native void base(); public Missing unused(Missing value) { return value; } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<String>.Arg> { public native void call(); }");
    Files.delete(new File(classes, "p/Missing.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardQualifierRejectsConflictingEnclosingArguments() throws Exception {
    compile(classes, expected, "p/Outer.java", "package p; public class Outer<T> { public class Inner {} }",
        "p/ZBase.java", "package p; class Hidden<T> { public class Arg {} } public class ZBase<T> extends Hidden<T> {}",
        "p/ABounded.java", "package p; public class ABounded<T extends Outer<Integer>.Inner> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.ZBase<? extends p.Outer<String>.Inner>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardQualifierWithCompatibleEnclosingArguments() throws Exception {
    compile(classes, expected, "p/Outer.java", "package p; public class Outer<T> { public class Inner {} }",
        "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase<T extends Outer<String>.Inner> extends Hidden<T> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<? extends p.Outer<?>.Inner>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierReordersArguments() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<A, B> { public class Arg<C> {} }"
            + " public class PublicBase<X, Y> extends Hidden<Y, X> {}",
        "q/Api.java",
        "package q; public class Api implements java.util.function.Supplier<p.PublicBase<String, Integer>.Arg<Long>> {"
            + " public p.PublicBase<String, Integer>.Arg<Long> get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierMatchesNestedArguments() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} }"
            + " public class PublicBase<X> extends Hidden<java.util.List<X>> {}",
        "q/Api.java", "package q; public class Api implements java.util.function.Supplier<p.PublicBase<String>.Arg> {"
            + " public p.PublicBase<String>.Arg get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierWithFixedArgument() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class PublicBase extends Hidden<String> {}",
        "q/Api.java", "package q; public class Api implements java.util.function.Supplier<p.PublicBase.Arg> {"
            + " public p.PublicBase.Arg get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierWithUnusedParameter() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} }"
            + " public class PublicBase<X, Y extends Number> extends Hidden<X> {}",
        "q/Api.java",
        "package q; public class Api implements java.util.function.Supplier<p.PublicBase<String, Integer>.Arg> {"
            + " public p.PublicBase<String, Integer>.Arg get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierRejectsStrongerBounds() throws Exception {
    compile(classes, expected, "p/ZPublicBase.java",
        "package p; class Hidden<T> { public class Arg {} } public class ZPublicBase<X> extends Hidden<X> {}",
        "p/ABounded.java", "package p; public class ABounded<X extends Number> extends Hidden<X> {}", "q/Api.java",
        "package q; public class Api implements java.util.function.Supplier<p.ZPublicBase<String>.Arg> {"
            + " public p.ZPublicBase<String>.Arg get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierRetainsEnclosingArguments() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A, B> { public class Arg {} }"
            + " public class Outer<X> { public class PublicBase<Y> extends Hidden<X, Y> {} }",
        "q/Api.java",
        "package q; public class Api implements java.util.function.Supplier<p.Outer<String>.PublicBase<Integer>.Arg> {"
            + " public p.Outer<String>.PublicBase<Integer>.Arg get() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierUsesInterfaceBounds() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} }"
            + " public class PublicBase<X extends Number & Comparable<X>> extends Hidden<X> {}",
        "q/Outer.java", "package q; public interface Outer<T extends Number & Comparable<T>>"
            + " extends java.util.function.Supplier<p.PublicBase<T>.Arg> { class Api { public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedQualifierUsesConstructorBounds() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} }"
            + " public class PublicBase<X extends Number & Comparable<X>> extends Hidden<X> {}",
        "p/Parent.java",
        "package p; public class Parent {"
            + " protected <T extends Number & Comparable<T>> Parent(PublicBase<T>.Arg a, T b) {}"
            + " protected Parent(String a, Runnable b, int c) {} }",
        "q/Api.java", "package q; public class Api extends p.Parent {"
            + " public Api(p.PublicBase<Integer>.Arg a) { super(a, (Integer) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
    assertTrue(text(new File(work, "generated/sources/q/Api.java")).contains(".Arg) null"));
  }

  public void testParameterizedQualifierPreservesMethodVariable() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<T> { public class Arg {} }"
            + " public class PublicBase<X extends Number & Comparable<X>> extends Hidden<X> {}",
        "_NarMethod0.java", "public class _NarMethod0<X extends Number & Comparable<X>> extends p.PublicBase<X> {}",
        "p/Left.java",
        "package p; public interface Left { default <T extends Number & Comparable<T>> PublicBase<T>.Arg call(T value) { return null; } }",
        "p/Right.java",
        "package p; public interface Right { default <T extends Number & Comparable<T>> PublicBase<T>.Arg call(T value) { return null; } }",
        "Api.java",
        "public class Api implements p.Left, p.Right { public <T extends Number & Comparable<T>> _NarMethod0<T>.Arg call(T value) { return null; }"
            + " public native void nativeCall(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testClassVariableReservesGenericBodyQualifier() throws Exception {
    compile(classes, expected, "p/Exposed.java",
        "package p; class Hidden { public static class Arg {} } public class Exposed extends Hidden {}",
        "_NarType0.java", "public class _NarType0 extends p.Exposed {}", "p/Left.java",
        "package p; public interface Left { default <T> Exposed.Arg call(T value) { return null; } }", "p/Right.java",
        "package p; public interface Right { default <T> Exposed.Arg call(T value) { return null; } }", "Outer.java",
        "public interface Outer<X> extends p.Left, p.Right { <T> _NarType0.Arg call(T value);"
            + " class Api { public native void nativeCall(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testActiveClassVariableDoesNotCaptureLaterQualifier() throws Exception {
    compile(classes, expected, "p/Exposed.java",
        "package p; class Hidden { public static class Arg {} } public class Exposed extends Hidden {}",
        "_NarType0.java", "public class _NarType0 extends p.Exposed {}", "p/Left.java",
        "package p; public interface Left { default Exposed.Arg call() { return null; } }", "p/Right.java",
        "package p; public interface Right { default Exposed.Arg call() { return null; } }", "Outer.java",
        "public interface Outer<X> extends p.Left, p.Right { _NarType0.Arg call();"
            + " class Api { public native void nativeCall(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testDiscoveredQualifierPackageDoesNotCaptureMethodVariable() throws Exception {
    compile(classes, expected, "p/Exposed.java",
        "package p; class Hidden { public static class Arg {} } public class Exposed extends Hidden {}",
        "_NarMethod0/Alias.java", "package _NarMethod0; public class Alias extends p.Exposed {}", "p/Left.java",
        "package p; public interface Left { default <T> Exposed.Arg call(T value) { return null; } }", "p/Right.java",
        "package p; public interface Right { default <T> Exposed.Arg call(T value) { return null; } }", "Api.java",
        "public class Api implements p.Left, p.Right { public <T> _NarMethod0.Alias.Arg call(T value) { return null; }"
            + " public native void nativeCall(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testNativeBindingMismatchIsRejectedBeforeCompilation() throws Exception {
    compile(classes, expected, "Shadow.java", "public class Shadow {}", "Api.java",
        "public class Api { public native Shadow call();"
            + " public static class Shadow { public native void nested(); } }");
    // Model bytecode whose native descriptor cannot use the apparent source
    // spelling: Api.Shadow would capture the unnamed-package Shadow reference.
    File original = new File(classes, "Api.class");
    org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(Files.readAllBytes(original.toPath()));
    final ClassWriter writer = new ClassWriter(0);
    reader.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
          String[] exceptions) {
        return super.visitMethod(access, name, name.equals("call") ? "()LShadow;" : descriptor, signature, exceptions);
      }
    }, 0);
    Files.write(original.toPath(), writer.toByteArray());
    Files.createDirectories(actual.toPath());
    for (String name : expected.list()) {
      Files.copy(new File(expected, name).toPath(), new File(actual, name).toPath());
    }
    failure("lexical name Shadow binds to [Api$Shadow]", Collections.<String> emptySet());
    assertFalse("Reject the incorrect binding before javac", new File(work, "generated/classes/Api.class").isFile());
    equalHeaders(); // Both previously published headers must remain intact.
  }

  public void testNativeAbiGuardRejectsDescriptorChange() throws Exception {
    compile(classes, null, "Api.java", "public class Api { public native Object call(); }");
    JniClass original = new JniClass();
    new org.objectweb.asm.ClassReader(Files.readAllBytes(new File(classes, "Api.class").toPath())).accept(original, 0);
    File generated = directory("changed");
    compile(generated, null, "Api.java", "public class Api { public native String call(); }");
    try {
      JavacHeaders.verifyNativeMethods(original, new File(generated, "Api.class"));
      fail("A changed native descriptor must not be published");
    } catch (IOException ex) {
      assertTrue(ex.getMessage(), ex.getMessage().contains("Native method ABI changed"));
    }
  }

  public void testNativeAbiGuardRejectsStaticChange() throws Exception {
    compile(classes, null, "Api.java", "public class Api { public native void call(); }");
    JniClass original = new JniClass();
    new org.objectweb.asm.ClassReader(Files.readAllBytes(new File(classes, "Api.class").toPath())).accept(original, 0);
    File generated = directory("changed");
    compile(generated, null, "Api.java", "public class Api { public static native void call(); }");
    try {
      JavacHeaders.verifyNativeMethods(original, new File(generated, "Api.class"));
      fail("A changed jobject/jclass receiver must not be published");
    } catch (IOException ex) {
      assertTrue(ex.getMessage(), ex.getMessage().contains("Native method ABI changed"));
    }
  }

  public void testUnrelatedUnnamedTypeDoesNotBlockImport() throws Exception {
    compile(classes, expected, "Throwable.java", "public class Throwable {}", "Base.java",
        "public class Base { protected Base() throws Exception {} }", "java.java",
        "import java.lang.Throwable; public class java extends Base {"
            + " public java() throws Throwable {} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testThrowingConstructorThroughGeneratedSuperclass() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { protected Base() throws Exception {} }", "Middle.java",
        "public class Middle extends Base { public Middle() throws Exception {} public native void middle(); }",
        "Api.java", "public class Api extends Middle { public Api() throws Exception {} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPackageShadowByClass() throws Exception {
    compile(classes, expected, "p/java.java", "package p; public class java { public native String call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPackageShadowBySupportMethodFormal() throws Exception {
    compile(classes, expected, "p/Left.java",
        "package p; public interface Left { default <java extends CharSequence> java value(){ return null; } }",
        "p/Right.java", "package p; public interface Right { <java extends CharSequence> java value(); }", "p/Api.java",
        "package p; public class Api implements Left, Right { public <java extends CharSequence> java value(){return null;} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPackageShadowByInterfaceFormal() throws Exception {
    compile(classes, expected, "p/Container.java",
        "package p; public interface Container<java extends CharSequence> { class Api { public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSuperclassCovariantInterfaceOverride() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { public Object value(){return null;} }",
        "p/Contract.java", "package p; public interface Contract { String value(); }", "p/Api.java",
        "package p; public class Api extends Base implements Contract { public String value(){return null;} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSuperclassCovariantDefaultOverride() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { public Object value(){return null;} }",
        "p/Contract.java", "package p; public interface Contract { default String value(){return null;} }",
        "p/Api.java",
        "package p; public class Api extends Base implements Contract { public String value(){return null;} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSuperclassAccessInterfaceOverride() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { protected String value(){return null;} }",
        "p/Contract.java", "package p; public interface Contract { String value(); }", "p/Api.java",
        "package p; public class Api extends Base implements Contract { public String value(){return null;} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSuperclassThrowsInterfaceOverride() throws Exception {
    compile(classes, expected, "p/Base.java",
        "package p; public class Base { public String value() throws Exception {return null;} }", "p/Contract.java",
        "package p; public interface Contract { String value(); }", "p/Api.java",
        "package p; public class Api extends Base implements Contract { public String value(){return null;} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPackageShadowInConstructorTypes() throws Exception {
    compile(classes, expected, "p/Base.java",
        "package p; import java.util.List; public class Base { protected Base(List<String> value) {} }", "p/java.java",
        "package p; import java.util.List; public class java extends Base {"
            + " public java() { super(null); } public native String call(List<String> value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPackageShadowInConstantOnlyInterface() throws Exception {
    compile(classes, expected, "p/java.java",
        "package p; import java.lang.annotation.Native; public interface java { @Native int VALUE = 4; }");
    generate(classes, Arrays.asList(classes), set("p.java"), Collections.<String> emptySet());
    equalHeaders();
    assertTrue(text(new File(actual, "p_java.h")).contains("p_java_VALUE 4L"));
    assertFalse(text(new File(actual, "p_java.h")).contains("__nar_header"));
  }

  public void testPackageShadowWithUnrelatedNestedSimpleName() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; public class Outer {" + " public static class java { public native String call(); }"
            + " public static class Other { public static class String { public native void call(); } } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPackageShadowByInheritedMember() throws Exception {
    compile(classes, expected, "p/Base.java", "package p; public class Base { public static class java {} }",
        "p/Api.java", "package p; public class Api extends Base { public native String call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCompatibleSuperclassDoesNotRetainUnneededOverride() throws Exception {
    compile(classes, expected, "p/Missing.java", "package p; public class Missing {}", "p/Base.java",
        "package p; public class Base { public Object value() { return null; } }", "p/Contract.java",
        "package p; public interface Contract { Object value(); }", "p/Api.java",
        "package p; public class Api extends Base implements Contract {"
            + " public Missing value() { return null; } public native void call(); }");
    assertTrue(new File(classes, "p/Missing.class").delete());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardOwnerOverloadAmbiguity() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { public static class Owner<T> { public class Inner {} }"
        + " protected Base(Owner<?>.Inner a, CharSequence b) {} protected <T> Base(Owner<T>.Inner a, Object b) {} }",
        "Api.java",
        "public class Api extends Base { public Api() { super((Base.Owner<String>.Inner) null, (Object) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericOwnerOnlyCandidate() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { public static class Owner<T> { public class Inner {} } protected <T> Base(Owner<T>.Inner a) {} }",
        "Api.java",
        "public class Api extends Base { public Api() { super((Base.Owner<?>.Inner) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardOwnerAndMemberOverloadAmbiguity() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { public static class Owner<T extends Number> { public class Inner<U extends CharSequence> {} }"
            + " protected Base(Owner<?>.Inner<?> a, CharSequence b) {}"
            + " protected <T extends Number, U extends CharSequence> Base(Owner<T>.Inner<U> a, Object b) {} }",
        "Api.java", "public class Api extends Base { public Api() {"
            + " super((Base.Owner<Integer>.Inner<String>) null, (Object) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorWildcardArrayBound() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { protected <T> Base(java.util.List<? extends T[]> values) {} }", "Api.java",
        "public class Api extends Base { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMixedNullConstructorAlternative() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class A {} private static class B {}"
            + " protected Base(A value) {} protected Base(B value) {} protected Base(A value, int other) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null, 0); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMixedNullConstructorAlternativeNonGeneric() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { private static class A {} private static class B {}"
            + " protected Base(A value) {} protected Base(B value) {} protected Base(A value, int other) {} }",
        "Api.java", "public class Api extends Base { public Api() { super(null, 0); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMixedNullConstructorOnlyCandidate() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class A {} private static class B {}"
            + " protected Base(A value, int other) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null, 0); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCastConstructorHigherArityAlternative() throws Exception {
    compile(classes, expected, "Base.java", "public class Base<T> { private static class A {} private static class B {}"
        + " protected Base(A value) {} protected Base(B value) {} protected Base(java.util.List<A> values, int other) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((java.util.List) null, 0); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorSelectionWithSameFileNames() throws Exception {
    compile(classes, expected, "dep/Base.java",
        "package dep; public class Base<T> { private static class A {} private static class B {}"
            + " protected Base(A value) {} protected Base(B value) {} protected Base(A value, int other) {} }",
        "dep/Only.java", "package dep; public class Only { private static class A {} protected Only(A value) {} }",
        "left/Api.java",
        "package left; public class Api extends dep.Base<String> { public Api() { super(null, 0); } public native void call(); }",
        "right/Api.java",
        "package right; public class Api extends dep.Only { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorSelectionForNestedTargets() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class A {} private static class B {}"
            + " protected Base(A value) {} protected Base(B value) {} protected Base(A value, int other) {} }",
        "Outer.java",
        "public class Outer extends Base<String> { public Outer() { super(null, 0); } public native void outer();"
            + " public static class Inner extends Base<Integer> { public Inner() { super(null, 0); } public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorSelectionAddsSupportingMember() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class A {} private static class B {}"
            + " protected Base(A value) {} protected Base(B value) {} protected Base(A value, Api.Marker marker) {} }",
        "Api.java", "public class Api extends Base<String> { public interface Marker {}"
            + " public Api() { super(null, null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAmbiguousTypedConstructorAlternative() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <U extends Runnable & java.io.Serializable> Base(U value) {}"
            + " protected <U extends java.io.Serializable & Runnable, V> Base(U value) {} protected Base(int a, int b) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(0, 0); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAmbiguousConstructorsFailBeforeCompilationAndPreserveHeaders() throws Exception {
    compile(classes, expected, "Api.java", "public class Api { public native int call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    byte[] original = Files.readAllBytes(new File(actual, "Api.h").toPath());
    compile(classes, null, "Base.java",
        "public class Base<T> { private static class A {} private static class B {}"
            + " protected Base(A value) {} protected Base(B value) {} public static A value() { return null; } }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(Base.value()); } public native void call(); }");
    FileUtils.deleteDirectory(new File(work, "generated"));
    failure("ambiguous", Collections.<String> emptySet());
    assertTrue(Arrays.equals(original, Files.readAllBytes(new File(actual, "Api.h").toPath())));
    assertFalse("Reject ambiguous constructors before starting compilation",
        new File(work, "generated/javac.args").exists());
  }

  public void testPrivateConstructorNullOverloadAlternative() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class Hidden {}"
            + " protected Base(Hidden value) {} protected Base(java.util.List<Hidden> values) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((java.util.List) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorNullOverloadAlternativeNonGeneric() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { private static class Hidden {}"
            + " protected Base(Hidden value) {} protected Base(java.util.List<Hidden> values) {} }",
        "Api.java",
        "public class Api extends Base { public Api() { super((java.util.List) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testDirectPrivateConstructorParameter() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class Hidden {} protected Base(Hidden value) {} }", "Api.java",
        "public class Api extends Base<String> { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorFreshNamesSimultaneousRename() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <_NarConstructor1 extends Number & Comparable<_NarConstructor1>, _NarConstructor0 extends _NarConstructor1> Base(_NarConstructor0 value) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((Integer) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorFallbackRetainsVisibleBound() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private interface Hidden {} protected <U extends Number & Hidden, V extends CharSequence & Comparable<V>> Base(U value, V other) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null, (String) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorWildcardArgument() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private interface Hidden {} protected Base(java.util.List<? extends Hidden> values) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorNullOverloadReversedDeclarations() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class Hidden {}"
            + " protected Base(java.util.List<Hidden> values) {} protected Base(Hidden value) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((java.util.List) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorNullOverloadInnerSuperclass() throws Exception {
    compile(classes, expected, "Owner.java",
        "public class Owner<T> { private static class Hidden {} public class Base<V> {"
            + " protected Base(Hidden value) {} protected Base(java.util.List<Hidden> values) {} } }",
        "Api.java", "public class Api extends Owner<String>.Base<Integer> {"
            + " public Api(Owner<String> owner) { owner.super((java.util.List) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorBoundOverloadAlternative() throws Exception {
    compile(classes, expected, "Base.java", "public class Base<T> { private interface Hidden {}"
        + " protected <U extends Number & Hidden> Base(U value) {} protected Base(java.util.List<Hidden> values) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((java.util.List) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorNullOverloadMostSpecific() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class Hidden {} private static class Specific extends Hidden {}"
            + " protected Base(Hidden value) {} protected Base(Specific value) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorPrivateTypeArgument() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class Hidden {} protected Base(java.util.List<Hidden> values) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorPrivateTypeArgumentNonGenericBase() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { private static class Hidden {} protected Base(java.util.List<Hidden> values) {} }",
        "Api.java", "public class Api extends Base { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorPrivateTypeBound() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private interface Hidden {} protected <U extends Number & Hidden> Base(U value) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorUnusedPrivateTypeBound() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private interface Hidden {} protected <U extends Hidden> Base() {} }", "Api.java",
        "public class Api extends Base<String> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorRecursiveComparableWildcard() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <U extends Number & Comparable<? super U>> Base(U value) {} }", "Api.java",
        "public class Api extends Base<String> { public Api() { super((Integer) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testWildcardRecursiveComparableProjection() throws Exception {
    compile(classes, expected, "Values.java",
        "public class Values<T extends CharSequence & Comparable<? super T>> extends java.util.ArrayList<T> {}",
        "Left.java", "public interface Left { java.util.Collection<? extends Comparable<?>> value(); }", "Right.java",
        "public interface Right { Values<?> value(); }", "Api.java",
        "public enum Api implements Left, Right { VALUE { public Values<?> value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorFormalCapturesSpecializedClass() throws Exception {
    compile(classes, expected, "U.java", "public class U {}", "Base.java",
        "public class Base<T> { protected <U extends T> Base(U value) {} }", "Api.java",
        "public class Api extends Base<U> { public Api() { super((U) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorFormalCapturesPackage() throws Exception {
    compile(classes, expected, "Base.java", "public class Base<T> { protected <java extends T> Base(java value) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((String) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorArgumentsWithOverloads() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class Hidden {}"
            + " protected Base(java.util.List<Hidden> values) {} protected Base(java.util.Set<Hidden> values) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((java.util.List) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorArgumentAndIntersection() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class Hidden {}"
            + " protected <U extends Number & Comparable<U>> Base(java.util.List<Hidden> values, U count) {}"
            + " protected Base(java.util.List<Hidden> values, String text) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null, (Integer) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorDependentBounds() throws Exception {
    compile(classes, expected, "Base.java", "public class Base<T> { private interface Hidden {}"
        + " protected <U extends Number & Hidden, V extends U> Base(V[] values, java.util.List<? super U> more) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null, null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorInnerSuperclass() throws Exception {
    compile(classes, expected, "Owner.java",
        "public class Owner<T> { private static class Hidden {} public class Base<V> {"
            + " protected Base(java.util.List<Hidden> values, T text, V number) {} } }",
        "Api.java", "public class Api extends Owner<String>.Base<Integer> {"
            + " public Api(Owner<String> owner) { owner.super(null, null, null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorFreshNamesAvoidClassAndPackage() throws Exception {
    compile(classes, expected, "_NarConstructor0.java", "public class _NarConstructor0 {}",
        "_NarConstructor1/Value.java", "package _NarConstructor1; public class Value {}", "Base.java",
        "public class Base<T> { protected <java extends T, U extends java>"
            + " Base(java value, U[] more, _NarConstructor1.Value marker) {} }",
        "Api.java", "public class Api extends Base<_NarConstructor0> { public Api() {"
            + " super((_NarConstructor0) null, (_NarConstructor0[]) null, null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorFreshRecursiveNames() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <java extends Number & Comparable<? super java>, U extends java>"
            + " Base(java value, U[] more) {} }",
        "Api.java", "public class Api extends Base<String> { public Api() {"
            + " super((Integer) null, (Integer[]) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorFreshNameAvoidsTargetClass() throws Exception {
    compile(classes, expected, "Base.java", "public class Base<T> { protected <U extends T> Base(U value) {} }",
        "_NarConstructor0.java", "public class _NarConstructor0 extends Base<String> {"
            + " public _NarConstructor0() { super((String) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPrivateConstructorArgumentArraysAndPrimitives() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private static class Hidden {}"
            + " protected Base(java.util.List<Hidden>[] values, boolean flag, int[] data, long count) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super(null, false, null, 0); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedConstructorIntersection() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <U extends Number & Comparable<U>> Base(U value) {} }", "Api.java",
        "public class Api extends Base<String> { public Api() { super((Integer) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedConstructorRecursiveBound() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <U extends Comparable<U>> Base(U value) {} }", "Api.java",
        "public class Api extends Base<String> { public Api() { super((String) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedConstructorDependentArguments() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <U> Base(java.util.List<U> value, U other) {} }", "Api.java",
        "public class Api extends Base<String> { public Api() { super(null, null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testNativeGenericSuperclassOverride() throws Exception {
    compile(classes, expected, "Base.java", "public class Base<T> { public T value() { return null; } }", "Api.java",
        "public class Api extends Base<java.util.List<String>> { public native java.util.List<String> value(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedGenericConstructorBoundOnClass() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T extends Number & Runnable> { protected Base(T value) {} }", "Value.java",
        "public abstract class Value extends Number implements Runnable {}", "Api.java",
        "public class Api extends Base<Value> { public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCovariantBoundedWildcardReturn() throws Exception {
    compile(classes, expected, "TextList.java",
        "public class TextList<T extends CharSequence> extends java.util.ArrayList<T> {}", "Left.java",
        "public interface Left { java.util.Collection<? extends CharSequence> value(); }", "Right.java",
        "public interface Right { TextList<?> value(); }", "Api.java",
        "public enum Api implements Left, Right { VALUE { public TextList<?> value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCovariantBoundedWildcardReturnReversed() throws Exception {
    compile(classes, expected, "TextList.java",
        "public class TextList<T extends CharSequence> extends java.util.ArrayList<T> {}", "Left.java",
        "public interface Left { java.util.Collection<? extends CharSequence> value(); }", "Right.java",
        "public interface Right { TextList<?> value(); }", "Api.java",
        "public enum Api implements Right, Left { VALUE { public TextList<?> value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericConstructorIntersectionOverloads() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <U extends Number & Comparable<U>> Base(U value) {}"
            + " protected Base(String value) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((Integer) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericInnerConstructorIntersection() throws Exception {
    compile(classes, expected, "Owner.java",
        "public class Owner<T> { public class Base<V> {"
            + " protected <U extends Number & Comparable<U>> Base(U value, T other, V[] array) {} } }",
        "Api.java", "public class Api extends Owner<String>.Base<Integer> {"
            + " public Api(Owner<String> owner) { owner.super((Integer) null, null, null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testChainedGenericConstructorBounds() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <U extends Number & Comparable<U>, V extends U>"
            + " Base(java.util.List<? super U> values, V other, U[] array) {} }",
        "Api.java", "public class Api extends Base<String> { public Api() {"
            + " super((java.util.List<Number>) null, (Integer) null, (Integer[]) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUnusedConstructorFormalNeedsNoDeclaration() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private interface Hidden {} protected <U extends Hidden> Base() {} }", "Api.java",
        "public class Api extends Base<String> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInaccessibleConstructorBoundUsesAlternative() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { private interface Hidden {}"
            + " protected <U extends Number & Hidden> Base(U value) {} protected Base(String value) {} }",
        "Api.java",
        "public class Api extends Base<String> { public Api() { super((String) null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorBoundNeedsSupportingMember() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> { protected <U extends Number & Outer.Marker> Base(U value) {} }", "Value.java",
        "public abstract class Value extends Number implements Outer.Marker {}", "Outer.java",
        "public class Outer { public interface Marker {} public static class Api extends Base<String> {"
            + " public Api() { super((Value) null); } public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCovariantDeclaredLowerWildcardBound() throws Exception {
    boundedWildcardReturns("T extends CharSequence", "? super String", "CharSequence");
  }

  public void testCovariantExplicitWildcardIntersection() throws Exception {
    boundedWildcardReturns("T extends CharSequence", "? extends java.io.Serializable", "CharSequence");
  }

  public void testCovariantDeclaredWildcardIntersection() throws Exception {
    boundedWildcardReturns("T extends CharSequence & java.io.Serializable", "?", "java.io.Serializable");
  }

  public void testCovariantDependentWildcardBounds() throws Exception {
    boundedWildcardReturns("A extends CharSequence, B extends A, T extends B", "?, ?, ?", "CharSequence");
  }

  public void testCovariantRecursiveWildcardBounds() throws Exception {
    boundedWildcardReturns("T extends CharSequence & Comparable<T>", "?", "Comparable<?>");
  }

  public void testCovariantWildcardBoundFromOwner() throws Exception {
    compile(classes, expected, "Owner.java",
        "public class Owner<A extends CharSequence> { public class Values<T extends A> extends java.util.ArrayList<T> {} }",
        "Left.java", "public interface Left { java.util.Collection<? extends CharSequence> value(); }", "Right.java",
        "public interface Right { Owner<String>.Values<?> value(); }", "Api.java",
        "public enum Api implements Left, Right { VALUE { public Owner<String>.Values<?> value() { return null; } }; public native void call(); }",
        "Reverse.java",
        "public enum Reverse implements Right, Left { VALUE { public Owner<String>.Values<?> value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  private void boundedWildcardReturns(String formals, String arguments, String upper) throws Exception {
    String result = "Bounded<" + arguments + ">";
    compile(classes, expected, "Bounded.java",
        "public class Bounded<" + formals + "> extends java.util.ArrayList<T> {}", "Left.java",
        "public interface Left { java.util.Collection<? extends " + upper + "> value(); }", "Right.java",
        "public interface Right { " + result + " value(); }", "Api.java",
        "public enum Api implements Left, Right { VALUE { public " + result
            + " value() { return null; } }; public native void call(); }",
        "Reverse.java", "public enum Reverse implements Right, Left { VALUE { public " + result
            + " value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRawParentParameterizedDirectInterface() throws Exception {
    compile(classes, expected, "Base.java",
        "public abstract class Base<T> implements java.util.function.Supplier<T> {}", "Text.java",
        "public interface Text<T> extends java.util.function.Supplier<T> {}", "Api.java",
        "public abstract class Api extends Base<String> implements Text<String> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedCovariantEnumMethod() throws Exception {
    compile(classes, expected, "Left.java", "public interface Left { java.util.List<? extends CharSequence> value(); }",
        "Right.java", "public interface Right { java.util.List<String> value(); }", "Api.java",
        "public enum Api implements Left, Right { VALUE { public java.util.List<String> value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedCovariantEnumMethodReversed() throws Exception {
    compile(classes, expected, "Left.java", "public interface Left { java.util.List<? extends CharSequence> value(); }",
        "Right.java", "public interface Right { java.util.List<String> value(); }", "Api.java",
        "public enum Api implements Right, Left { VALUE { public java.util.List<String> value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedSuperclassConstructor() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> implements java.util.function.Supplier<T> {"
            + " protected Base(T value) {} public final T get() { return null; } }",
        "Text.java", "public interface Text<T> extends java.util.function.Supplier<T> {}", "Api.java",
        "public class Api extends Base<String> implements Text<String> {"
            + " public Api() { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testParameterizedInnerSuperclassConstructor() throws Exception {
    compile(classes, expected, "Owner.java",
        "public class Owner<T> { public abstract class Base<U> implements java.util.function.Supplier<T> {"
            + " protected Base(T value, U other) {} } }",
        "Text.java", "public interface Text<T> extends java.util.function.Supplier<T> {}", "Api.java",
        "public abstract class Api extends Owner<String>.Base<Integer> implements Text<String> {"
            + " public Api(Owner<String> owner) { owner.super(null, null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedGenericSuperclassCompatibility() throws Exception {
    compile(classes, expected, "Base.java",
        "public abstract class Base<T> implements java.util.function.Supplier<T> { public native void base(); }",
        "Text.java", "public interface Text<T> extends java.util.function.Supplier<T> {}", "Api.java",
        "public abstract class Api extends Base<String> implements Text<String> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCovariantLowerWildcardReturns() throws Exception {
    covariantReturns("java.util.List<? super String> value()", "java.util.List<? super CharSequence> value()",
        "java.util.List<Object> value()");
  }

  public void testCovariantNestedWildcardReturns() throws Exception {
    covariantReturns("java.util.List<? extends java.util.List<? extends CharSequence>> value()",
        "java.util.List<java.util.List<String>> value()", "java.util.List<java.util.List<String>> value()");
  }

  public void testCovariantGenericSubtypeReturns() throws Exception {
    covariantReturns("java.util.Collection<? extends CharSequence> value()", "java.util.ArrayList<String> value()",
        "java.util.ArrayList<String> value()");
  }

  public void testCovariantProjectedWildcardReturns() throws Exception {
    covariantReturns("java.util.Collection<? super String> value()", "java.util.List<? super CharSequence> value()",
        "java.util.List<Object> value()");
  }

  public void testCovariantParameterizedArrayReturns() throws Exception {
    covariantReturns("java.util.List<? extends CharSequence>[] value()", "java.util.List<String>[] value()",
        "java.util.List<String>[] value()");
  }

  public void testCovariantGenericMethodReturns() throws Exception {
    covariantReturns("<T extends CharSequence> java.util.List<? extends T> value()",
        "<U extends CharSequence> java.util.List<U> value()", "<V extends CharSequence> java.util.List<V> value()");
  }

  public void testCovariantMethodBoundReturns() throws Exception {
    covariantReturns("<T extends CharSequence> java.util.List<? extends CharSequence> value()",
        "<U extends CharSequence> java.util.List<U> value()", "<V extends CharSequence> java.util.List<V> value()");
  }

  public void testCovariantSupportingInterfaceBoundReturns() throws Exception {
    compile(classes, expected, "Left.java",
        "public interface Left<T extends Number & CharSequence> { java.util.List<? extends CharSequence> value(); }",
        "Right.java", "public interface Right<T extends Number & CharSequence> { java.util.List<T> value(); }",
        "Outer.java",
        "public class Outer { public abstract static class Digits extends Number implements CharSequence {} public interface Contract<T extends Number & CharSequence> extends Left<T>, Right<T> {}"
            + " public enum Api implements Contract<Digits> { VALUE { public java.util.List<Digits> value() { return null; } };"
            + " public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  private void covariantReturns(String left, String right, String implementation) throws Exception {
    compile(classes, expected, "Left.java", "public interface Left { " + left + "; }", "Right.java",
        "public interface Right { " + right + "; }", "Api.java",
        "public enum Api implements Left, Right { VALUE { public " + implementation
            + " { return null; } }; public native void call(); }",
        "Reverse.java", "public enum Reverse implements Right, Left { VALUE { public " + implementation
            + " { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testEnumArgumentBoundOnGeneratedClass() throws Exception {
    compile(classes, expected, "Bound.java", "public interface Bound<T extends Comparable<T>> {}", "Payload.java",
        "public class Payload implements Comparable<Payload> { public int compareTo(Payload p) { return 0; } public native void call(); }",
        "Api.java", "public enum Api implements Bound<Payload> { VALUE; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSupportingInterfaceGenericMethodConflict() throws Exception {
    compile(classes, expected, "Left.java",
        "public interface Left<T> { default <U extends T> U value(U input) { return null; } }", "Right.java",
        "public interface Right<T> { <U extends T> U value(U input); }", "Outer.java",
        "public class Outer { public interface Contract<T extends CharSequence> extends Left<T>, Right<T> { <U extends T> U value(U input); }"
            + " public enum Api implements Contract<String> { VALUE { public <U extends String> U value(U input) { return null; } }; public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSupportingInterfaceSelfBound() throws Exception {
    compile(classes, expected, "Outer.java",
        "public class Outer { public interface Ordered<T extends Ordered<T>> extends Comparable<T> {}"
            + " public enum Api implements Ordered<Api> { VALUE; public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testEnumNestedWildcardArguments() throws Exception {
    compile(classes, expected, "Sink.java", "public interface Sink<T> { void accept(T input); }", "Api.java",
        "public enum Api implements Sink<java.util.List<? super String>> { VALUE { public void accept(java.util.List<? super String> input) {} }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSupportingGenericMethodChainedBounds() throws Exception {
    compile(classes, expected, "Left.java",
        "public interface Left<T> { default <U extends T, V extends U> V value(V input) { return null; } }",
        "Right.java", "public interface Right<T> { <U extends T, V extends U> V value(V input); }", "Outer.java",
        "public class Outer { public interface Contract<T extends CharSequence> extends Left<T>, Right<T> {"
            + " <U extends T, V extends U> V value(V input); }"
            + " public enum Api implements Contract<String> { VALUE {"
            + " public <U extends String, V extends U> V value(V input) { return null; } }; public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSupportingGenericMethodIntersectionAndShadowing() throws Exception {
    String method = "<T extends Number & Outer.Marker & Comparable<T>> T[] value(java.util.List<? super T> input, T[] array)";
    compile(classes, expected, "Missing.java", "public class Missing {}", "Left.java",
        "public interface Left<T> { default " + method + " { return null; }"
            + " default <U extends Missing> U unrelated() { return null; } }",
        "Right.java", "public interface Right<T> { " + method + "; }", "Outer.java",
        "public class Outer { public interface Marker {}" + " public interface Contract<T> extends Left<T>, Right<T> { "
            + method + "; }" + " public enum Api implements Contract<String> { VALUE { public " + method
            + " { return null; } };" + " public native void call(); } }");
    Files.delete(new File(classes, "Missing.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInterfaceTypeVariableAvoidsMemberNameInBody() throws Exception {
    compile(classes, expected, "Left.java", "public interface Left<T> { default T get() { return null; } }",
        "Right.java", "public interface Right<T> { T get(); }", "Container.java",
        "public interface Container<T> extends Left<T>, Right<T> { T get();"
            + " class _NarType0 {} class Api implements Container<String> { public String get() { return null; }"
            + " public native void call(_NarType0 value); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSupportingGenericMethodParameterizedOwner() throws Exception {
    String method = "<U extends T> Box<T>.Value<U> value(Box<T>.Value<? extends U> input)";
    compile(classes, expected, "Box.java", "public class Box<T> { public class Value<U> {} }", "Left.java",
        "public interface Left<T> { default " + method + " { return null; } }", "Right.java",
        "public interface Right<T> { " + method + "; }", "Outer.java",
        "public class Outer { public interface Contract<T extends CharSequence> extends Left<T>, Right<T> { " + method
            + "; } public enum Api implements Contract<String> { VALUE {"
            + " public <U extends String> Box<String>.Value<U> value(Box<String>.Value<? extends U> input) { return null; } };"
            + " public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericEnumInterface() throws Exception {
    compile(classes, expected, "Ordered.java", "public interface Ordered<T> extends Comparable<T> {}", "Api.java",
        "public enum Api implements Ordered<Api> { VALUE; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedOuterTypeVariable() throws Exception {
    compile(classes, expected, "Outer.java",
        "public class Outer<T> { public class Base { public final void accept(T value) {} } }", "Strings.java",
        "public class Strings extends Outer<String>.Base { public Strings(Outer<String> outer) { outer.super(); } }",
        "Text.java", "public interface Text extends java.util.function.Consumer<String> {}", "Wide.java",
        "public interface Wide { default void accept(String value) {} }", "Api.java",
        "public class Api extends Strings implements Text, Wide { public Api() { super(new Outer<String>()); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMultipleEnclosingGenericScopes() throws Exception {
    compile(classes, expected, "Outer.java",
        "public class Outer<A> { public class Middle<B> { public class Base<C> { public final void accept(A a, B b, C c) {} } } }",
        "Strings.java",
        "public class Strings extends Outer<String>.Middle<Integer>.Base<Long> { public Strings(Outer<String>.Middle<Integer> owner) { owner.super(); } }",
        "Operation.java", "public interface Operation<A, B, C> { void accept(A a, B b, C c); }", "Text.java",
        "public interface Text extends Operation<String, Integer, Long> {}", "Wide.java",
        "public interface Wide { default void accept(String a, Integer b, Long c) {} }", "Api.java",
        "public class Api extends Strings implements Text, Wide { public Api() { super(new Outer<String>().new Middle<Integer>()); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMemberTypeParameterShadowsOwnerParameter() throws Exception {
    compile(classes, expected, "Outer.java",
        "public class Outer<T> { public class Base<T> { public final void accept(T value) {} } }", "Strings.java",
        "public class Strings extends Outer<Integer>.Base<String> { public Strings(Outer<Integer> owner) { owner.super(); } }",
        "Text.java", "public interface Text extends java.util.function.Consumer<String> {}", "Wide.java",
        "public interface Wide { default void accept(String value) {} }", "Api.java",
        "public class Api extends Strings implements Text, Wide { public Api() { super(new Outer<Integer>()); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRawEnclosingGenericScope() throws Exception {
    compile(classes, expected, "Outer.java",
        "public class Outer<T extends Number> { public class Base { public final void accept(T value) {} } }",
        "Numbers.java", "public class Numbers extends Outer.Base { public Numbers(Outer owner) { owner.super(); } }",
        "Numeric.java", "public interface Numeric extends java.util.function.Consumer<Number> {}", "Wide.java",
        "public interface Wide { default void accept(Number value) {} }", "Api.java",
        "public class Api extends Numbers implements Numeric, Wide { public Api() { super(new Outer()); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testStaticMemberStopsEnclosingGenericScope() throws Exception {
    compile(classes, expected, "Outer.java",
        "public class Outer<T> { public static class Middle<U> { public class Base<V> { public final void accept(U a, V b) {} } } }",
        "Strings.java",
        "public class Strings extends Outer.Middle<String>.Base<Integer> { public Strings(Outer.Middle<String> owner) { owner.super(); } }",
        "Text.java", "public interface Text extends java.util.function.BiConsumer<String, Integer> {}", "Wide.java",
        "public interface Wide { default void accept(String a, Integer b) {} }", "Api.java",
        "public class Api extends Strings implements Text, Wide { public Api() { super(new Outer.Middle<String>()); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericMemberEnumInterfaceAndCovariance() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { public Outer.Ordered<?> call() { return null; } }",
        "Outer.java",
        "public class Outer<T> { public interface Ordered<T extends Enum<T>> extends Comparable<T> {}"
            + " public enum Api implements Ordered<Api> { VALUE; public native void call(); }"
            + " public enum Reference implements java.util.function.Supplier<Outer<String>> { VALUE; public native Outer<String> get(); }"
            + " public static class Caller extends Base { public native Api call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericSupportingInterfaceConflict() throws Exception {
    compile(classes, expected, "Left.java", "public interface Left<T> { default T value(T input) { return null; } }",
        "Right.java", "public interface Right<T> { T value(T input); }", "Outer.java",
        "public class Outer { public interface Contract<T extends CharSequence> extends Left<T>, Right<T> { T value(T input); }"
            + " public enum Api implements Contract<String> { VALUE { public String value(String input) { return null; } }; public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericEnumNativeContract() throws Exception {
    compile(classes, expected, "Sink.java", "public interface Sink<T> { void accept(T value); }", "Api.java",
        "public enum Api implements Sink<String> { VALUE; public native void accept(String value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRawGenericEnumContract() throws Exception {
    compile(classes, expected, "Source.java", "public interface Source<T extends Number> { T get(); }", "Api.java",
        "public enum Api implements Source { VALUE { public Number get() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testErasedSupportingRecordContract() throws Exception {
    String version = System.getProperty("java.specification.version");
    if (version.startsWith("1.") || Integer.parseInt(version) < 16) {
      return;
    }
    compile(classes, expected, "Container.java",
        "public record Container<T>(T value) implements java.util.function.Supplier<T> {"
            + " public T get() { return value; } public static class Api { public native Container<String> call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testNestedParameterizedSignatureErasure() throws Exception {
    compile(classes, expected, "Generic.java",
        "public interface Generic<T> { java.util.List<T>[] convert(java.util.List<? extends T> value); }", "Text.java",
        "public interface Text extends Generic<String> {}", "Api.java",
        "public enum Api implements Text { VALUE { public java.util.List<String>[] convert(java.util.List<? extends String> value) { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testEnumSpecializedGenericContract() throws Exception {
    compile(classes, expected, "Text.java", "public interface Text extends java.util.function.Supplier<String> {}",
        "Api.java",
        "public enum Api implements Text { VALUE { public String get() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRecordSpecializedGenericContract() throws Exception {
    String version = System.getProperty("java.specification.version");
    if (version.startsWith("1.") || Integer.parseInt(version) < 16) {
      return;
    }
    compile(classes, expected, "Text.java", "public interface Text extends java.util.function.Consumer<String> {}",
        "Container.java",
        "public record Container(int x) implements Text { public void accept(String s) {} public static class Api { public native Container call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedCovariantBridgeDefaultConflict() throws Exception {
    compile(classes, expected, "Wide.java", "public interface Wide { default Object value() { return null; } }",
        "Narrow.java", "public interface Narrow { String value(); }", "Base.java",
        "public class Base { public String value() { return null; } }", "Api.java",
        "public class Api extends Base implements Wide, Narrow { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedFinalCovariantBridgeDefaultConflict() throws Exception {
    compile(classes, expected, "Wide.java", "public interface Wide { default Object value() { return null; } }",
        "Narrow.java", "public interface Narrow { String value(); }", "Base.java",
        "public class Base { public final String value() { return null; } }", "Api.java",
        "public class Api extends Base implements Wide, Narrow { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testTransitiveGenericContracts() throws Exception {
    compile(classes, expected, "Transform.java", "public interface Transform<A, B> { B[][] convert(A[] input); }",
        "Flip.java", "public interface Flip<X, Y> extends Transform<Y, X> {}", "Text.java",
        "public interface Text extends Flip<String, Integer> {}", "Lists.java",
        "public interface Lists<T> extends java.util.function.Supplier<java.util.List<T>> {}", "Strings.java",
        "public interface Strings extends Lists<String> {}", "Api.java",
        "public enum Api implements Text, Strings { VALUE {"
            + " public String[][] convert(Integer[] input) { return null; }"
            + " public java.util.List<String> get() { return null; } }; public native void call(); }",
        "Sink.java", "public interface Sink extends java.util.function.Consumer<String> {}", "Native.java",
        "public enum Native implements Sink { VALUE; public native void accept(String value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericMethodBoundsAndShadowing() throws Exception {
    compile(classes, expected, "Generic.java",
        "public interface Generic<T> { <T extends Number> T value(); <U extends T> U adjust(U input); }", "Text.java",
        "public interface Text extends Generic<CharSequence> {}", "Api.java",
        "public enum Api implements Text { VALUE {" + " public <T extends Number> T value() { return null; }"
            + " public <U extends CharSequence> U adjust(U input) { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRawAndGeneratedGenericInterfaceContracts() throws Exception {
    compile(classes, expected, "Generic.java",
        "public interface Generic<T> extends java.util.function.Supplier<String> {}", "Api.java",
        "public enum Api implements Generic<Integer> { VALUE { public String get() { return null; } }; public native void call(); }",
        "Outer.java", "public class Outer { public interface Generic<T> extends java.util.function.Supplier<T> {}"
            + " public enum Api implements Generic<String> { VALUE { public String get() { return null; } }; public native void call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSpecializedCovariantContracts() throws Exception {
    compile(classes, expected, "Text.java", "public interface Text extends java.util.function.Supplier<String> {}",
        "Wide.java", "public interface Wide { Object get(); }", "Api.java",
        "public enum Api implements Text, Wide { VALUE { public String get() { return null; } }; public native void call(); }",
        "Reverse.java",
        "public enum Reverse implements Wide, Text { VALUE { public String get() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedGenericImplementation() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base<T> implements java.util.function.Supplier<T> { public final T get() { return null; } }",
        "Strings.java", "public class Strings extends Base<String> {}", "Text.java",
        "public interface Text extends java.util.function.Supplier<String> {}", "Wide.java",
        "public interface Wide { default Object get() { return null; } }", "Api.java",
        "public class Api extends Strings implements Text, Wide { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUnrelatedGenericContractTypeRemainsUnresolved() throws Exception {
    compile(classes, expected, "Missing.java", "public class Missing {}", "Pair.java",
        "public interface Pair<A> extends java.util.function.Supplier<A> { default java.util.List<Missing> ignored() { return null; } }",
        "Text.java", "public interface Text extends Pair<String> {}", "Api.java",
        "public enum Api implements Text { VALUE { public String get() { return null; } }; public native void call(); }");
    Files.delete(new File(classes, "Missing.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testExplicitComparableEnum() throws Exception {
    compile(classes, expected, "Api.java",
        "public enum Api implements Comparable<Api> { VALUE; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericInterfaceAlsoInheritedFromBase() throws Exception {
    compile(classes, expected, "Value.java", "public interface Value<T> { T value(); }", "Base.java",
        "public abstract class Base implements Value<String> {}", "Api.java",
        "public abstract class Api extends Base implements Value<String> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testClassResolvesDefaultConflict() throws Exception {
    compile(classes, expected, "Left.java", "public interface Left { default int value() { return 1; } }", "Right.java",
        "public interface Right { default int value() { return 2; } }", "Api.java",
        "public class Api implements Left, Right { public int value() { return 3; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testEnumResolvesDefaultConflict() throws Exception {
    compile(classes, expected, "Left.java", "public interface Left { default int value() { return 1; } }", "Right.java",
        "public interface Right { default int value() { return 2; } }", "Api.java",
        "public enum Api implements Left, Right { VALUE; public int value() { return 3; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testEnumConstantCovariantContracts() throws Exception {
    compile(classes, expected, "Narrow.java", "public interface Narrow { String value(); }", "Wide.java",
        "public interface Wide { Object value(); }", "Api.java",
        "public enum Api implements Narrow, Wide { VALUE { public String value() { return null; } }; public native void call(); }",
        "Reverse.java",
        "public enum Reverse implements Wide, Narrow { VALUE { public String value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testDefaultOverrideAncestryAndUnrelatedMissingTypes() throws Exception {
    compile(classes, expected, "Missing.java", "public class Missing {}", "Root.java",
        "public interface Root { default int value() { return 1; } default Missing ignored() { return null; } }",
        "Left.java", "public interface Left extends Root { default int value() { return 2; } }", "Right.java",
        "public interface Right extends Root {}", "Required.java",
        "public interface Required extends Left { int value(); }", "Api.java",
        "public class Api implements Right, Left { public native void call(); }", "Reverse.java",
        "public class Reverse implements Left, Right { public native void call(); }", "PerConstant.java",
        "public enum PerConstant implements Required { VALUE { public int value() { return 3; } }; public native void call(); }");
    Files.delete(new File(classes, "Missing.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testDefaultConflictThroughSuperclassAndSupportingDeclarations() throws Exception {
    compile(classes, expected, "Left.java", "public interface Left { default int value() { return 1; } }", "Right.java",
        "public interface Right { default int value() { return 2; } }", "Base.java",
        "public class Base implements Left { public final int value() { return 3; } }", "Api.java",
        "public class Api extends Base implements Right { public native void call(); }", "Native.java",
        "public class Native implements Left, Right { public native int value(); }", "Outer.java",
        "public class Outer implements Left, Right { public int value() { return 3; }"
            + " public static class Api extends Outer { public native void call(); } }",
        "Enclosing.java",
        "public interface Enclosing extends Left, Right { int value();"
            + " public class Api { public native Enclosing call(); } }",
        "Broad.java", "public class Broad { public Object text() { return null; } }", "First.java",
        "public interface First { default String text() { return null; } }", "Second.java",
        "public interface Second { default String text() { return null; } }", "Narrow.java",
        "public class Narrow extends Broad implements First, Second {"
            + " public String text() { return null; } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRedundantGenericSuperinterfaces() throws Exception {
    compile(classes, expected, "Value.java", "public interface Value<T> { T value(); }", "Text.java",
        "public interface Text extends Value<String> {}", "Api.java",
        "public abstract class Api implements Value<String>, Text { public native void call(); }", "Reverse.java",
        "public abstract class Reverse implements Text, Value<String> { public native void call(); }", "Base.java",
        "public abstract class Base implements Text {}", "Inherited.java",
        "public abstract class Inherited extends Base implements Value<String>, Text { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCovariantArrayAndInterfaceContracts() throws Exception {
    compile(classes, expected, "Narrow.java", "public interface Narrow { String[][] items(); java.util.List value(); }",
        "Wide.java", "public interface Wide { Object[] items(); java.util.Collection value(); }", "Api.java",
        "public enum Api implements Narrow, Wide { VALUE { public String[][] items() { return null; }"
            + " public java.util.List value() { return null; } }; public native void call(); }",
        "Reverse.java",
        "public enum Reverse implements Wide, Narrow { VALUE { public String[][] items() { return null; }"
            + " public java.util.List value() { return null; } }; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRecordResolvesDefaultConflict() throws Exception {
    String version = System.getProperty("java.specification.version");
    if (version.startsWith("1.") || Integer.parseInt(version) < 16) {
      return;
    }
    compile(classes, expected, "Left.java", "public interface Left { default int value() { return 1; } }", "Right.java",
        "public interface Right { default int value() { return 2; } }", "Container.java",
        "public record Container(int value) implements Left, Right {"
            + " public static class Api { public native Container call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCovariantInterfaceReturn() throws Exception {
    compile(classes, expected, "Marker.java", "public interface Marker {}", "Base.java",
        "public class Base { public Marker call() { return null; } }", "Api.java",
        "public class Api extends Base implements Marker { public native Api call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCovariantEnclosingRecordReturn() throws Exception {
    String version = System.getProperty("java.specification.version");
    if (version.startsWith("1.") || Integer.parseInt(version) < 16) {
      return;
    }
    compile(classes, expected, "Base.java", "public class Base { public Record call() { return null; } }",
        "Container.java",
        "public record Container(int x) { public static class Api extends Base { public native Container call(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testManifestClassPath() throws Exception {
    File dep = directory("dependency");
    compile(dep, null, "dep/Base.java", "package dep; public class Base {}");
    File dependencyJar = jar(dep, null, false);
    File library = directory("library");
    compile(library, null, "Anchor.java", "public class Anchor {}");
    File libraryJar = new File(work, "library.jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, "dependency.jar");
    try (OutputStream stream = Files.newOutputStream(libraryJar.toPath());
        JarOutputStream out = new JarOutputStream(stream, manifest)) {
      addJarClasses(out, library, "");
    }
    compileWithPath(classes, expected, Arrays.asList(libraryJar), "Api.java",
        "public class Api extends dep.Base { public native void call(); }");
    File javah = TestJavah.jdkTool("javah");
    if (javah.isFile()) {
      File legacy = directory("legacy");
      Process process = new ProcessBuilder(javah.getPath(), "-classpath", classes + File.pathSeparator + libraryJar,
          "-d", legacy.getPath(), "Api").inheritIO().start();
      assertEquals("Legacy javah resolves manifest Class-Path", 0, process.waitFor());
      assertTrue(new File(legacy, "Api.h").isFile());
      System.out.println("Legacy javah control passed with manifest Class-Path");
    }
    generate(classes, Arrays.asList(classes, libraryJar), Collections.<String> emptySet(),
        Collections.<String> emptySet());
    equalHeaders();
  }

  public void testCovariantNestedInterfaceReturnWithUnrelatedMissingType() throws Exception {
    compile(classes, expected, "Missing.java", "public class Missing {}", "Root.java",
        "public interface Root { Missing ignored(); }", "Base.java",
        "public class Base { public Root call() { return null; } }", "Outer.java",
        "public class Outer { public interface Marker extends Root {}"
            + " public abstract static class Api extends Base implements Marker { public native Api call(); } }");
    Files.delete(new File(classes, "Missing.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testEnumInterfaceContract() throws Exception {
    compile(classes, expected, "Api.java",
        "public enum Api implements Runnable { VALUE; public void run() {} public native Api call(); }",
        "PerConstant.java",
        "public enum PerConstant implements Runnable { VALUE { public void run() {} }; public native void call(); }",
        "Identity.java", "public interface Identity { boolean equals(Object o); int hashCode(); }", "Inherited.java",
        "public enum Inherited implements Identity { VALUE; public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRecordInterfaceContractAndConstantOnlyTarget() throws Exception {
    String version = System.getProperty("java.specification.version");
    if (version.startsWith("1.") || Integer.parseInt(version) < 16) {
      return;
    }
    compile(classes, expected, "Base.java",
        "public class Base { public java.util.function.IntSupplier call() { return null; } }", "Container.java",
        "public record Container(int value) implements java.util.function.IntSupplier, java.util.function.Supplier<String> {"
            + " @java.lang.annotation.Native public static final int VALUE=42; public int getAsInt() { return value; }"
            + " public String get() { return Integer.toString(value); }"
            + " public static class Api extends Base { public native Container call(); } }");
    generate(classes, Arrays.asList(classes), set("Container"), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSealedInterfaceOnModernJdk() throws Exception {
    String version = System.getProperty("java.specification.version");
    if (version.startsWith("1.") || Integer.parseInt(version) < 17) {
      return;
    }
    compile(classes, expected, "Marker.java", "public sealed interface Marker permits Api {}", "Base.java",
        "public class Base { public Marker call() { return null; } }", "Api.java",
        "public final class Api extends Base implements Marker { public native Api call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testManifestClassPathOrderAndCycles() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "dep/Base.java",
        "package dep; public class Base { public static final int VALUE=1; protected Base(String value) {} }");
    boolean modern = compilerRelease() >= 11;
    jar(modern ? "dependency one.jar" : "dependency.jar", dependency, "library.jar");
    File other = directory("other");
    compile(other, null, "dep/Base.java",
        "package dep; public class Base { public static final int VALUE=2; protected Base(int value) {} }");
    File otherJar = jar("other.jar", other, null);
    File empty = directory("empty");
    File library = jar("library.jar", empty, "bridge.jar missing.jar bridge.jar");
    jar("bridge.jar", empty, "library.jar " + (modern ? "dependency%20one.jar" : "dependency.jar"));
    // A transitive manifest entry precedes later explicit entries, but never
    // earlier ones.
    for (List<File> paths : Arrays.asList(Arrays.asList(library, otherJar), Arrays.asList(otherJar, library))) {
      compileWithPath(classes, expected, paths, "Api.java",
          "public class Api extends dep.Base {" + " public Api() { super("
              + (paths.get(0).equals(library) ? "null" : "0") + "); } public native void call(); }");
      List<File> classpath = new ArrayList<File>(Arrays.asList(classes));
      classpath.addAll(paths);
      generate(classes, classpath, Collections.<String> emptySet(), Collections.<String> emptySet());
      equalHeaders();
    }
    // The containing JAR is searched before its own manifest entries.
    jar("library.jar", other, "bridge.jar");
    compileWithPath(classes, expected, Arrays.asList(library), "Api.java",
        "public class Api extends dep.Base {" + " public Api() { super(0); } public native void call(); }");
    generate(classes, Arrays.asList(classes, library), Collections.<String> emptySet(),
        Collections.<String> emptySet());
    equalHeaders();
  }

  public void testManifestDirectoryClassPath() throws Exception {
    boolean modern = compilerRelease() >= 11;
    File dependency = directory(modern ? "dependency classes" : "dependency");
    compile(dependency, null, "dep/Base.java", "package dep; public class Base {}");
    File library = jar("library.jar", directory("empty"), modern ? "dependency%20classes/" : "dependency/");
    compileWithPath(classes, expected, Arrays.asList(library), "Api.java",
        "public class Api extends dep.Base { public native void call(); }");
    generate(classes, Arrays.asList(classes, library), Collections.<String> emptySet(),
        Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRecordSupportingInterfaceOmitsUnrelatedMembers() throws Exception {
    if (compilerRelease() < 16) {
      return;
    }
    compile(classes, expected, "Missing.java", "public class Missing {}", "Outer.java",
        "public class Outer { public interface Value { Missing ignored(); }"
            + " public record Container(Missing value) implements Value { public Missing ignored() { return value; } }"
            + " public static class Api { public native Container call(); } }");
    Files.delete(new File(classes, "Missing.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testManifestPathsUseSelectedCompilerVersion() throws Exception {
    File literal = directory("literal");
    File decoded = directory("decoded");
    compile(literal, null, "Api.java", "public class Api { public static final int VALUE=8; }");
    compile(decoded, null, "Api.java", "public class Api { public static final int VALUE=11; }");
    jar("dependency%20one.jar", literal, null);
    jar("dependency one.jar", decoded, null);
    File library = jar("library.jar", directory("empty"), "dependency%20one.jar");
    File javaHome = TestJavah.jdkTool("javac").getCanonicalFile().getParentFile().getParentFile();
    for (int release : new int[] {
        8, 9, 10, 11, 21
    }) {
      try (JniClassPath metadata = new JniClassPath(Arrays.asList(library), javaHome, release)) {
        assertEquals("Manifest interpretation for compiler JDK " + release, Integer.valueOf(release < 11 ? 8 : 11),
            metadata.resolve("Api").constants.get(0).value);
      }
    }
  }

  private static int compilerRelease() {
    String version = System.getProperty("java.specification.version");
    return Integer.parseInt(version.startsWith("1.") ? version.substring(2) : version);
  }

  public void testNestedDeclarationsAndSignatures() throws Exception {
    compile(classes, expected, "p/Outer.java", "package p; public class Outer {"
        + " public static class Static { private native long call(int[] v); public static native Throwable call(Throwable v);"
        + "  public native Inner sibling(Inner v); public native Support support(Support v); public static class Deep { native String[] array(String... values); } }"
        + " public class Inner { protected native double call(double[][] v); public class Deep { public native void call(); } }"
        + " public static class Support { public native void ignored(); } }", "p/Outer$Dollar.java",
        "package p; public class Outer$Dollar { native char call(char c); }", "p/Interface.java",
        "package p; public interface Interface { class Nested { public native boolean call(); } }",
        "DefaultNative.java", "public class DefaultNative { public native void call(); }", "p/Choice.java",
        "package p; public enum Choice { FIRST; public native Choice call(Choice c); }");
    Set<String> excludes = set("**/Outer$Support.class");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), excludes);
    Files.delete(new File(expected, "p_Outer_Support.h").toPath());
    equalHeaders();
    assertFalse("Support-only outer must not request a header", new File(actual, "p_Outer.h").exists());
    // Recreate the class directory in reverse order; graph construction must not
    // depend on discovery order.
    File reversed = directory("reversed");
    List<File> files = classFiles(classes);
    Collections.reverse(files);
    for (File file : files) {
      File destination = new File(reversed, classes.toPath().relativize(file.toPath()).toString());
      Files.createDirectories(destination.getParentFile().toPath());
      Files.copy(file.toPath(), destination.toPath());
    }
    generate(reversed, Arrays.asList(reversed), Collections.<String> emptySet(), excludes);
    equalHeaders();
  }

  public void testConstantsConstructorsAndThrowableHierarchy() throws Exception {
    compile(classes, expected, "p/Base.java",
        "package p; public abstract class Base<T> {"
            + " public static final int INHERITED=42, HIDDEN=1; private static final long PRIVATE=7L;"
            + " protected Base(String name, int count) throws Exception {} public abstract void ignored(T value); }",
        "p/Api.java",
        "package p; public abstract class Api extends Base<String> {"
            + " public static final int HIDDEN=2; public static final long BIG=1234567890123L, MIN=Long.MIN_VALUE;"
            + " public static final boolean YES=true; public static final char LETTER='x';"
            + " public static final byte BYTE=-2; public static final short SHORT=4;"
            + " public static final float NAN=Float.NaN, INFINITY=Float.NEGATIVE_INFINITY, FLOAT=-0.0f;"
            + " public static final double DOUBLE=1.25, DINF=Double.POSITIVE_INFINITY;"
            + " public Api() throws Exception { super(null, 0); }"
            + " native Failure call(Failure failure); native void overloaded(int a); native void overloaded(long a);"
            + " void overloaded(String a) {} public static class Failure extends Exception {} }",
        "p/Outer.java",
        "package p; public class Outer { public class Base {" + " protected Base(int a) throws Exception {} } }",
        "p/Child.java", "package p; public class Child extends Outer.Base {"
            + " public Child(Outer o) throws Exception { o.super(1); } native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
    String api = text(new File(actual, "p_Api.h"));
    assertTrue(api.contains("jthrowable"));
    assertTrue(api.contains("1234567890123"));
  }

  public void testGeneratedSuperclassAndInnerSuperclass() throws Exception {
    compile(classes, expected, "p/Outer.java", "package p; public class Outer { public class Base {"
        + " protected Base(String s) throws Exception {} native void base(); }"
        + " public class Child extends Base { public Child() throws Exception { super(null); } native void call(); } }",
        "p/Base.java", "package p; public class Base { protected Base(int i) {} native void base(); }", "p/Child.java",
        "package p; public class Child extends Base { public Child() { super(0); } native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testDependencyOnlyAndConstantOnlyExtraClasses() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, expected, "dep/Outer.java",
        "package dep; public class Outer { public static class Api { public native void call(); } }",
        "dep/Constants.java", "package dep; public class Constants {"
            + " @java.lang.annotation.Native public static final long VALUE=42L; }");
    File jar = jar(dependency, null, false);
    generate(classes, Arrays.asList(jar), set("dep.Outer$Api", "dep.Constants"), Collections.<String> emptySet());
    equalHeaders();
    assertFalse(text(new File(actual, "dep_Constants.h")).contains("__nar_header"));
  }

  public void testUnrelatedMissingDependencyMembersAreNotResolved() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "dep/Missing.java", "package dep; public class Missing {}", "dep/Payload.java",
        "package dep; public class Payload { public Missing field; public Missing ignored(Missing x) { return x; } }",
        "dep/Base.java",
        "package dep; public abstract class Base { protected Base() {} public abstract Missing ignored(); }");
    compileWithPath(classes, expected, Arrays.asList(dependency), "Api.java",
        "public abstract class Api extends dep.Base { public native dep.Payload call(dep.Payload p); }");
    Files.delete(new File(dependency, "dep/Missing.class").toPath());
    generate(classes, Arrays.asList(classes, dependency), Collections.<String> emptySet(),
        Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMissingRequiredTypesAndSuperclass() throws Exception {
    compile(classes, expected, "Missing.java", "public class Missing {}", "Api.java",
        "public class Api { public native Missing call(); }");
    Files.delete(new File(classes, "Missing.class").toPath());
    failure("Missing class definition: Missing", Collections.<String> emptySet());
    FileUtils.deleteDirectory(classes);
    classes.mkdirs();
    compile(classes, expected, "Base.java", "public class Base {}", "Api.java",
        "public class Api extends Base { public native void call(); }");
    Files.delete(new File(classes, "Base.class").toPath());
    failure("Missing class definition: Base", Collections.<String> emptySet());
  }

  public void testUnsupportedTargetsAndHeaderCollisions() throws Exception {
    compile(classes, expected, "Outer.java",
        "public class Outer { Object make() { class Local { native void call();"
            + " class Member { native void call(); } } return new Local(); }"
            + " Object anonymous = new Object() { public native void call(); }; }");
    for (String target : Arrays.asList("Outer$1Local", "Outer$1Local$Member", "Outer$1")) {
      try {
        generate(classes, Arrays.asList(classes), set(target), set("**/*.class"));
        fail("Expected unsupported local/anonymous target " + target);
      } catch (IOException ex) {
        assertTrue(ex.getMessage(), ex.getMessage().contains("local/anonymous"));
      }
    }
    FileUtils.deleteDirectory(classes);
    classes.mkdirs();
    failure("JDK/platform", set("java.lang.String"));
    compile(classes, null, "A.java", "public class A { public static class B { native void call(); } }", "A_B.java",
        "public class A_B { native void call(); }");
    failure("filename collision", Collections.<String> emptySet());
  }

  public void testFailedCompilerPreservesPublishedHeaders() throws Exception {
    compile(classes, expected, "Api.java", "public class Api { public native int call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    byte[] original = Files.readAllBytes(new File(actual, "Api.h").toPath());
    // Valid bytecode, but a method name that the selected javac cannot compile as
    // source.
    ClassWriter writer = new ClassWriter(0);
    writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Api", null, "java/lang/Object", null);
    writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "call", "()V", null, null).visitEnd();
    writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "call", "()I", null, null).visitEnd();
    writer.visitEnd();
    Files.write(new File(classes, "Api.class").toPath(), writer.toByteArray());
    failure("failed (exit", Collections.<String> emptySet());
    assertTrue(Arrays.equals(original, Files.readAllBytes(new File(actual, "Api.h").toPath())));
    String diagnostics = text(new File(work, "generated/javac.log"));
    assertFalse("Unrelated compiler errors must not change constructors",
        diagnostics.contains("Compilation attempt 2:"));
  }

  public void testMultiReleaseJarUsesCompilerVersion() throws Exception {
    File base = directory("base");
    File versioned = directory("versioned");
    compile(base, null, "Api.java",
        "public class Api { public static final int VERSION=8; public native int call(); }");
    compile(versioned, null, "Api.java",
        "public class Api { public static final int VERSION=9; public native long call(); }");
    File jar = jar(base, versioned, true);
    generate(classes, Arrays.asList(jar), set("Api"), Collections.<String> emptySet());
    String header = text(new File(actual, "Api.h"));
    boolean modern = !System.getProperty("java.specification.version").startsWith("1.");
    assertTrue(header.contains("#define Api_VERSION " + (modern ? "9L" : "8L")));
    assertTrue(header.contains(modern ? "jlong" : "jint"));
    File wrapper = jar("wrapper.jar", directory("empty"), "dependency.jar");
    generate(classes, Arrays.asList(wrapper), set("Api"), Collections.<String> emptySet());
    assertEquals("Manifest lookup retains the selected multi-release view", header, text(new File(actual, "Api.h")));
  }

  public void testEnclosingRecordOnModernJdk() throws Exception {
    String version = System.getProperty("java.specification.version");
    if (version.startsWith("1.") || Integer.parseInt(version) < 16) {
      return;
    }
    compile(classes, expected, "Container.java", "public record Container(String value) {"
        + " public static class Api { public native Container call(Container c); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testConstructorSelectionAndDiagnostics() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "dep/Base.java", "package dep; public class Base { private static class Hidden {}"
        + " protected Base(Hidden value) {} protected Base(String value, int count) throws Throwable {} }");
    compileWithPath(classes, expected, Arrays.asList(dependency), "Api.java",
        "public class Api extends dep.Base { public Api() throws Throwable { super(null, 0); }"
            + " native void call(); }");
    generate(classes, Arrays.asList(classes, dependency), Collections.<String> emptySet(),
        Collections.<String> emptySet());
    equalHeaders();
    FileUtils.deleteDirectory(dependency);
    dependency.mkdirs();
    compile(dependency, null, "dep/Missing.java", "package dep; public class Missing {}", "dep/Base.java",
        "package dep; public class Base { protected Base(Missing value) {} }");
    Files.delete(new File(dependency, "dep/Missing.class").toPath());
    try {
      generate(classes, Arrays.asList(classes, dependency), Collections.<String> emptySet(),
          Collections.<String> emptySet());
      fail("Missing constructor argument must be diagnosed");
    } catch (IOException ex) {
      assertTrue(ex.getMessage(), ex.getMessage().contains("Cannot construct superclass dep/Base for Api"));
      assertTrue(ex.getMessage(), ex.getMessage().contains("(Ldep/Missing;)V"));
    }
  }

  public void testProtectedConstructorParameter() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, null, "dep/Base.java", "package dep; public class Base {"
        + " protected static class Parameter {} protected Base(Parameter p) throws Exception {} }");
    compileWithPath(classes, expected, Arrays.asList(dependency), "Api.java",
        "public class Api extends dep.Base { public Api() throws Exception { super(null); } native void call(); }");
    generate(classes, Arrays.asList(classes, dependency), Collections.<String> emptySet(),
        Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMissingSignatureSuperclassAndEnclosingClass() throws Exception {
    compile(classes, expected, "Base.java", "public class Base {}", "Payload.java",
        "public class Payload extends Base {}", "Api.java", "public class Api { native Payload call(); }");
    Files.delete(new File(classes, "Base.class").toPath());
    failure("Missing class definition: Base", Collections.<String> emptySet());
    FileUtils.deleteDirectory(classes);
    classes.mkdirs();
    compile(classes, expected, "Outer.java", "public class Outer { public static class Api { native void call(); } }");
    Files.delete(new File(classes, "Outer.class").toPath());
    failure("Missing class definition: Outer", Collections.<String> emptySet());
  }

  public void testExcludedEnclosingClassDoesNotGenerateHeader() throws Exception {
    compile(classes, expected, "Outer.java",
        "public class Outer { native void outer();" + " public static class Inner { public native void inner(); } }");
    generate(classes, Arrays.asList(classes), set("Outer$Inner"), set("**/*.class"));
    Files.delete(new File(expected, "Outer.h").toPath());
    equalHeaders();
  }

  public void testConstantOnlyInterfaceAndDependencyChanges() throws Exception {
    File dependency = directory("dependency");
    compile(dependency, expected, "Constants.java",
        "public interface Constants {" + " @java.lang.annotation.Native int VALUE=1; }");
    generate(classes, Arrays.asList(dependency), set("Constants"), Collections.<String> emptySet());
    equalHeaders();
    compile(dependency, expected, "Constants.java",
        "public interface Constants {" + " @java.lang.annotation.Native int VALUE=2; }");
    generate(classes, Arrays.asList(dependency), set("Constants"), Collections.<String> emptySet());
    equalHeaders();
    assertTrue(text(new File(actual, "Constants.h")).contains("VALUE 2L"));
  }

  public void testRestrictedTypeNameUsesCompilerLanguage() throws Exception {
    ClassWriter writer = new ClassWriter(0);
    writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "var", null, "java/lang/Object", null);
    writer.visitEnd();
    Files.write(new File(classes, "var.class").toPath(), writer.toByteArray());
    String version = System.getProperty("java.specification.version");
    if (version.startsWith("1.") || Integer.parseInt(version) < 10) {
      generate(classes, Arrays.asList(classes), set("var"), Collections.<String> emptySet());
      assertTrue(new File(actual, "var.h").isFile());
    } else {
      failure("Source-inexpressible type name", set("var"));
      assertFalse("Validate names before emitting sources", new File(work, "generated/sources").exists());
    }
  }

  public void testMetadataValidation() throws Exception {
    ClassWriter writer = new ClassWriter(0);
    writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Bad-Name", null, "java/lang/Object", null);
    writer.visitEnd();
    Files.write(new File(classes, "Bad-Name.class").toPath(), writer.toByteArray());
    failure("Source-inexpressible", set("Bad-Name"));
    FileUtils.deleteDirectory(classes);
    classes.mkdirs();
    malformedMember("A", "B", "A");
    malformedMember("B", "A", "B");
    failure("Containment cycle", set("A"));
  }

  public void testGenericConstructorInferredUncheckedException() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "Base.java",
        "import java.lang.Exception; public class Base { protected <E extends Exception> Base() throws E {} }",
        "java.java", "public class java extends Base { public native void call(Exception e, Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAlternativeNonthrowingConstructor() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "Base.java",
        "import java.lang.Exception; public class Base { protected Base() throws Exception {} protected Base(int value) {} }",
        "java.java",
        "public class java extends Base { public java(){super(0);} public native void call(Exception e, Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testNestedExceptionsAcrossGeneratedParents() throws Exception {
    compile(classes, expected, "Owner.java",
        "public class Owner { public static class Problem extends Exception { public native void problem(); } public static class Base { protected Base() throws Problem {} } }",
        "Middle.java",
        "public class Middle extends Owner.Base { public Middle() throws Owner.Problem {} public native void middle(); }",
        "Api.java",
        "public class Api extends Middle { public Api() throws Owner.Problem {} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericConstructorWithRecursiveBoundsAndThrows() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { protected <E extends Exception & Runnable> Base(java.util.List<E> values) throws E {} }",
        "Api.java",
        "public class Api extends Base { public Api() throws Exception { super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericConstructorOwnerAndExceptionBindings() throws Exception {
    compile(classes, expected, "Owner.java",
        "public class Owner<E extends Exception> { public class Base { protected <T extends Comparable<T>> Base(java.util.List<T> values) throws E {} } }",
        "Api.java",
        "public class Api extends Owner<java.io.IOException>.Base { public Api(Owner<java.io.IOException> o) throws java.io.IOException { o.super(null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedArrayReturnMeetsBothContracts() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { public Object[] value(){return null;} }",
        "Middle.java",
        "public class Middle extends Base { public String[] value(){return null;} public native void middle(); }",
        "Contract.java", "public interface Contract { java.io.Serializable value(); }", "Api.java",
        "public class Api extends Middle implements Contract { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testPreciseConstructorExceptionMissingMarker() throws Exception {
    compile(classes, expected, "Marker.java", "public interface Marker {}", "Problem.java",
        "public class Problem extends RuntimeException implements Marker {}", "Base.java",
        "public class Base { protected Base() throws Problem {} }", "Api.java",
        "public class Api extends Base { public native void call(); }");
    assertTrue(new File(classes, "Marker.class").delete());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericConstructorInferredUncheckedThrowable() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "Base.java",
        "import java.lang.Throwable; public class Base { protected <E extends Throwable> Base() throws E {} }",
        "java.java", "public class java extends Base { public native void call(Exception e, Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericConstructorDependentExceptionBounds() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base {" + " protected <T extends Exception, E extends T> Base(T value) throws E {} }", "Api.java",
        "public class Api extends Base { public Api() throws Exception { super(null); }"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericConstructorSpecificCheckedException() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base {" + " protected <E extends java.io.IOException> Base() throws E {} }", "Api.java",
        "public class Api extends Base { public Api() throws java.io.IOException {} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAlternativeConstructorWithSupportingMember() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "Owner.java",
        "public class Owner { public native void owner(); public static class Argument {} }", "Base.java",
        "import java.lang.Exception; public class Base {"
            + " protected Base() throws Exception {} protected Base(Owner.Argument value) {} }",
        "java.java", "public class java extends Base { public java(){super(null);}"
            + " public native void call(Exception e, Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAlternativeConstructorBesideMandatoryImport() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "q/Arg.java", "package q; public class Arg {}",
        "Base.java", "public class Base { protected Base(Arg value) {} protected Base(String value) {} }", "q.java",
        "import q.Arg; public class q extends Base { public q(){super((String)null);} public native Arg call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRejectedConstructorDoesNotKeepImports() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "one/Problem.java", "package one; public class Problem extends Exception {}",
        "two/Problem.java", "package two; public class Problem {}", "Base.java",
        "import java.lang.Exception; public class Base {"
            + " protected Base() throws one.Problem, Exception {} protected Base(two.Problem value) {} }",
        "java.java", "public class java extends Base { public static class one {} public static class two {}"
            + " public java(){super(null);} public native void call(Exception e, Throwable t, one a, two b); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testOnlyConstructorCanUseUncastNullBesideImport() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "q/Arg.java", "package q; public class Arg {}",
        "Base.java", "public class Base { protected Base(Arg value) {} }", "q.java",
        "import q.Arg; public class q extends Base { public q(){super(null);} public native Arg call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testDefaultPackageConstructorExceptionBesideImport() throws Exception {
    compile(classes, expected, "Problem.java", "public class Problem extends Exception {}", "q/Problem.java",
        "package q; public class Problem {}", "Base.java", "public class Base { protected Base() throws Problem {} }",
        "q.java",
        "import q.Problem; public class q extends Base { public q() throws Exception {} public native Problem call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedSuperclassNeedsAlternativeForSubclass() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "Base.java",
        "import java.lang.Exception; public class Base { protected Base() throws Exception {} protected Base(int value) {} }",
        "Parent.java", "public class Parent extends Base { public Parent(){super(0);} public native void parent(); }",
        "java.java", "public class java extends Parent { public native void call(Exception e, Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGenericExceptionWithTwoSymbolicLowerBounds() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { protected <E extends Exception,T extends E,U extends E> Base(T a,U b) throws E {} }",
        "Api.java",
        "public class Api extends Base { public Api() throws Exception { super(null,null); } public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSeveralIndependentGenericExceptions() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { protected <E extends Exception,F extends java.io.IOException> Base() throws E,F {} }",
        "Api.java",
        "public class Api extends Base { public Api() throws java.io.IOException {} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testNestedSourceConstructorOrderControl() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { protected Base() throws java.io.IOException {} }",
        "Owner.java",
        "public class Owner { public static class A extends Z { public A() throws java.io.IOException {} public native void a(); } public static class Z extends Base { public Z() throws java.io.IOException {} public native void z(); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testNestedDefaultPackageExceptionBesideImport() throws Exception {
    compile(classes, expected, "Owner.java", "public class Owner { public static class Problem extends Exception {} }",
        "q/Owner.java", "package q; public class Owner {}", "Base.java",
        "public class Base { protected Base() throws Owner.Problem {} }", "q.java",
        "import q.Owner; public class q extends Base { public q() throws Exception {} public native Owner call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUncheckedInvocationErasesGenericThrows() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { private static class Hidden {} protected <E extends Exception> Base(java.util.List<Hidden> value) throws E {} }",
        "Api.java", "public class Api extends Base { public Api(){super(null);} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUncheckedInvocationErasesNarrowGenericThrows() throws Exception {
    compile(classes, expected, "Base.java",
        "public class Base { private static class Hidden {} protected <E extends java.io.IOException> Base(java.util.List<Hidden> value) throws E {} }",
        "Api.java",
        "public class Api extends Base { public Api() throws java.io.IOException {super(null);} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUncastNullPreservesOtherArgumentForOverloadSelection() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "q/Arg.java", "package q; public class Arg {}",
        "Base.java", "public class Base { protected Base(Arg a, String b) {} protected Base(Arg a, Integer b) {} }",
        "q.java", "import q.Arg; public class q extends Base {"
            + " public q(){super(null,(String)null);} public native Arg call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testUncastNullBesideImportsStillRejectsAmbiguity() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "Other.java", "public class Other {}", "q/Arg.java",
        "package q; public class Arg {}", "q/Other.java", "package q; public class Other {}", "Factory.java",
        "public class Factory { public static Arg argument(){return null;} }", "Base.java",
        "public class Base { protected Base(Arg value) {} protected Base(Other value) {} }", "q.java",
        "import q.Arg; import q.Other; public class q extends Base {"
            + " public q(){super(Factory.argument());} public native Arg call(Other value); }");
    Files.createDirectories(actual.toPath());
    File published = new File(actual, "q.h");
    Files.write(published.toPath(), "previous header".getBytes(StandardCharsets.UTF_8));
    failure("ambiguous constructor", Collections.<String> emptySet());
    assertEquals("previous header", text(published));
    assertFalse("Reject ambiguous calls before compilation", new File(work, "generated/javac.log").exists());
  }

  public void testGeneratedAncestorChoiceIncludesWideningInIntermediateClass() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "Problem.java",
        "import java.lang.Exception; public class Problem extends Exception {}", "q/Problem.java",
        "package q; public class Problem {}", "Base.java",
        "public class Base { protected Base() throws Problem {} protected Base(int value) {} }", "Parent.java",
        "public class Parent extends Base { public Parent(){super(0);} public native void parent(); }", "q.java",
        "import q.Problem; public class q extends Parent { public native Problem middle(); }", "java.java",
        "public class java extends q { public native void call(Exception e, Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testDescendantRejectionRollsBackParentImports() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "Problem.java", "public class Problem {}", "one/Problem.java",
        "package one; public class Problem extends Exception {}", "two/Problem.java",
        "package two; public class Problem extends Exception {}", "Base.java",
        "public class Base { protected Base() throws one.Problem {}"
            + " protected Base(int value) throws two.Problem {} }",
        "Owner.java",
        "import two.Problem; public class Owner { public static class one {} public static class two {}"
            + " public static class Parent extends Base { public Parent() throws Problem {super(0);}"
            + " public native void parent(one a, two b); } }",
        "java.java", "public class java extends Owner.Parent { public static class one {}"
            + " public java() throws two.Problem {} public native void call(Exception e, Throwable t, Problem p, one a); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testShadowedGenericBoundKeepsDisambiguatingRawCast() throws Exception {
    compile(classes, expected, "Bound.java", "public class Bound {}", "q/Bound.java",
        "package q; public class Bound {}", "Base.java",
        "public class Base { protected <T extends Bound> Base(java.util.List<T> values) {}"
            + " protected Base(String value) {} }",
        "q.java", "import q.Bound; public class q extends Base { public q(){super((java.util.List)null);}"
            + " public native Bound call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testNestedDefaultPackageExceptionHiddenByMember() throws Exception {
    compile(classes, expected, "Owner.java", "public class Owner { public static class Problem extends Exception {} }",
        "Base.java", "public class Base { protected Base() throws Owner.Problem {} }", "Api.java",
        "public class Api extends Base { public static class Owner {}"
            + " public Api() throws Exception {} public native void call(Owner value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMemberShadowsDefaultPackageConstructorArgument() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "Base.java",
        "public class Base { protected Base(Arg value) {} }", "Api.java",
        "public class Api extends Base { public static class Arg {} public Api(){super(null);} public native void call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testMemberShadowsDefaultPackageConstructorException() throws Exception {
    compile(classes, expected, "Problem.java", "public class Problem extends Exception {}", "Base.java",
        "public class Base { protected Base() throws Problem {} }", "Api.java",
        "public class Api extends Base { public static class Problem {} public Api() throws Exception {} public native void call(Problem value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedMemberShadowsDefaultPackageConstructorArgument() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "Types.java",
        "public interface Types { class Arg {} }", "Base.java", "public class Base { protected Base(Arg value) {} }",
        "Api.java",
        "public class Api extends Base implements Types { public Api(){super(null);} public native void call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSecondDescendantRejectsFirstParentChoice() throws Exception {
    compile(classes, expected, "Exception.java", "public class Exception {}", "Throwable.java",
        "public class Throwable {}", "Base.java",
        "import java.lang.Exception; public class Base { protected Base() throws Exception {} protected Base(int value) {} }",
        "Parent.java", "public class Parent extends Base { public Parent(){super(0);} public native void parent(); }",
        "A.java", "public class A extends Parent { public native void a(); }", "java.java",
        "public class java extends Parent { public native void call(Exception e, Throwable t); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSiblingConstructorsShareSourceNames() throws Exception {
    compile(classes, expected, "p/Problem.java", "package p; public class Problem extends Exception {}",
        "q/Problem.java", "package q; public class Problem extends Exception {}", "BaseP.java",
        "public class BaseP { protected BaseP() throws p.Problem {} }", "BaseQ.java",
        "public class BaseQ { protected BaseQ() throws q.Problem {} }", "Owner.java",
        "public class Owner { public static class p {} public static class q {} public static class A extends BaseP { public A() throws Exception {} public native void a(p v); } public static class B extends BaseQ { public B() throws Exception {} public native void b(q v); } }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSelfNamedConstructorArgumentControl() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { protected Base(Api value) {} }", "Api.java",
        "public class Api extends Base { public Api(){super(null);} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testOwnNestedExceptionControl() throws Exception {
    compile(classes, expected, "Base.java", "public class Base { protected Base() throws Api.Problem {} }", "Api.java",
        "public class Api extends Base { public static class Problem extends Exception {} public Api() throws Problem {} public native void call(Problem p); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testInheritedMemberHidesCurrentClassConstructorArgument() throws Exception {
    compile(classes, expected, "Types.java", "public interface Types { class Api {} }", "Base.java",
        "public class Base { protected Base(Api value) {} }", "Api.java",
        "public class Api extends Base implements Types { public Api(){super(null);} public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testOwnNestedExceptionWithHiddenEnclosingName() throws Exception {
    compile(classes, expected, "Types.java", "public interface Types { class Api {} }", "Base.java",
        "public class Base { protected Base() throws Api.Problem {} }", "Api.java",
        "public class Api extends Base implements Types { public static class Problem extends Exception {}"
            + " public Api() throws Problem {} public native void call(Problem p); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testSuperclassArgumentUsesHeaderScope() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "Base.java",
        "public class Base<T> { protected Base(T value) {} }", "Api.java",
        "public class Api extends Base<Arg> { public static class Arg {}"
            + " public Api(){super(null);} public native void call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testAmbiguousInheritedMembersHideDefaultPackageArgument() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "Left.java", "public interface Left { class Arg {} }",
        "Right.java", "public interface Right { class Arg {} }", "Base.java",
        "public class Base { protected Base(Arg value) {} }", "Api.java",
        "public class Api extends Base implements Left, Right {"
            + " public Api(){super(null);} public native void call(Left.Arg left, Right.Arg right); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testLexicalBindingsDoNotResolveUnrelatedMembers() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "Types.java",
        "public interface Types { class Arg {} class Unrelated {} }", "Base.java",
        "public class Base { protected Base(Arg value) {} }", "Api.java",
        "public class Api extends Base implements Types { public Api(){super(null);} public native void call(Arg value); }");
    Files.delete(new File(classes, "Types$Unrelated.class").toPath());
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testGeneratedSuperclassMemberHidesDefaultPackageArgument() throws Exception {
    compile(classes, expected, "Arg.java", "public class Arg {}", "Base.java",
        "public class Base { protected Base(Arg value) {} public native void base(); }", "Parent.java",
        "public class Parent extends Base { public static class Arg {} public Parent(){super(null);}"
            + " public native void parent(Arg value); }",
        "Api.java", "public class Api extends Parent { public native void call(Arg value); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedOuterVariablesKeepDistinctBounds() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A, B> { public class Arg {} }" + " public class Outer<X extends Number> {"
            + " public native X outer(X value);"
            + " public class PublicBase<Y extends CharSequence> extends Hidden<X,Y> {"
            + " public native X outerValue(X value);" + " public native Y innerValue(Y value); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.PublicBase<String>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedOuterVariablesSatisfySuperclassBounds() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A extends Number, B extends CharSequence> { public class Arg {} }"
            + " public class Outer<X extends Number> {" + " public native X outer(X value);"
            + " public class PublicBase<Y extends CharSequence> extends Hidden<X,Y> {"
            + " public native Y innerValue(Y value); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.PublicBase<String>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedOuterVariableWithoutInnerFormalsControl() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A> { public class Arg {} }" + " public class Outer<X extends Number> {"
            + " public native X outer(X value);" + " public class PublicBase extends Hidden<X> {"
            + " public native X value(X value); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.PublicBase.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedInnerVariableShadowsOuterControl() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A> { public class Arg {} }" + " public class Outer<X extends Number> {"
            + " public native X outer(X value);"
            + " public class PublicBase<X extends CharSequence> extends Hidden<X> {"
            + " public native X value(X value); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.PublicBase<String>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testDependentWildcardWithObjectControl() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<A,B> { public class Arg {} }"
            + " public class PublicBase<X,Y extends X> extends Hidden<X,Y> {}",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<Object,? extends Number>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedNativeMethodWithIndependentFormalsControl() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<A> { public class Arg {} }"
            + " public class PublicBase<X extends Number> extends Hidden<X> {"
            + " public native <Y extends CharSequence> X value(X a,Y b); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<Integer>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedVariablesAcrossThreeEnclosingScopes() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A extends Number, B extends CharSequence, C extends Throwable> {"
            + " protected Hidden(A a, B b, C c) {} public class Arg {} }"
            + " public class Outer<X extends Number> { public native X outer(X value);"
            + " public class Middle<Y extends CharSequence> { public native Y middle(Y value);"
            + " public class PublicBase<Z extends Throwable> extends Hidden<X, Y, Z> {"
            + " public PublicBase(X x, Y y, Z z) { super(x, y, z); }"
            + " public native X outerValue(X value); public native Y middleValue(Y value);"
            + " public native Z innerValue(Z value); } } }",
        "q/Api.java",
        "package q; public abstract class Api implements"
            + " java.util.function.Supplier<p.Outer<Integer>.Middle<String>.PublicBase<Exception>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedInnerBoundUsesEnclosingVariable() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A extends Number, B extends A> {"
            + " protected Hidden(A a, B b) {} public class Arg {} }"
            + " public class Outer<X extends Number> { public native X outer(X value);"
            + " public class PublicBase<Y extends X> extends Hidden<X, Y> {"
            + " public PublicBase(X x, Y y) { super(x, y); }"
            + " public native X outerValue(X value); public native Y innerValue(Y value); } }",
        "q/Api.java", "package q; public abstract class Api implements"
            + " java.util.function.Supplier<p.Outer<Number>.PublicBase<Integer>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedSiblingScopes() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A,B> { public class Arg {} }" + " public class Outer<X extends Number> {"
            + " public native X outer(X x);" + " public class Left<Y extends CharSequence> extends Hidden<X,Y> {"
            + " public native X leftOuter(X x);"
            + " public native Y left(Y y); } public class Right<Z extends Throwable> extends Hidden<X,Z> {"
            + " public native X rightOuter(X x);" + " public native Z right(Z z); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<java.util.Map<p.Outer<Integer>.Left<String>.Arg,p.Outer<Integer>.Right<Exception>.Arg>> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedStaticScope() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A> { public class Arg {} }" + " public class Outer<X extends Number> {"
            + " public native X outer(X x); public static class PublicBase<Y extends CharSequence> extends Hidden<Y> {"
            + " public native Y value(Y y); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer.PublicBase<String>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedRecursiveAndDependentBounds() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A extends Number,B extends CharSequence & Comparable<B>,C extends A> { public class Arg {} }"
            + " public class Outer<X extends Number> {" + " public native X outer(X x);"
            + " public class PublicBase<Y extends CharSequence & Comparable<Y>, Z extends X> extends Hidden<X,Y,Z> {"
            + " public native X outerValue(X x);" + " public native Y text(Y y);" + " public native Z number(Z z);"
            + " public native <W extends Z> W dependent(W w); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Number>.PublicBase<String,Integer>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedOriginalAllocatorNames() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A,B> { public class Arg {} }" + " public class Outer<_NarType0 extends Number> {"
            + " public native _NarType0 outer(_NarType0 x);"
            + " public class PublicBase<_NarType1 extends CharSequence> extends Hidden<_NarType0,_NarType1> {"
            + " public native _NarType0 outerValue(_NarType0 x);" + " public native _NarType1 value(_NarType1 y); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.PublicBase<String>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedMethodShadowAndRestore() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A,B> { public class Arg {} }" + " public class Outer<X extends Number> {"
            + " public native X outer(X x);" + " public class PublicBase<Y extends CharSequence> extends Hidden<X,Y> {"
            + " public native <X extends Throwable> X raise(X x) throws X;" + " public native X after(X x);"
            + " public native <Y extends Number> Y number(Y y);" + " public native Y text(Y y); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.PublicBase<String>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedIntermediateShadow() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A,B> { public class Arg {} }" + " public class Outer<X extends Number> {"
            + " public native X outer(X x);" + " public class Middle<X extends CharSequence> {"
            + " public native X middle(X x);" + " public class PublicBase<Y extends Throwable> extends Hidden<X,Y> {"
            + " public native X middleValue(X x);" + " public native Y value(Y y); } } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.Middle<String>.PublicBase<Exception>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedConstructorAndClassNames() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A,B> { protected <T extends B> Hidden(A a,T b) {} public class Arg {} }"
            + " public class Outer<_NarConstructor0 extends Number> {"
            + " public native _NarConstructor0 outer(_NarConstructor0 x);"
            + " public class PublicBase<_NarMethod0 extends CharSequence> extends Hidden<_NarConstructor0,_NarMethod0> { public PublicBase(_NarConstructor0 a,_NarMethod0 b) { super(a,b); } public native _NarConstructor0 outerValue(_NarConstructor0 a);"
            + " public native _NarMethod0 innerValue(_NarMethod0 b); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.PublicBase<String>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedOwnerAndInnerArrays() throws Exception {
    compile(classes, expected, "p/Outer.java",
        "package p; class Hidden<A,B> { public class Arg {} }" + " public class Outer<X extends Number> {"
            + " public native X[] outer(X[] x);"
            + " public class PublicBase<Y extends CharSequence> extends Hidden<X,Y> {"
            + " public native X[][] outerValue(X[][] x);" + " public native Y[] innerValue(Y[] y);"
            + " public native java.util.List<X> list(java.util.List<Y> y); } }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.Outer<Integer>.PublicBase<String>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedClassParameterCapturedByConstructorAlias() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<A> { protected <T extends CharSequence> Hidden(A a,T b) {} public class Arg {} }"
            + " public class PublicBase<_NarConstructor0 extends Number> extends Hidden<_NarConstructor0> { public PublicBase(_NarConstructor0 a) { super(a,\"\"); } public native _NarConstructor0 value(_NarConstructor0 a); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<Integer>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedClassParameterWithDistinctConstructorNamesControl() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<A> { protected <T extends CharSequence> Hidden(A a,T b) {} public class Arg {} }"
            + " public class PublicBase<X extends Number> extends Hidden<X> { public PublicBase(X a) { super(a,\"\"); } public native X value(X a); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<Integer>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedClassParameterMatchesConstructorFormal() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<A> { protected <T extends CharSequence> Hidden(A a,T b) {} public class Arg {} }"
            + " public class PublicBase<T extends Number> extends Hidden<T> { public PublicBase(T a) { super(a,\"\"); } public native T value(T a); }",
        "q/Api.java",
        "package q; public abstract class Api implements java.util.function.Supplier<p.PublicBase<Integer>.Arg> {"
            + " public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedConstructorSpecializesRecursiveDependentFormals() throws Exception {
    compile(classes, expected, "p/PublicBase.java",
        "package p; class Hidden<A> {"
            + " protected <T extends CharSequence & Comparable<T>, U extends T> Hidden(A a, T b, U c) {}"
            + " public class Arg {} } public class PublicBase<T extends Number> extends Hidden<T> {"
            + " public PublicBase(T a) { super(a, \"\", \"\"); } public native T value(T a); }",
        "q/Api.java", "package q; public abstract class Api implements"
            + " java.util.function.Supplier<p.PublicBase<Integer>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  public void testRetainedSupportingMethodSpecializesDistinctFormals() throws Exception {
    compile(classes, expected, "p/Left.java",
        "package p; public interface Left<A> {"
            + " default <T extends CharSequence> java.util.Map<A, T> apply(A a, T b) { return null; } }",
        "p/Right.java",
        "package p; public interface Right<A> {"
            + " default <U extends CharSequence> java.util.Map<A, U> apply(A a, U b) { return null; } }",
        "p/PublicBase.java",
        "package p; class Hidden<A> { public class Arg {} }"
            + " public class PublicBase<T extends Number> extends Hidden<T> implements Left<T>, Right<T> {"
            + " public <U extends CharSequence> java.util.Map<T, U> apply(T a, U b) { return null; }"
            + " public native T value(T a); }",
        "q/Api.java", "package q; public abstract class Api implements"
            + " java.util.function.Supplier<p.PublicBase<Integer>.Arg> { public native void call(); }");
    generate(classes, Arrays.asList(classes), Collections.<String> emptySet(), Collections.<String> emptySet());
    equalHeaders();
  }

  private void malformedMember(String name, String outer, String simple) throws Exception {
    ClassWriter writer = new ClassWriter(0);
    writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
    writer.visitInnerClass(name, outer, simple, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
    writer.visitEnd();
    Files.write(new File(classes, name + ".class").toPath(), writer.toByteArray());
  }

  private void failure(String message, Set<String> extras) throws Exception {
    try {
      generate(classes, Arrays.asList(classes), extras, Collections.<String> emptySet());
      fail("Expected failure containing " + message);
    } catch (IOException ex) {
      assertTrue(ex.getMessage(), ex.getMessage().contains(message));
    }
  }

  private void generate(File scanned, List<File> paths, Set<String> extras, Set<String> excludes) throws Exception {
    new JavacHeaders(TestJavah.jdkTool("javac"), new File(work, "generated"), paths, Collections.<File> emptyList(),
        new SystemStreamLog()).generate(scanned, set("**/*.class"), excludes, extras, actual);
    File diagnostics = new File(work, "generated/javac.log");
    if (diagnostics.isFile()) {
      assertFalse("Select a valid call before compiling", text(diagnostics).contains("Compilation attempt 2:"));
    }
  }

  private void compile(File destination, File headers, String... sources) throws Exception {
    compileWithPath(destination, headers, Collections.<File> emptyList(), sources);
  }

  private void compileWithPath(File destination, File headers, List<File> paths, String... sources) throws Exception {
    File sourceDirectory = Files.createTempDirectory(work.toPath(), "src").toFile();
    List<String> args = new ArrayList<String>(
        Arrays.asList("-proc:none", "-encoding", "UTF-8", "-d", destination.getPath()));
    if (headers != null) {
      Collections.addAll(args, "-h", headers.getPath());
    }
    if (!paths.isEmpty()) {
      StringBuilder path = new StringBuilder();
      for (File entry : paths) {
        if (path.length() != 0) {
          path.append(File.pathSeparator);
        }
        path.append(entry);
      }
      Collections.addAll(args, "-classpath", path.toString());
    }
    for (int i = 0; i < sources.length; i += 2) {
      File file = new File(sourceDirectory, sources[i]);
      Files.createDirectories(file.getParentFile().toPath());
      Files.write(file.toPath(), sources[i + 1].getBytes(StandardCharsets.UTF_8));
      args.add(file.getPath());
    }
    assertEquals("Fixture compilation", 0,
        ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[args.size()])));
  }

  private File jar(File base, File versioned, boolean multiRelease) throws Exception {
    File jar = new File(work, "dependency.jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (multiRelease) {
      manifest.getMainAttributes().putValue("Multi-Release", "true");
    }
    try (OutputStream stream = Files.newOutputStream(jar.toPath());
        JarOutputStream out = new JarOutputStream(stream, manifest)) {
      addJarClasses(out, base, "");
      if (versioned != null) {
        addJarClasses(out, versioned, "META-INF/versions/9/");
      }
    }
    return jar;
  }

  private File jar(String name, File base, String classPath) throws Exception {
    File jar = new File(work, name);
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (classPath != null) {
      manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath);
    }
    try (OutputStream stream = Files.newOutputStream(jar.toPath());
        JarOutputStream out = new JarOutputStream(stream, manifest)) {
      addJarClasses(out, base, "");
    }
    return jar;
  }

  private void addJarClasses(JarOutputStream out, File directory, String prefix) throws Exception {
    for (File file : classFiles(directory)) {
      out.putNextEntry(new JarEntry(
          prefix + directory.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')));
      Files.copy(file.toPath(), out);
      out.closeEntry();
    }
  }

  private List<File> classFiles(File directory) {
    List<File> result = new ArrayList<File>();
    File[] children = directory.listFiles();
    if (children != null) {
      Arrays.sort(children);
      for (File child : children) {
        if (child.isDirectory()) {
          result.addAll(classFiles(child));
        } else if (child.getName().endsWith(".class")) {
          result.add(child);
        }
      }
    }
    return result;
  }

  private void equalHeaders() throws Exception {
    Set<String> expectedNames = new HashSet<String>(Arrays.asList(expected.list()));
    Set<String> actualNames = new HashSet<String>(Arrays.asList(actual.list()));
    assertEquals("Only requested headers", expectedNames, actualNames);
    for (String name : expectedNames) {
      assertEquals(name, text(new File(expected, name)), text(new File(actual, name)));
    }
  }

  private File directory(String name) throws IOException {
    File directory = new File(work, name);
    Files.createDirectories(directory.toPath());
    return directory;
  }

  private static Set<String> set(String... values) {
    return new HashSet<String>(Arrays.asList(values));
  }

  private static String text(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }
}
