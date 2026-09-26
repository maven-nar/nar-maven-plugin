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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import javax.tools.ToolProvider;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.FileUtils;

import junit.framework.TestCase;

/** Regression for header generation with a JDK toolchain without javah. */
public class TestJavah extends TestCase {
  private File work;

  @Override
  protected void setUp() throws Exception {
    work = Files.createTempDirectory("nar jni headers ").toFile();
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtils.deleteDirectory(work);
  }

  public void testToolchainWithoutJavah() throws Exception {
    File source = new File(work, "NativeApi.java");
    Files.write(source.toPath(), Arrays.asList("public class NativeApi { public native int call(String[] values); }"),
        StandardCharsets.UTF_8);
    File classes = new File(work, "classes");
    File expected = new File(work, "expected");
    classes.mkdirs();
    expected.mkdirs();
    assertNotNull("Run this test with a JDK", ToolProvider.getSystemJavaCompiler());
    assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classes.getPath(), "-h",
        expected.getPath(), source.getPath()));

    final String javac = jdkTool("javac").getAbsolutePath();
    final Toolchain toolchain = new Toolchain() {
      public String getType() {
        return "jdk";
      }

      public String findTool(String tool) {
        return "javac".equals(tool) ? javac : null;
      }
    };
    ToolchainManager manager = (ToolchainManager) Proxy.newProxyInstance(ToolchainManager.class.getClassLoader(),
        new Class<?>[] {
            ToolchainManager.class
        }, new InvocationHandler() {
          public Object invoke(Object proxy, Method method, Object[] arguments) {
            return "getToolchainFromBuildContext".equals(method.getName()) ? toolchain : null;
          }
        });
    Model model = new Model();
    model.setGroupId("test");
    model.setArtifactId("jni");
    model.setVersion("1");
    Build build = new Build();
    build.setDirectory(new File(work, "target").getPath());
    model.setBuild(build);
    MavenProject project = new MavenProject(model);
    project.setFile(new File(work, "pom.xml"));
    NarJavahMojo mojo = new NarJavahMojo();
    set(mojo, AbstractNarMojo.class, "mavenProject", project);
    set(mojo, AbstractNarMojo.class, "classesDirectory", classes);
    set(mojo, AbstractNarMojo.class, "os", System.getProperty("os.name"));
    set(mojo, AbstractNarMojo.class, "aolId", new AOL("amd64-Linux-gpp"));
    // The selected toolchain must be authoritative, even when javaHome has no
    // tools.
    File emptyHome = new File(work, "empty-jdk");
    emptyHome.mkdirs();
    set(mojo, AbstractNarMojo.class, "javaHome", emptyHome);
    set(mojo, NarJavahMojo.class, "toolchainManager", manager);
    File actual = new File(work, "headers");
    Javah headers = new Javah();
    headers.setAbstractCompileMojo(mojo);
    set(headers, Javah.class, "classDirectory", classes);
    set(headers, Javah.class, "jniDirectory", actual);
    set(headers, Javah.class, "classPaths", Arrays.asList(classes));
    headers.execute();
    assertEquals(new String(Files.readAllBytes(new File(expected, "NativeApi.h").toPath()), StandardCharsets.UTF_8),
        new String(Files.readAllBytes(new File(actual, "NativeApi.h").toPath()), StandardCharsets.UTF_8));
  }

  static File jdkTool(String name) {
    File home = new File(System.getProperty("java.home"));
    if ("jre".equals(home.getName())) {
      home = home.getParentFile();
    }
    return new File(new File(home, "bin"), name + (File.separatorChar == '\\' ? ".exe" : ""));
  }

  static void set(Object instance, Class<?> owner, String name, Object value) throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(instance, value);
  }
}
