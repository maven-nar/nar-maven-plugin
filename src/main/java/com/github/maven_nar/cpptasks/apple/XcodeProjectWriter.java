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
package com.github.maven_nar.cpptasks.apple;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.tools.ant.BuildException;
import org.xml.sax.SAXException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.TargetInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;
import com.github.maven_nar.cpptasks.gcc.GccCCompiler;
import com.github.maven_nar.cpptasks.ide.CommentDef;
import com.github.maven_nar.cpptasks.ide.DependencyDef;
import com.github.maven_nar.cpptasks.ide.ProjectDef;
import com.github.maven_nar.cpptasks.ide.ProjectWriter;

/**
 * Writes a Apple Xcode 2.1+ project directory. XCode stores project
 * configuration as a PropertyList. Though it will always write the project
 * as a Cocoa Old-Style ASCII property list, it will read projects
 * stored using Cocoa's XML Property List format.
 */
public final class XcodeProjectWriter implements ProjectWriter {

  /**
   * Represents a property map with an 96 bit identity.
   * When placed in a property list, this object will
   * output the string representation of the identity
   * which XCode uses to find the corresponding property
   * bag in the "objects" property of the top-level property list.
   */
  private static final class PBXObjectRef {
    /**
     * Next available identifier.
     */
    private static int nextID = 0;
    /**
     * Identifier.
     */
    private final String id;
    /**
     * Properties.
     */
    private final Map properties;

    /**
     * Create reference.
     * 
     * @param props
     *          properties.
     */
    public PBXObjectRef(final Map props) {
      if (props == null) {
        throw new NullPointerException("props");
      }
      final StringBuffer buf = new StringBuffer("000000000000000000000000");
      final String idStr = Integer.toHexString(nextID++);
      buf.replace(buf.length() - idStr.length(), buf.length(), idStr);
      this.id = buf.toString();
      this.properties = props;
    }

    /**
     * Get object identifier.
     * 
     * @return object identifier.
     */
    public String getID() {
      return this.id;
    }

    /**
     * Get properties.
     * 
     * @return properties.
     */
    public Map getProperties() {
      return this.properties;
    }

    /**
     * Get object identifier.
     * 
     * @return identifier.
     */
    @Override
    public String toString() {
      return this.id;
    }
  }

  /**
   * Create PBXBuildFile.
   * 
   * @param fileRef
   *          source file.
   * @param settings
   *          build settings.
   * @return PBXBuildFile.
   */
  private static PBXObjectRef createPBXBuildFile(final PBXObjectRef fileRef, final Map settings) {
    final Map map = new HashMap();
    map.put("fileRef", fileRef);
    map.put("isa", "PBXBuildFile");
    if (settings != null) {
      map.put("settings", settings);
    }
    return new PBXObjectRef(map);
  }

  /**
   * Create a proxy for a file in a different project.
   * 
   * @param containerPortal
   *          XcodeProject containing file.
   * @param proxyType
   *          proxy type.
   * @return PBXContainerItemProxy.
   */
  private static PBXObjectRef createPBXContainerItemProxy(final PBXObjectRef containerPortal, final int proxyType,
      final String remoteInfo) {
    final Map map = new HashMap();
    map.put("isa", "PBXContainerItemProxy");
    map.put("containerPortal", containerPortal);
    map.put("proxyType", NumberFormat.getIntegerInstance(Locale.US).format(proxyType));
    map.put("remoteInfo", remoteInfo);
    return new PBXObjectRef(map);
  }

  /**
   * Create a build phase that copies files to a destination.
   * 
   * @param buildActionMask
   *          build action mask.
   * @param dstPath
   *          destination path.
   * @param dstSubfolderSpec
   *          subfolder spec.
   * @param files
   *          files.
   * @param runOnly
   *          if true, phase should only be run on deployment.
   * @return PBXCopyFileBuildPhase.
   */
  private static PBXObjectRef createPBXCopyFilesBuildPhase(final int buildActionMask, final String dstPath,
      final String dstSubfolderSpec, final List files, final boolean runOnly) {
    final Map map = new HashMap();
    map.put("isa", "PBXCopyFilesBuildPhase");
    map.put("buildActionMask", NumberFormat.getIntegerInstance(Locale.US).format(buildActionMask));
    map.put("dstPath", dstPath);
    map.put("dstSubfolderSpec", dstSubfolderSpec);
    map.put("files", files);
    map.put("runOnlyForDeploymentPostprocessing", toString(runOnly));
    return new PBXObjectRef(map);
  }

