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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.ToolProvider;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.FileUtils;

import junit.framework.TestCase;

public class TestJavahModes extends TestCase {
  private File work;
  private File classes;
  private File output;
  private final List<String> requested = new ArrayList<String>();

  @Override
  protected void setUp() throws Exception {
    work = Files.createTempDirectory("nar modes ").toFile();
    classes = new File(work, "classes");
    output = new File(work, "headers");
    classes.mkdirs();
    File source = new File(work, "Api.java");
    Files.write(source.toPath(), Arrays.asList("public class Api { public native void call(); }"),
        StandardCharsets.UTF_8);
    assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-d", classes.getPath(),
        source.getPath()));
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtils.deleteDirectory(work);
  }

  public void testEmptySelectionDoesNotRequireCompiler() throws Exception {
    FileUtils.deleteDirectory(classes);
    classes.mkdirs();
    for (String mode : Arrays.asList("auto", "javac", "javah")) {
      configure(mode, null, noTools()).execute();
      assertFalse(output.exists());
    }
  }

  public void testNonNativeSelectionDoesNotRequireCompiler() throws Exception {
    FileUtils.deleteDirectory(classes);
    classes.mkdirs();
    File source = new File(work, "Plain.java");
    Files.write(source.toPath(), Arrays.asList("public class Plain {}"), StandardCharsets.UTF_8);
    assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-d", classes.getPath(),
        source.getPath()));
    configure("auto", null, noTools()).execute();
    assertFalse(output.exists());
  }

  public void testExcludedNativeSelectionDoesNotRequireCompiler() throws Exception {
    Javah generator = configure("javac", null, noTools());
    TestJavah.set(generator, Javah.class, "excludes", new java.util.HashSet<String>(Arrays.asList("**/*.class")));
    generator.execute();
    assertFalse(output.exists());
  }

  public void testExtraClassesStillRequireCompiler() throws Exception {
    FileUtils.deleteDirectory(classes);
    classes.mkdirs();
    Javah generator = configure("auto", null, noTools());
    TestJavah.set(generator, Javah.class, "extraClasses", new java.util.HashSet<String>(Arrays.asList("Api")));
    expectFailure(generator, "Cannot find javac");
  }

  public void testDependencyOnlyExtraClassesThroughModeSelection() throws Exception {
    File dependency = new File(work, "dependency");
    Files.move(classes.toPath(), dependency.toPath());
    classes.mkdirs();
    Javah generator = configure("javac", null, toolchain(false));
    TestJavah.set(generator, Javah.class, "extraClasses", new java.util.HashSet<String>(Arrays.asList("Api")));
    TestJavah.set(generator, Javah.class, "classPaths", Arrays.asList(classes, dependency));
    generator.execute();
    assertTrue(new File(output, "Api.h").isFile());
  }

  private Toolchain noTools() {
    return new Toolchain() {
      public String getType() {
        return "jdk";
      }

      public String findTool(String tool) {
        return null;
      }
    };
  }

  public void testExplicitJavac() throws Exception {
    Javah generator = configure("javac", null, toolchain(true));
    generator.execute();
    assertTrue(new File(output, "Api.h").isFile());
    assertEquals(Arrays.asList("javac"), requested);
    // A future legacy timestamp cannot suppress the new backend.
    File stamp = new File(output, "javah");
    Files.write(stamp.toPath(), new byte[0]);
    assertTrue(stamp.setLastModified(System.currentTimeMillis() + 600000));
    Files.write(new File(output, "Api.h").toPath(), new byte[0]);
    generator.execute();
    assertTrue(new File(output, "Api.h").length() > 0);
  }

  public void testExplicitLegacyWithoutJavahFails() throws Exception {
    expectFailure(configure("javah", null, toolchain(false)), "Cannot find javah");
    assertFalse(requested.contains("javac"));
  }

  public void testSelectedToolchainDoesNotFallBackToJavaHome() throws Exception {
    Toolchain empty = new Toolchain() {
      public String getType() {
        return "jdk";
      }

      public String findTool(String tool) {
        return null;
      }
    };
    expectFailure(configure("javac", null, empty), "Cannot find javac");
  }

  public void testAutomaticSelectionFromJavaHome() throws Exception {
    configure("auto", null, null).execute();
    assertTrue(new File(output, "Api.h").isFile());
    boolean legacy = TestJavah.jdkTool("javah").isFile();
    assertEquals(legacy, new File(output, "javah").isFile());
    assertEquals(!legacy, new File(work, "target/nar/javac-headers/javac.args").isFile());
  }

  public void testAutomaticSelectionOfLegacyToolchain() throws Exception {
    if (!TestJavah.jdkTool("javah").isFile()) {
      return;
    }
    Javah generator = configure("auto", null, toolchain(true));
    generator.execute();
    assertFalse(requested.contains("javac"));
    assertTrue(new File(output, "javah").isFile());
    // Legacy mode retains its timestamp-based behavior.
    File stamp = new File(output, "javah");
    assertTrue(stamp.setLastModified(System.currentTimeMillis() + 600000));
    Files.write(new File(output, "Api.h").toPath(), new byte[0]);
    generator.execute();
    assertEquals(0L, new File(output, "Api.h").length());
  }

  public void testLegacyFailureDoesNotTriggerFallback() throws Exception {
    final String java = TestJavah.jdkTool("java").getAbsolutePath();
    Toolchain broken = new Toolchain() {
      public String getType() {
        return "jdk";
      }

      public String findTool(String tool) {
        requested.add(tool);
        return "javah".equals(tool) ? java : TestJavah.jdkTool("javac").getAbsolutePath();
      }
    };
    expectFailure(configure("auto", null, broken), "exit code");
    assertFalse(requested.contains("javac"));
    assertFalse(output.exists() && new File(output, "Api.h").exists());
  }

  public void testExplicitCustomCommandIsPreserved() throws Exception {
    // A real executable with incompatible arguments fails: no scripts or fake
    // executables.
    expectFailure(configure("auto", TestJavah.jdkTool("java").getAbsolutePath(), toolchain(false)), "exit code");
    assertTrue(requested.isEmpty());
  }

  public void testAbsoluteCustomLegacyCommand() throws Exception {
    if (!TestJavah.jdkTool("javah").isFile()) {
      return;
    }
    configure("auto", TestJavah.jdkTool("javah").getAbsolutePath(), toolchain(false)).execute();
    assertTrue(new File(output, "Api.h").isFile());
    assertTrue(new File(output, TestJavah.jdkTool("javah").getName()).isFile());
    assertTrue(requested.isEmpty());
  }

  public void testInvalidMode() throws Exception {
    expectFailure(configure("invalid", null, toolchain(false)), "Unknown javah.mode");
  }

  private Toolchain toolchain(final boolean legacy) {
    return new Toolchain() {
      public String getType() {
        return "jdk";
      }

      public String findTool(String tool) {
        requested.add(tool);
        if ("javac".equals(tool) || (legacy && "javah".equals(tool) && TestJavah.jdkTool(tool).isFile())) {
          return TestJavah.jdkTool(tool).getAbsolutePath();
        }
        return null;
      }
    };
  }

  private Javah configure(String mode, String command, final Toolchain toolchain) throws Exception {
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
    TestJavah.set(mojo, AbstractNarMojo.class, "mavenProject", project);
    TestJavah.set(mojo, AbstractNarMojo.class, "classesDirectory", classes);
    TestJavah.set(mojo, AbstractNarMojo.class, "os", System.getProperty("os.name"));
    TestJavah.set(mojo, AbstractNarMojo.class, "aolId", new AOL("amd64-Linux-gpp"));
    TestJavah.set(mojo, AbstractNarMojo.class, "javaHome", TestJavah.jdkTool("javac").getParentFile().getParentFile());
    if (toolchain != null) {
      ToolchainManager manager = (ToolchainManager) Proxy.newProxyInstance(ToolchainManager.class.getClassLoader(),
          new Class<?>[] {
              ToolchainManager.class
          }, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
              return "getToolchainFromBuildContext".equals(method.getName()) ? toolchain : null;
            }
          });
      TestJavah.set(mojo, NarJavahMojo.class, "toolchainManager", manager);
    }
    Javah generator = new Javah();
    generator.setAbstractCompileMojo(mojo);
    TestJavah.set(generator, Javah.class, "mode", mode);
    if (command != null) {
      TestJavah.set(generator, Javah.class, "name", command);
    }
    TestJavah.set(generator, Javah.class, "classDirectory", classes);
    TestJavah.set(generator, Javah.class, "jniDirectory", output);
    TestJavah.set(generator, Javah.class, "classPaths", Arrays.asList(classes));
    return generator;
  }

  private void expectFailure(Javah generator, String message) throws Exception {
    try {
      generator.execute();
      fail("Expected " + message);
    } catch (MojoExecutionException ex) {
      assertTrue(ex.getMessage(), ex.getMessage().contains(message));
    } catch (MojoFailureException ex) {
      assertTrue(ex.getMessage(), ex.getMessage().contains(message));
    }
  }
}
