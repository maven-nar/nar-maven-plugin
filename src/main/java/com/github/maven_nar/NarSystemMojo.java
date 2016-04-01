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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Generates a NarSystem class with static methods to use inside the java part
 * of the library. Runs in generate-resources rather than generate-sources to
 * allow the maven-swig-plugin (which runs in generate-sources) to configure the
 * nar plugin and to let it generate a proper system file.
 *
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-system-generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true)
public class NarSystemMojo extends AbstractNarMojo {

  /** @component */
  private BuildContext buildContext;

  private String generateExtraMethods() throws MojoFailureException {
    final StringBuilder builder = new StringBuilder();

    builder.append("\n    private static String[] getAOLs() {\n");
    builder
        .append("        final String ao = System.getProperty(\"os.arch\") + \"-\" + System.getProperty(\"os.name\").replaceAll(\" \", \"\");\n");

    // build map: AO -> AOLs
    final Map<String, List<String>> aoMap = new LinkedHashMap<>();
    for (final String aol : NarProperties.getInstance(getMavenProject()).getKnownAOLs()) {
      final int dash = aol.lastIndexOf('-');
      final String ao = aol.substring(0, dash);
      List<String> list = aoMap.get(ao);
      if (list == null) {
        aoMap.put(ao, list = new ArrayList<>());
      }
      list.add(aol);
    }

    builder.append("\n        // choose the list of known AOLs for the current platform\n");
    String delimiter = "        ";
    for (final Map.Entry<String, List<String>> entry : aoMap.entrySet()) {
      builder.append(delimiter);
      delimiter = " else ";

      builder.append("if (ao.startsWith(\"").append(entry.getKey()).append("\")) {\n");
      builder.append("            return new String[] {\n");
      String delimiter2 = "              ";
      for (final String aol : entry.getValue()) {
        builder.append(delimiter2).append("\"").append(aol).append("\"");
        delimiter2 = ", ";
      }
      builder.append("\n            };");
      builder.append("\n        }");
    }
    builder.append(" else {\n");
    builder.append("            throw new RuntimeException(\"Unhandled architecture/OS: \" + ao);\n");
    builder.append("        }\n");
    builder.append("    }\n");
    builder.append("\n");
    builder.append("    private static String[] getMappedLibraryNames(String fileName) {\n");
    builder.append("        String mapped = System.mapLibraryName(fileName);\n");
    builder
    .append("        final String ao = System.getProperty(\"os.arch\") + \"-\" + System.getProperty(\"os.name\").replaceAll(\" \", \"\");\n");
    builder.append("    	if (ao.startsWith(\"x86_64-MacOSX\")){\n");
    builder.append("    		// .jnilib or .dylib depends on JDK version\n");
    builder.append("    		mapped = mapped.substring(0, mapped.lastIndexOf('.'));\n");
    builder.append("    		return new String[]{mapped+\".dylib\", mapped+\".jnilib\"};\n");
    builder.append("    	}\n");
    builder.append("    	return new String[]{mapped};\n");
    builder.append("    }\n");
    builder.append("\n");
    builder
        .append("    private static File getUnpackedLibPath(final ClassLoader loader, final String[] aols, final String fileName, final String[] mappedNames) {\n");
    builder.append("        final String classPath = NarSystem.class.getName().replace('.', '/') + \".class\";\n");
    builder.append("        final URL url = loader.getResource(classPath);\n");
    builder.append("        if (url == null || !\"file\".equals(url.getProtocol())) return null;\n");
    builder.append("        final String path = url.getPath();\n");
    builder
        .append("        final String prefix = path.substring(0, path.length() - classPath.length()) + \"../nar/\" + fileName + \"-\";\n");
    builder.append("        for (final String aol : aols) {\n");
    builder.append("            for(final String mapped : mappedNames) {\n");
    builder
        .append("                final File file = new File(prefix + aol + \"-jni/lib/\" + aol + \"/jni/\" + mapped);\n");
    builder.append("                if (file.isFile()) return file;\n");
    builder.append("                final File fileShared = new File(prefix + aol + \"-shared/lib/\" + aol + \"/shared/\" + mapped);\n");
    builder.append("                if (fileShared.isFile()) return fileShared;\n");
    builder.append("            }\n");
    builder.append("        }\n");
    builder.append("        return null;\n");
    builder.append("    }\n");
    builder.append("\n");
    builder
        .append("    private static String getLibPath(final ClassLoader loader, final String[] aols, final String[] mappedNames) {\n");
    builder.append("        for (final String aol : aols) {\n");
    builder.append("            final String libPath = \"lib/\" + aol + \"/jni/\";\n");
    builder.append("            final String libPathShared = \"lib/\" + aol + \"/shared/\";\n");
    builder.append("            for(final String mapped : mappedNames) {\n");
    builder.append("                if (loader.getResource(libPath + mapped) != null) return libPath;\n");
    builder.append("                if (loader.getResource(libPathShared + mapped) != null) return libPathShared;\n");
    builder.append("            }\n");
    builder.append("        }\n");
    builder.append("        throw new RuntimeException(\"Library '\" + mappedNames[0] + \"' not found!\");\n");
    builder.append("    }\n");

    return builder.toString();
  }