  /**
   * Create PBXFileReference.
   * 
   * @param sourceTree
   *          source tree.
   * @param baseDir
   *          base directory.
   * @param file
   *          file.
   * @return PBXFileReference object.
   */
  private static PBXObjectRef createPBXFileReference(final String sourceTree, final String baseDir, final File file) {
    final Map map = new HashMap();
    map.put("isa", "PBXFileReference");

    final String relPath = CUtil.toUnixPath(CUtil.getRelativePath(baseDir, file));
    map.put("path", relPath);
    map.put("name", file.getName());
    map.put("sourceTree", sourceTree);
    return new PBXObjectRef(map);
  }

  /**
   * Create PBXFrameworksBuildPhase.
   * 
   * @param buildActionMask
   *          build action mask.
   * @param files
   *          files.
   * @param runOnly
   *          if true, phase should only be run on deployment.
   * @return PBXFrameworkBuildPhase.
   */
  private static PBXObjectRef createPBXFrameworksBuildPhase(final int buildActionMask, final List files,
      final boolean runOnly) {
    final Map map = new HashMap();
    map.put("isa", "PBXFrameworksBuildPhase");
    map.put("buildActionMask", NumberFormat.getIntegerInstance(Locale.US).format(buildActionMask));
    map.put("files", files);
    map.put("runOnlyForDeploymentPostprocessing", toString(runOnly));
    return new PBXObjectRef(map);
  }

  /**
   * Create PBXGroup.
   * 
   * @param name
   *          group name.
   * @param sourceTree
   *          source tree.
   * @param children
   *          list of PBXFileReferences.
   * @return group.
   */
  private static PBXObjectRef createPBXGroup(final String name, final String sourceTree, final List children) {
    final Map map = new HashMap();
    map.put("isa", "PBXGroup");
    map.put("name", name);
    map.put("sourceTree", sourceTree);
    map.put("children", children);
    return new PBXObjectRef(map);
  }

  /**
   * Create PBXNativeTarget.
   * 
   * @param name
   *          name.
   * @param buildConfigurationList
   *          build configuration list.
   * @param buildPhases
   *          build phases.
   * @param buildRules
   *          build rules.
   * @param dependencies
   *          dependencies.
   * @param productInstallPath
   *          product install path.
   * @param productName
   *          product name.
   * @param productReference
   *          file reference for product.
   * @param productType
   *          product type.
   * @return native target.
   */
  private static PBXObjectRef createPBXNativeTarget(final String name, final PBXObjectRef buildConfigurationList,
      final List buildPhases, final List buildRules, final List dependencies, final String productInstallPath,
      final String productName, final PBXObjectRef productReference, final String productType) {
    final Map map = new HashMap();
    map.put("isa", "PBXNativeTarget");
    map.put("buildConfigurationList", buildConfigurationList);
    map.put("buildPhases", buildPhases);
    map.put("buildRules", buildRules);
    map.put("dependencies", dependencies);
    map.put("name", name);
    map.put("productInstallPath", productInstallPath);
    map.put("productName", productName);
    map.put("productReference", productReference);
    map.put("productType", productType);
    return new PBXObjectRef(map);
  }

  /**
   * Create PBXProject.
   * 
   * @param buildConfigurationList
   *          build configuration list.
   * @param mainGroup
   *          main group.
   * @param projectDirPath
   *          project directory path.
   * @param targets
   *          targets.
   * @param projectRoot
   *          projectRoot directory relative to
   * @return project.
   */
  private static PBXObjectRef createPBXProject(final PBXObjectRef buildConfigurationList, final PBXObjectRef mainGroup,
      final String projectDirPath, final String projectRoot, final List targets) {
    final Map map = new HashMap();
    map.put("isa", "PBXProject");
    map.put("buildConfigurationList", buildConfigurationList.getID());
    map.put("hasScannedForEncodings", "0");
    map.put("mainGroup", mainGroup.getID());
    map.put("projectDirPath", projectDirPath);
    map.put("targets", targets);
    map.put("projectRoot", projectRoot);
    return new PBXObjectRef(map);
  }

  /**
   * Create a proxy for a file in a different project.
   * 
   * @param remoteRef
   *          PBXContainerItemProxy for reference.
   * @param dependency
   *          dependency.
   * @return PBXContainerItemProxy.
   */
  private static PBXObjectRef createPBXReferenceProxy(final PBXObjectRef remoteRef, final DependencyDef dependency) {
    final Map map = new HashMap();
    map.put("isa", "PBXReferenceProxy");
    final String fileType = "compiled.mach-o.dylib";
    map.put("fileType", fileType);
    map.put("remoteRef", remoteRef);
    map.put("path", dependency.getFile().getName() + ".dylib");
    map.put("sourceTree", "BUILT_PRODUCTS_DIR");
    return new PBXObjectRef(map);
  }

  /**
   * Create PBXSourcesBuildPhase.
   * 
   * @param buildActionMask
   *          build action mask.
   * @param files
   *          source files.
   * @param runOnly
   *          if true, phase should only be run on deployment.
   * @return PBXSourcesBuildPhase.
   */
  private static PBXObjectRef createPBXSourcesBuildPhase(final int buildActionMask, final List files,
      final boolean runOnly) {
    final Map map = new HashMap();
    map.put("buildActionMask", String.valueOf(buildActionMask));
    map.put("files", files);
    map.put("isa", "PBXSourcesBuildPhase");
    map.put("runOnlyForDeploymentPostprocessing", toString(runOnly));
    return new PBXObjectRef(map);
  }

  /**
   * Create XCBuildConfiguration.
   * 
   * @param name
   *          name.
   * @param buildSettings
   *          build settings.
   * @return build configuration.
   */
  private static PBXObjectRef createXCBuildConfiguration(final String name, final Map<String, ?> buildSettings) {
    final Map map = new HashMap();
    map.put("isa", "XCBuildConfiguration");
    map.put("buildSettings", buildSettings);
    map.put("name", name);
    return new PBXObjectRef(map);
  }

  /**
   * Create XCConfigurationList.
   * 
   * @param buildConfigurations
   *          build configurations.
   * @return configuration list.
   */
  private static PBXObjectRef createXCConfigurationList(final List buildConfigurations) {
    final Map map = new HashMap();
    map.put("isa", "XCConfigurationList");
    map.put("buildConfigurations", buildConfigurations);
    return new PBXObjectRef(map);
  }

  /**
   * Method returns "1" for true, "0" for false.
   * 
   * @param b
   *          boolean value.
   * @return "1" for true, "0" for false.
   */
  private static String toString(final boolean b) {
    if (b) {
      return "1";
    } else {
      return "0";
    }
  }

  /**
   * Constructor.
   */
  public XcodeProjectWriter() {
  }

  /**
   * Adds a dependency to the object graph.
   * 
   * @param objects
   * @param project
   * @param mainGroupChildren
   * @param baseDir
   * @param dependency
   * @return PBXBuildFile to add to PBXFrameworksBuildPhase.
   */
  private PBXObjectRef addDependency(final Map objects, final PBXObjectRef project, final List mainGroupChildren,
      final String baseDir, final DependencyDef dependency) {
    if (dependency.getFile() != null) {
      final File xcodeDir = new File(dependency.getFile().getAbsolutePath() + ".xcodeproj");
      if (xcodeDir.exists()) {
        final PBXObjectRef xcodePrj = createPBXFileReference("SOURCE_ROOT", baseDir, xcodeDir);
        mainGroupChildren.add(xcodePrj);
        objects.put(xcodePrj.getID(), xcodePrj.getProperties());

        final int proxyType = 2;
        final PBXObjectRef proxy = createPBXContainerItemProxy(xcodePrj, proxyType, dependency.getName());
        objects.put(proxy.getID(), proxy.getProperties());

        final PBXObjectRef referenceProxy = createPBXReferenceProxy(proxy, dependency);
        objects.put(referenceProxy.getID(), referenceProxy.getProperties());

        final PBXObjectRef buildFile = createPBXBuildFile(referenceProxy, Collections.emptyMap());
        objects.put(buildFile.getID(), buildFile.getProperties());

        final List productsChildren = new ArrayList();
        productsChildren.add(referenceProxy);
        final PBXObjectRef products = createPBXGroup("Products", "<group>", productsChildren);
        objects.put(products.getID(), products.getProperties());

        final Map projectReference = new HashMap();
        projectReference.put("ProductGroup", products);
        projectReference.put("ProjectRef", xcodePrj);

        List projectReferences = (List) project.getProperties().get("ProjectReferences");
        if (projectReferences == null) {
          projectReferences = new ArrayList();
          project.getProperties().put("ProjectReferences", projectReferences);
        }
        projectReferences.add(projectReference);
        return buildFile;
      }
    }
    return null;
  }