  private boolean hasNativeLibLoaderAsDependency() {
    for (MavenProject project = getMavenProject(); project != null; project = project.getParent()) {
      final List<Dependency> dependencies = project.getDependencies();
      for (final Dependency dependency : dependencies) {
        final String artifactId = dependency.getArtifactId();
        if ("native-lib-loader".equals(artifactId)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public final void narExecute() throws MojoExecutionException, MojoFailureException {
    // get packageName if specified for JNI.
    String packageName = null;
    String narSystemName = null;
    File narSystemDirectory = null;
    boolean jniFound = false;
    for (final Library library : getLibraries()) {
      if (library.getType().equals(Library.JNI) || library.getType().equals(Library.SHARED)) {
        packageName = library.getNarSystemPackage();
        narSystemName = library.getNarSystemName();
        narSystemDirectory = new File(getTargetDirectory(), library.getNarSystemDirectory());
        jniFound = true;
      }
    }

    if (!jniFound || packageName == null) {
      if (!jniFound) {
        getLog().debug("NAR: not building a shared or JNI library, so not generating NarSystem class.");
      } else {
        getLog().warn("NAR: no system package specified; unable to generate NarSystem class.");
      }
      return;
    }

    // make sure destination is there
    narSystemDirectory.mkdirs();

    getMavenProject().addCompileSourceRoot(narSystemDirectory.getPath());

    final File fullDir = new File(narSystemDirectory, packageName.replace('.', '/'));
    fullDir.mkdirs();

    final File narSystem = new File(fullDir, narSystemName + ".java");
    getLog().info("Generating " + narSystem);
    // initialize string variable to be used in NarSystem.java
    final String importString, loadLibraryString, extraMethods, output = getOutput(true);
    if (hasNativeLibLoaderAsDependency()) {
      getLog().info("Using 'native-lib-loader'");
      importString = "import java.io.File;\n" + "import java.net.URL;\n"
          + "import org.scijava.nativelib.DefaultJniExtractor;\n" + "import org.scijava.nativelib.JniExtractor;\n";
      loadLibraryString = "final String fileName = \""
          + output
          + "\";\n"
          + "        //first try if the library is on the configured library path\n"
          + "        try {\n"
          + "            System.loadLibrary(\""
          + output
          + "\");\n"
          + "            return;\n"
          + "        }\n"
          + "        catch (Exception e) {\n"
          + "        }\n"
          + "        catch (UnsatisfiedLinkError e) {\n"
          + "        }\n"
          + "        final String[] mappedNames = getMappedLibraryNames(fileName);\n"
          + "        final String[] aols = getAOLs();\n"
          + "        final ClassLoader loader = NarSystem.class.getClassLoader();\n"
          + "        final File unpacked = getUnpackedLibPath(loader, aols, fileName, mappedNames);\n"
          + "        if (unpacked != null) {\n"
          + "            System.load(unpacked.getPath());\n"
          + "        } else try {\n"
          + "            final String libPath = getLibPath(loader, aols, mappedNames);\n"
          + "            final JniExtractor extractor = new DefaultJniExtractor(NarSystem.class, System.getProperty(\"java.io.tmpdir\"));\n"
          + "            final File extracted = extractor.extractJni(libPath, fileName);\n"
          + "            System.load(extracted.getPath());\n" + "        } catch (final Exception e) {\n"
          + "            e.printStackTrace();\n" + "            throw e instanceof RuntimeException ?\n"
          + "                (RuntimeException) e : new RuntimeException(e);\n" + "        }";
      extraMethods = generateExtraMethods();
    } else {
      getLog().info("Not using 'native-lib-loader' because it is not a dependency)");
      importString = null;
      loadLibraryString = "System.loadLibrary(\"" + output + "\");";
      extraMethods = null;
    }

    try {
      final FileOutputStream fos = new FileOutputStream(narSystem);
      final PrintWriter p = new PrintWriter(fos);
      p.println("// DO NOT EDIT: Generated by NarSystemGenerate.");
      p.println("package " + packageName + ";");
      p.println("");
      if (importString != null) {
        p.println(importString);
      }
      p.println("/**");
      p.println(" * Generated class to load the correct version of the jni library");
      p.println(" *");
      p.println(" * @author nar-maven-plugin");
      p.println(" */");
      p.println("public final class NarSystem");
      p.println("{");
      p.println("");
      p.println("    private NarSystem() ");
      p.println("    {");
      p.println("    }");
      p.println("");
      p.println("    /**");
      p.println("     * Load jni library: " + output);
      p.println("     *");
      p.println("     * @author nar-maven-plugin");
      p.println("     */");
      p.println("    public static void loadLibrary()");
      p.println("    {");
      p.println("        " + loadLibraryString);
      p.println("    }");
      p.println("");
      p.println("    public static int runUnitTests() {");
      p.println("	       return new NarSystem().runUnitTestsNative();");
      p.println("    }");
      p.println("");
      p.println("    public native int runUnitTestsNative();");
      if (extraMethods != null) {
        p.println(extraMethods);
      }
      p.println("}");
      p.close();
      fos.close();
    } catch (final IOException e) {
      throw new MojoExecutionException("Could not write '" + narSystemName + "'", e);
    }

    if (this.buildContext != null) {
      this.buildContext.refresh(narSystem);
    }
  }
}