  /**
   * Add documentation group to map of objects.
   * 
   * @param objects
   *          object map.
   * @param sourceTree
   *          source tree description.
   * @return documentation group.
   */
  private PBXObjectRef addDocumentationGroup(final Map objects, final String sourceTree) {
    final List productsList = new ArrayList();
    final PBXObjectRef products = createPBXGroup("Documentation", sourceTree, productsList);
    objects.put(products.getID(), products.getProperties());
    return products;
  }

  /**
   * Add native target to map of objects.
   * 
   * @param objects
   *          map of objects.
   * @param linkTarget
   *          description of executable or shared library.
   * @param product
   *          product.
   * @param projectName
   *          project name.
   * @param sourceGroupChildren
   *          source files needed to build product.
   * @return native target.
   */
  private PBXObjectRef addNativeTarget(final Map objects, final TargetInfo linkTarget, final PBXObjectRef product,
      final String projectName, final List<PBXObjectRef> sourceGroupChildren,
      final List<PBXObjectRef> frameworkBuildFiles) {

    final PBXObjectRef buildConfigurations = addNativeTargetConfigurationList(objects, projectName);

    int buildActionMask = 2147483647;
    final List<PBXObjectRef> buildPhases = new ArrayList<>();

    final Map settings = new HashMap();
    settings.put("ATTRIBUTES", new ArrayList());
    final List buildFiles = new ArrayList();
    for (final PBXObjectRef sourceFile : sourceGroupChildren) {
      final PBXObjectRef buildFile = createPBXBuildFile(sourceFile, settings);
      buildFiles.add(buildFile);
      objects.put(buildFile.getID(), buildFile.getProperties());
    }

    final PBXObjectRef sourcesBuildPhase = createPBXSourcesBuildPhase(buildActionMask, buildFiles, false);
    objects.put(sourcesBuildPhase.getID(), sourcesBuildPhase.getProperties());
    buildPhases.add(sourcesBuildPhase);

    buildActionMask = 8;
    final PBXObjectRef frameworksBuildPhase = createPBXFrameworksBuildPhase(buildActionMask, frameworkBuildFiles, false);
    objects.put(frameworksBuildPhase.getID(), frameworksBuildPhase.getProperties());
    buildPhases.add(frameworksBuildPhase);

    final PBXObjectRef copyFilesBuildPhase = createPBXCopyFilesBuildPhase(8, "/usr/share/man/man1", "0",
        new ArrayList(), true);
    objects.put(copyFilesBuildPhase.getID(), copyFilesBuildPhase.getProperties());
    buildPhases.add(copyFilesBuildPhase);

    final List buildRules = new ArrayList();

    final List dependencies = new ArrayList();

    final String productInstallPath = "$(HOME)/bin";

    final String productType = getProductType(linkTarget);

    final PBXObjectRef nativeTarget = createPBXNativeTarget(projectName, buildConfigurations, buildPhases, buildRules,
        dependencies, productInstallPath, projectName, product, productType);
    objects.put(nativeTarget.getID(), nativeTarget.getProperties());

    return nativeTarget;
  }

  /**
   * Add native target configuration list.
   * 
   * @param objects
   *          map of objects.
   * @param projectName
   *          project name.
   * @return build configurations for native target.
   */
  private PBXObjectRef addNativeTargetConfigurationList(final Map objects, final String projectName) {

    //
    // Create a configuration list with
    // two stock configurations: Debug and Release
    //
    final List<PBXObjectRef> configurations = new ArrayList<>();
    final Map debugSettings = new HashMap();
    debugSettings.put("COPY_PHASE_STRIP", "NO");
    debugSettings.put("GCC_DYNAMIC_NO_PIC", "NO");
    debugSettings.put("GCC_ENABLE_FIX_AND_CONTINUE", "YES");
    debugSettings.put("GCC_MODEL_TUNING", "G5");
    debugSettings.put("GCC_OPTIMIZATION_LEVEL", "0");
    debugSettings.put("INSTALL_PATH", "$(HOME)/bin");
    debugSettings.put("PRODUCT_NAME", projectName);
    debugSettings.put("ZERO_LINK", "YES");
    final PBXObjectRef debugConfig = createXCBuildConfiguration("Debug", debugSettings);
    objects.put(debugConfig.getID(), debugConfig.getProperties());
    configurations.add(debugConfig);

    final Map<String, Object> releaseSettings = new HashMap<>();
    final List<String> archs = new ArrayList<>();
    archs.add("ppc");
    archs.add("i386");
    releaseSettings.put("ARCHS", archs);
    releaseSettings.put("GCC_GENERATE_DEBUGGING_SYMBOLS", "NO");
    releaseSettings.put("GCC_MODEL_TUNING", "G5");
    releaseSettings.put("INSTALL_PATH", "$(HOME)/bin");
    releaseSettings.put("PRODUCT_NAME", projectName);
    final PBXObjectRef releaseConfig = createXCBuildConfiguration("Release", releaseSettings);
    objects.put(releaseConfig.getID(), releaseConfig.getProperties());
    configurations.add(releaseConfig);

    final PBXObjectRef configurationList = createXCConfigurationList(configurations);
    objects.put(configurationList.getID(), configurationList.getProperties());
    return configurationList;
  }

  /**
   * Add file reference of product to map of objects.
   * 
   * @param objects
   *          object map.
   * @param linkTarget
   *          build description for executable or shared library.
   * @return file reference to generated executable or shared library.
   */
  private PBXObjectRef addProduct(final Map objects, final TargetInfo linkTarget) {

    //
    // create file reference for executable file
    // forget Ant's location, just place in XCode's default location
    final PBXObjectRef executable = createPBXFileReference("BUILD_PRODUCTS_DIR", linkTarget.getOutput().getParent(),
        linkTarget.getOutput());
    final Map executableProperties = executable.getProperties();

    final String fileType = getFileType(linkTarget);
    executableProperties.put("explicitFileType", fileType);
    executableProperties.put("includeInIndex", "0");
    objects.put(executable.getID(), executableProperties);

    return executable;
  }

  /**
   * Add project configuration list.
   * 
   * @param objects
   *          map of objects.
   * @param baseDir
   *          base directory.
   * @param compilerConfig
   *          compiler configuration.
   * @return project configuration object.
   */
  private PBXObjectRef addProjectConfigurationList(final Map objects, final String baseDir,
      final List<DependencyDef> dependencies, final CommandLineCompilerConfiguration compilerConfig,
      final CommandLineLinkerConfiguration linkerConfig) {
    //
    // Create a configuration list with
    // two stock configurations: Debug and Release
    //
    final List configurations = new ArrayList();
    final Map<String, Object> debugSettings = new HashMap<>();
    debugSettings.put("GCC_WARN_ABOUT_RETURN_TYPE", "YES");
    debugSettings.put("GCC_WARN_UNUSED_VARIABLE", "YES");
    debugSettings.put("PREBINDING", "NO");
    debugSettings.put("SDKROOT", "/Developer/SDKs/MacOSX10.4u.sdk");

    final PBXObjectRef debugConfig = createXCBuildConfiguration("Debug", debugSettings);
    objects.put(debugConfig.getID(), debugConfig.getProperties());
    configurations.add(debugConfig);

    final Map<String, Object> releaseSettings = new HashMap<>();
    releaseSettings.put("GCC_WARN_ABOUT_RETURN_TYPE", "YES");
    releaseSettings.put("GCC_WARN_UNUSED_VARIABLE", "YES");
    releaseSettings.put("PREBINDING", "NO");
    releaseSettings.put("SDKROOT", "/Developer/SDKs/MacOSX10.4u.sdk");
    final PBXObjectRef releaseConfig = createXCBuildConfiguration("Release", releaseSettings);
    objects.put(releaseConfig.getID(), releaseConfig.getProperties());
    configurations.add(releaseConfig);
    final PBXObjectRef configurationList = createXCConfigurationList(configurations);
    final Map projectConfigurationListProperties = configurationList.getProperties();
    projectConfigurationListProperties.put("defaultConfigurationIsVisible", "0");
    projectConfigurationListProperties.put("defaultConfigurationName", "Debug");
    objects.put(configurationList.getID(), configurationList.getProperties());

    //
    // add include paths to both configurations
    //
    final File[] includeDirs = compilerConfig.getIncludePath();
    if (includeDirs.length > 0) {
      final List<String> includePaths = new ArrayList<>();
      final Map<String, String> includePathMap = new HashMap<>();
      for (final File includeDir : includeDirs) {
        if (!CUtil.isSystemPath(includeDir)) {
          final String absPath = includeDir.getAbsolutePath();
          if (!includePathMap.containsKey(absPath)) {
            if (absPath.startsWith("/usr/")) {
              includePaths.add(CUtil.toUnixPath(absPath));
            } else {
              final String relPath = CUtil.toUnixPath(CUtil.getRelativePath(baseDir, includeDir));
              includePaths.add(relPath);
            }
            includePathMap.put(absPath, absPath);
          }
        }
      }
      includePaths.add("${inherited)");
      debugSettings.put("HEADER_SEARCH_PATHS", includePaths);
      releaseSettings.put("HEADER_SEARCH_PATHS", includePaths);
    }

    //
    // add preprocessor definitions to both configurations
    //
    //
    final String[] preArgs = compilerConfig.getPreArguments();
    final List<String> defines = new ArrayList<>();
    for (final String preArg : preArgs) {
      if (preArg.startsWith("-D")) {
        defines.add(preArg.substring(2));
      }
    }
    if (defines.size() > 0) {
      defines.add("$(inherited)");
      debugSettings.put("GCC_PREPROCESSOR_DEFINITIONS", defines);
      releaseSettings.put("GCC_PREPROCESSOR_DEFINITIONS", defines);
    }

    if (linkerConfig != null) {
      final Map<String, String> librarySearchMap = new HashMap<>();
      final List<String> librarySearchPaths = new ArrayList<>();
      final List<String> otherLdFlags = new ArrayList<>();
      final String[] linkerArgs = linkerConfig.getEndArguments();
      for (final String linkerArg : linkerArgs) {
        if (linkerArg.startsWith("-L")) {
          final String libDir = linkerArg.substring(2);
          if (!librarySearchMap.containsKey(libDir)) {
            if (!libDir.equals("/usr/lib")) {
              librarySearchPaths.add(CUtil.toUnixPath(CUtil.getRelativePath(baseDir, new File(libDir))));
            }
            librarySearchMap.put(libDir, libDir);

          }
        } else if (linkerArg.startsWith("-l")) {
          //
          // check if library is in dependencies list
          //
          final String libName = linkerArg.substring(2);
          boolean found = false;
          for (final DependencyDef dependency : dependencies) {
            if (libName.startsWith(dependency.getName())) {
              final File dependencyFile = dependency.getFile();
              if (dependencyFile != null && new File(dependencyFile.getAbsolutePath() + ".xcodeproj").exists()) {
                found = true;
                break;
              }
            }
          }
          if (!found) {
            otherLdFlags.add(linkerArg);
          }
        }
      }

      debugSettings.put("LIBRARY_SEARCH_PATHS", librarySearchPaths);
      debugSettings.put("OTHER_LDFLAGS", otherLdFlags);
      releaseSettings.put("LIBRARY_SEARCH_PATHS", librarySearchPaths);
      releaseSettings.put("OTHER_LDFLAGS", otherLdFlags);
    }
    return configurationList;
  }

  /**
   * Add file references for all source files to map of objects.
   * 
   * @param objects
   *          map of objects.
   * @param sourceTree
   *          source tree.
   * @param basePath
   *          parent of XCode project dir
   * @param targets
   *          build targets.
   * @return list containing file references of source files.
   */
  private List<PBXObjectRef> addSources(final Map objects, final String sourceTree, final String basePath,
      final Map<String, TargetInfo> targets) {
    final List<PBXObjectRef> sourceGroupChildren = new ArrayList<>();

    final List<File> sourceList = new ArrayList<>(targets.size());
    for (final TargetInfo info : targets.values()) {
      final File[] targetsources = info.getSources();
      Collections.addAll(sourceList, targetsources);
    }
    final File[] sortedSources = sourceList.toArray(new File[sourceList.size()]);
    Arrays.sort(sortedSources, new Comparator<File>() {
      @Override
      public int compare(final File o1, final File o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    for (final File sortedSource : sortedSources) {
      final PBXObjectRef fileRef = createPBXFileReference(sourceTree, basePath, sortedSource);
      sourceGroupChildren.add(fileRef);
      objects.put(fileRef.getID(), fileRef.getProperties());
    }

    return sourceGroupChildren;
  }

  /**
   * Gets the first recognized compiler from the
   * compilation targets.
   *
   * @param targets
   *          compilation targets
   * @return representative (hopefully) compiler configuration
   */
  private CommandLineCompilerConfiguration getBaseCompilerConfiguration(final Map targets) {
    //
    // find first target with an GNU C++ compilation
    //
    CommandLineCompilerConfiguration compilerConfig;
    //
    // get the first target and assume that it is representative
    //
    for (final Object o : targets.values()) {
      final TargetInfo targetInfo = (TargetInfo) o;
      final ProcessorConfiguration config = targetInfo.getConfiguration();
      //
      // for the first cl compiler
      //
      if (config instanceof CommandLineCompilerConfiguration) {
        compilerConfig = (CommandLineCompilerConfiguration) config;
        if (compilerConfig.getCompiler() instanceof GccCCompiler) {
          return compilerConfig;
        }
      }
    }
    return null;
  }

  private String getFileType(final TargetInfo linkTarget) {
    switch (getProductTypeIndex(linkTarget)) {
      case 1:
        return "archive.ar";
      case 2:
        return "compiled.mach-o.dylib";
      default:
        return "compiled.mach-o.executable";
    }
  }

  private String getProductType(final TargetInfo linkTarget) {
    switch (getProductTypeIndex(linkTarget)) {
      case 1:
        return "com.apple.product-type.library.static";
      case 2:
        return "com.apple.product-type.library.dynamic";
      default:
        return "com.apple.product-type.tool";
    }
  }

  private int getProductTypeIndex(final TargetInfo linkTarget) {
    final String outPath = linkTarget.getOutput().getPath();
    String outExtension = null;
    final int lastDot = outPath.lastIndexOf('.');
    if (lastDot != -1) {
      outExtension = outPath.substring(lastDot);
    }
    if (".a".equalsIgnoreCase(outExtension) || ".lib".equalsIgnoreCase(outExtension)) {
      return 1;
    } else if (".dylib".equalsIgnoreCase(outExtension) || ".so".equalsIgnoreCase(outExtension)
        || ".dll".equalsIgnoreCase(outExtension)) {
      return 2;
    }
    return 0;
  }

  /**
   * Writes a project definition file.
   *
   * @param fileName
   *          File name base, writer may append appropriate extension
   * @param task
   *          cc task for which to write project
   * @param projectDef
   *          project element
   * @param targets
   *          compilation targets
   * @param linkTarget
   *          link target
   * @throws IOException
   *           if error writing project file
   */
  @Override
  public void writeProject(final File fileName, final CCTask task, final ProjectDef projectDef,
      final List<File> sources, final Map<String, TargetInfo> targets, final TargetInfo linkTarget) throws IOException {

    final File xcodeDir = new File(fileName + ".xcodeproj");
    if (!projectDef.getOverwrite() && xcodeDir.exists()) {
      throw new BuildException("Not allowed to overwrite project file " + xcodeDir.toString());
    }

    final CommandLineCompilerConfiguration compilerConfig = getBaseCompilerConfiguration(targets);
    if (compilerConfig == null) {
      throw new BuildException("Unable to find compilation target using GNU C++ compiler");
    }

    CommandLineLinkerConfiguration linkerConfig = null;
    if (linkTarget.getConfiguration() instanceof CommandLineLinkerConfiguration) {
      linkerConfig = (CommandLineLinkerConfiguration) linkTarget.getConfiguration();
    }

    String projectName = projectDef.getName();
    if (projectName == null) {
      projectName = fileName.getName();
    }
    final String basePath = fileName.getAbsoluteFile().getParent();

    xcodeDir.mkdir();

    final File xcodeProj = new File(xcodeDir, "project.pbxproj");

    //
    // create property list
    //
    final Map<String, Object> propertyList = new HashMap<>();
    propertyList.put("archiveVersion", "1");
    propertyList.put("classes", new HashMap());
    propertyList.put("objectVersion", "42");
    final Map objects = new HashMap();

    final String sourceTree = "<source>";

    //
    // add source files and source group to property list
    //
    final List<PBXObjectRef> sourceGroupChildren = addSources(objects, "SOURCE_ROOT", basePath, targets);
    final PBXObjectRef sourceGroup = createPBXGroup("Source", sourceTree, sourceGroupChildren);
    objects.put(sourceGroup.getID(), sourceGroup.getProperties());

    //
    // add product to property list
    //
    final PBXObjectRef product = addProduct(objects, linkTarget);
    final List<PBXObjectRef> productsList = new ArrayList<>();
    productsList.add(product);
    final PBXObjectRef productsGroup = createPBXGroup("Products", sourceTree, productsList);
    objects.put(productsGroup.getID(), productsGroup.getProperties());

    //
    // add documentation group to property list
    //
    final PBXObjectRef documentationGroup = addDocumentationGroup(objects, sourceTree);

    //
    // add main group containing source, products and documentation group
    //
    final List<PBXObjectRef> groups = new ArrayList<>(3);
    groups.add(sourceGroup);
    groups.add(documentationGroup);
    groups.add(productsGroup);
    final PBXObjectRef mainGroup = createPBXGroup(projectName, sourceTree, groups);
    final StringBuffer comments = new StringBuffer();
    for (final CommentDef commentDef : projectDef.getComments()) {
      comments.append(commentDef);
    }
    if (comments.length() > 0) {
      mainGroup.getProperties().put("comments", comments.toString());
    }
    objects.put(mainGroup.getID(), mainGroup.getProperties());

    //
    // add project configurations
    //
    final PBXObjectRef compilerConfigurations = addProjectConfigurationList(objects, basePath,
        projectDef.getDependencies(), compilerConfig, linkerConfig);

    final String projectDirPath = "";
    final List<PBXObjectRef> projectTargets = new ArrayList<>();

    //
    // add project to property list
    //
    //
    // Calculate path (typically several ../..) of the root directory
    // (where build.xml lives) relative to the XCode project directory.
    // XCode 3.0 will now prompt user to supply the value if not specified.
    final String projectRoot = CUtil.toUnixPath(CUtil.getRelativePath(basePath, projectDef.getProject().getBaseDir()));
    final PBXObjectRef project = createPBXProject(compilerConfigurations, mainGroup, projectDirPath, projectRoot,
        projectTargets);
    objects.put(project.getID(), project.getProperties());

    final List<PBXObjectRef> frameworkBuildFiles = new ArrayList<>();
    for (final DependencyDef dependency : projectDef.getDependencies()) {
      final PBXObjectRef buildFile = addDependency(objects, project, groups, basePath, dependency);
      if (buildFile != null) {
        frameworkBuildFiles.add(buildFile);
      }
    }
    //
    // add description of native target (that is the executable or
    // shared library)
    //
    final PBXObjectRef nativeTarget = addNativeTarget(objects, linkTarget, product, projectName, sourceGroupChildren,
        frameworkBuildFiles);
    projectTargets.add(nativeTarget);

    //
    // finish up overall property list
    //
    propertyList.put("objects", objects);
    propertyList.put("rootObject", project.getID());

    //
    // write property list out to XML file
    //
    try {
      PropertyListSerialization.serialize(propertyList, projectDef.getComments(), xcodeProj);
    } catch (final TransformerConfigurationException ex) {
      throw new IOException(ex.toString());
    } catch (final SAXException ex) {
      if (ex.getException() instanceof IOException) {
        throw (IOException) ex.getException();
      }
      throw new IOException(ex.toString());
    }
  }

}
