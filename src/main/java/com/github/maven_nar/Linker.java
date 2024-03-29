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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.LinkerEnum;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

/**
 * Linker tag
 * 
 * @author Mark Donszelmann
 */
public class Linker {

  /**
   * The Linker Some choices are: "msvc", "g++", "CC", "icpc", ... Default is
   * Architecture-OS-Linker specific: FIXME:
   * table missing
   */
  @Parameter
  private String name;

  /**
   * The prefix for the linker.
   */
  @Parameter
  private String prefix;

  /**
   * Path location of the linker tool
   */
  @Parameter
  private String toolPath;

  /**
   * Enables or disables incremental linking.
   */
  @Parameter(required = true)
  private boolean incremental = false;

  /**
   * Enables or disables the production of a map file.
   */
  @Parameter(required = true)
  private boolean map = false;

  @Parameter(required = true)
  private boolean skipDepLink = false;
  
  /**
   * Options for the linker Defaults to Architecture-OS-Linker specific values.
   * FIXME table missing
   */
  @Parameter
  private List options;

  /**
   * Additional options for the linker when running in the nar-testCompile
   * phase.
   * 
   */
  @Parameter
  private List testOptions;

  /**
   * Options for the linker as a whitespace separated list. Defaults to
   * Architecture-OS-Linker specific values. Will
   * work in combination with &lt;options&gt;.
   */
  @Parameter
  private String optionSet;

  /**
   * Clears default options
   */
  @Parameter(required = true)
  private boolean clearDefaultOptions;

  /**
   * Adds libraries to the linker.
   */
  @Parameter
  private List/* <Lib> */ libs;

  /**
   * Adds libraries to the linker. Will work in combination with &lt;libs&gt;.
   * The format is comma separated,
   * colon-delimited values (name:type:dir), like
   * "myLib:shared:/home/me/libs/, otherLib:static:/some/path".
   */
  @Parameter
  private String libSet;

  /**
   * Adds system libraries to the linker.
   */
  @Parameter
  private List/* <SysLib> */ sysLibs;

  /**
   * Adds system libraries to the linker. Will work in combination with
   * &lt;sysLibs&gt;. The format is comma
   * separated, colon-delimited values (name:type), like
   * "dl:shared, pthread:shared".
   */
  @Parameter
  private String sysLibSet;

  /**
   * <p>
   * Specifies the link ordering of libraries that come from nar dependencies.
   * The format is a comma separated list of dependency names, given as
   * groupId:artifactId.
   * </p>
   * <p>
   * Example: &lt;narDependencyLibOrder&gt;someGroup:myProduct,
   * other.group:productB&lt;narDependencyLibOrder&gt;
   * </p>
   */
  @Parameter
  private String narDependencyLibOrder;

  /**
   * <p>
   * Specifies to use Default link ordering of libraries that come from mvn dependency tree.
   * The Default link order (generated by nar) is a Level-order tree traversing list (also called BFS) of
   * dependency tree, given as a comma separated list of groupId:artifactId.
   * </p>
   * <p>
   *  default Value is "false"
   * </p>
   */
  @Parameter(defaultValue = "false")
  private boolean narDefaultDependencyLibOrder = false;
  
  /**
   * Specifies that if using default dependency lib order then turn on/off logic that pushes
   * dependencies to appropriate place in linker line based on transitive dependencies.
   * @since 3.5.2
   */
  @Parameter(defaultValue = "false")
  protected boolean pushDepsToLowestOrder = false;

  /**
   * Specify that the linker should generate an intermediate manifest based on
   * the inputs.
   */
  @Parameter(property = "nar.generateManifest", defaultValue = "true")
  private boolean generateManifest = true;

  private final Log log;

  public Linker() {
    // default constructor for use as TAG
    this(null);
  }

  public Linker(final Log log) {
    this.log = log;
  }

  /**
   * For use with specific named linker.
   * 
   * @param name
   */
  public Linker(final String name, final Log log) {
    this.name = name;
    this.log = log;
  }

  private void addLibraries(final String libraryList, final LinkerDef linker, final Project antProject,
      final boolean isSystem) {

    if (libraryList == null) {
      return;
    }

    final String[] lib = libraryList.split(",");

    for (final String element : lib) {

      final String[] libInfo = element.trim().split(":", 3);

      LibrarySet librarySet = new LibrarySet();

      if (isSystem) {
        librarySet = new SystemLibrarySet();
      }

      librarySet.setProject(antProject);
      librarySet.setLibs(new CUtil.StringArrayBuilder(libInfo[0]));

      if (libInfo.length > 1) {

        final LibraryTypeEnum libType = new LibraryTypeEnum();

        libType.setValue(libInfo[1]);
        librarySet.setType(libType);

        if (!isSystem && libInfo.length > 2) {
          librarySet.setDir(new File(libInfo[2]));
        }
      }

      if (!isSystem) {
        linker.addLibset(librarySet);
      } else {
        linker.addSyslibset((SystemLibrarySet) librarySet);
      }
    }
  }

  /**
   *  
   **/
  public boolean isGenerateManifest() {
    return generateManifest;
  }

  public final LinkerDef getLinker(final AbstractCompileMojo mojo, final CCTask task, final String os, final String prefix,
      final String type, final List<String> linkPaths) throws MojoFailureException, MojoExecutionException {
    Project antProject = task.getProject();
    if (this.name == null) {
      throw new MojoFailureException("NAR: Please specify a <Name> as part of <Linker>");
    }

    final LinkerDef linker = new LinkerDef();
    linker.setProject(antProject);
    final LinkerEnum linkerEnum = new LinkerEnum();
    linkerEnum.setValue(this.name);
    linker.setName(linkerEnum);

    // tool path
    if (this.toolPath != null) {
      linker.setToolPath(this.toolPath);
    } else if (Msvc.isMSVC(name)) {
      linker.setToolPath(mojo.getMsvc().getToolPath());
    }

    linker.setSkipDepLink(this.skipDepLink);
    
    // incremental, map
    final String linkerPrefix;
    // don't add prefix to ar-like commands FIXME should be done in cpptasks
    if (type.equals(Library.STATIC) && !getName(null, null).equals("msvc")) {
      linkerPrefix = null;
    }
    else if (isNullOrEmpty(this.prefix)) {
      String key = mojo.getAOL().getKey() + ".linker.prefix";
      linkerPrefix = NarProperties.getInstance(mojo.getMavenProject()).getProperty(key);
    }
    else {
      linkerPrefix = this.prefix;
    }

    linker.setLinkerPrefix(linkerPrefix);
    linker.setIncremental(this.incremental);
    linker.setMap(this.map);

    // Add definitions (Window only)
    if (os.equals(OS.WINDOWS) && getName(null, null).equals("msvc")
        && (type.equals(Library.SHARED) || type.equals(Library.JNI))) {
      final Set<File> defs = new HashSet<>();
      try {
        if (mojo.getC() != null) {
          final List cSrcDirs = mojo.getC().getSourceDirectories();
          for (final Object cSrcDir : cSrcDirs) {
            final File dir = (File) cSrcDir;
            if (dir.exists()) {
              defs.addAll(FileUtils.getFiles(dir, "**/*.def", null));
            }
          }
        }
      } catch (final IOException e) {
      }
      try {
        if (mojo.getCpp() != null) {
          final List cppSrcDirs = mojo.getCpp().getSourceDirectories();
          for (final Object cppSrcDir : cppSrcDirs) {
            final File dir = (File) cppSrcDir;
            if (dir.exists()) {
              defs.addAll(FileUtils.getFiles(dir, "**/*.def", null));
            }
          }
        }
      } catch (final IOException e) {
      }
      try {
        if (mojo.getFortran() != null) {
          final List fortranSrcDirs = mojo.getFortran().getSourceDirectories();
          for (final Object fortranSrcDir : fortranSrcDirs) {
            final File dir = (File) fortranSrcDir;
            if (dir.exists()) {
              defs.addAll(FileUtils.getFiles(dir, "**/*.def", null));
            }
          }
        }
      } catch (final IOException e) {
      }

      for (final Object def : defs) {
        final LinkerArgument arg = new LinkerArgument();
        arg.setValue("/def:" + def);
        linker.addConfiguredLinkerArg(arg);
      }
    }

    // FIXME, this should be done in CPPTasks at some point, and may not be
    // necessary, but was for VS 2010 beta 2
    if (os.equals(OS.WINDOWS) && getName(null, null).equals("msvc") && !getVersion(mojo).startsWith("6.")
        && (type.equals(Library.SHARED) || type.equals(Library.JNI) || type.equals(Library.EXECUTABLE))) {
      final LinkerArgument arg = new LinkerArgument();
      if (isGenerateManifest())
        arg.setValue("/MANIFEST");
      else
        arg.setValue("/MANIFEST:NO");
      linker.addConfiguredLinkerArg(arg);

      if (isGenerateManifest()) {
        final LinkerArgument arg2 = new LinkerArgument();
        arg2.setValue("/MANIFESTFILE:" + task.getOutfile() + ".manifest");
        linker.addConfiguredLinkerArg(arg2);
      }
    }

    // Add options to linker
    if (this.options != null) {
      for (final Object option : this.options) {
        final LinkerArgument arg = new LinkerArgument();
        arg.setValue((String) option);
        linker.addConfiguredLinkerArg(arg);
      }
    }

    if (this.optionSet != null) {

      final String[] opts = this.optionSet.split("\\s");

      for (final String opt : opts) {

        final LinkerArgument arg = new LinkerArgument();

        arg.setValue(opt);
        linker.addConfiguredLinkerArg(arg);
      }
    }

    if (!this.clearDefaultOptions) {
      final String option = NarProperties.getInstance(mojo.getMavenProject()).getProperty(prefix + "options");
      if (option != null) {
        final String[] opt = option.split(" ");
        for (final String element : opt) {
          final LinkerArgument arg = new LinkerArgument();
          arg.setValue(element);
          linker.addConfiguredLinkerArg(arg);
        }
      }
    }

    //if No user preference of dependency library link order is specified then use the Default one nar generate.
    if ((this.narDependencyLibOrder == null) && (narDefaultDependencyLibOrder)) {
         if (os.equals(OS.AIX) && (getName(null, null).equals("xlC_r") || getName(null, null).equals("xlC") || getName(null, null).equals("xlc"))){
            String dependencies = new StringBuilder(mojo.dependencyTreeOrderStr(pushDepsToLowestOrder, mojo.getDirectDepsOnly())).toString();
            List<String> dependency_list = Arrays.asList(dependencies.split("\\s*,\\s*"));
            Collections.reverse(dependency_list); 
            StringBuilder libOrder = new StringBuilder();
            boolean first = true;
            for (String dependency : dependency_list) {
                if (first) first = false;
                else libOrder.append(",");
                libOrder.append(dependency);
            }
            this.narDependencyLibOrder = libOrder.toString();
         } else {
            this.narDependencyLibOrder = mojo.dependencyTreeOrderStr(pushDepsToLowestOrder, mojo.getDirectDepsOnly());
         }
    } else if (pushDepsToLowestOrder && !narDefaultDependencyLibOrder) {
        mojo.getLog().warn("pushDepsToLowestOrder will have no effect since narDefaultDependencyLibOrder is disabled");
    } else if (mojo.getDirectDepsOnly() && !narDefaultDependencyLibOrder) {
        mojo.getLog().warn("directDepsOnly will have no effect since narDefaultDependencyLibOrder is disabled");
    }

    // Add transitive dependencies to the shared library search path if directDepsOnly is enabled, this is not a static library, and the OS is either Linux or AIX.
    if (linkPaths != null && linkPaths.size() > 0 && mojo.getDirectDepsOnly() && !type.equals(Library.STATIC) && (os.equals(OS.LINUX) || os.equals(OS.AIX))){
        StringBuilder argStrBuilder = new StringBuilder();
        if (os.equals(OS.LINUX))
        {
           argStrBuilder.append("-Wl,-rpath-link,");
        }
        else if (os.equals(OS.AIX))
        {
           argStrBuilder.append("-L");
        }
        for (String path : linkPaths){
            argStrBuilder.append(path).append(':');
        }
        String argStr = argStrBuilder.toString();
        final LinkerArgument linkPathArg = new LinkerArgument ();
        // Trim trailing ':' character from argument
        linkPathArg.setValue(argStr.substring(0, argStr.length() - 1));
        linker.addConfiguredLinkerArg(linkPathArg);
    }

    // record the preference for nar dependency library link order
    if (this.narDependencyLibOrder != null) {

      final List libOrder = new LinkedList();

      final String[] lib = this.narDependencyLibOrder.split(",");

      for (final String element : lib) {
        libOrder.add(element.trim());
      }

      mojo.setDependencyLibOrder(libOrder);
    }

    // Add Libraries to linker
    if (this.libs != null || this.libSet != null) {

      if (this.libs != null) {

        for (final Object lib1 : this.libs) {

          final Lib lib = (Lib) lib1;
          lib.addLibSet(mojo, linker, antProject);
        }
      }

      if (this.libSet != null) {
        addLibraries(this.libSet, linker, antProject, false);
      }
    } else {

      final String libsList = NarProperties.getInstance(mojo.getMavenProject()).getProperty(prefix + "libs");

      addLibraries(libsList, linker, antProject, false);
    }

    // Add System Libraries to linker
    if (this.sysLibs != null || this.sysLibSet != null) {

      if (this.sysLibs != null) {

        for (final Object sysLib1 : this.sysLibs) {

          final SysLib sysLib = (SysLib) sysLib1;
          linker.addSyslibset(sysLib.getSysLibSet(antProject));
        }
      }

      if (this.sysLibSet != null) {
        addLibraries(this.sysLibSet, linker, antProject, true);
      }
    } else {

      final String sysLibsList = NarProperties.getInstance(mojo.getMavenProject()).getProperty(prefix + "sysLibs");

      addLibraries(sysLibsList, linker, antProject, true);
    }

    mojo.getMsvc().configureLinker(linker);

    return linker;
  }

  public final String getName() {
    return this.name;
  }

  public final String getName(final NarProperties properties, final String prefix)
      throws MojoFailureException, MojoExecutionException {
    if (this.name == null && properties != null && prefix != null) {
      this.name = properties.getProperty(prefix + "linker");
    }
    if (this.name == null) {
      throw new MojoExecutionException("NAR: One of two things may be wrong here:\n\n"
          + "1. <Name> tag is missing inside the <Linker> tag of your NAR configuration\n\n"
          + "2. no linker is defined in the aol.properties file for '" + prefix + "linker'\n");
    }
    return this.name;
  }

  /**
   * @return The standard Linker configuration with 'testOptions' added to the
   *         argument list.
   */
  public final LinkerDef getTestLinker(final AbstractCompileMojo mojo, final CCTask task, final String os,
      final String prefix, final String type, final List<String> linkPaths) throws MojoFailureException, MojoExecutionException {
    final LinkerDef linker = getLinker(mojo, task, os, prefix, type, linkPaths);
    if (this.testOptions != null) {
      for (final Object testOption : this.testOptions) {
        final LinkerArgument arg = new LinkerArgument();
        arg.setValue((String) testOption);
        linker.addConfiguredLinkerArg(arg);
      }
    }
    return linker;
  }

  public final String getVersion() throws MojoFailureException, MojoExecutionException {
    return getVersion(new NarCompileMojo());
  }

  public final String getVersion(final AbstractNarMojo mojo) throws MojoFailureException, MojoExecutionException {
    if (this.name == null) {
      throw new MojoFailureException("Cannot deduce linker version if name is null");
    }

    String version = null;
    final String linkerPrefix = nullToEmpty(this.prefix);

    final TextStream out = new StringTextStream();
    final TextStream err = new StringTextStream();
    final TextStream dbg = new StringTextStream();

    if (this.name.equals("g++") || this.name.equals("gcc")) {
      NarUtil.runCommand(linkerPrefix+"gcc", new String[] {
        "--version"
      }, null, null, out, err, dbg, this.log);
      final Pattern p = Pattern.compile("\\d+\\.\\d+\\.\\d+");
      final Matcher m = p.matcher(out.toString());
      if (m.find()) {
        version = m.group(0);
      }
    } else if (this.name.equals("msvc")) {
      version = mojo.getMsvc().getVersion();
    } else if (this.name.equals("icc") || this.name.equals("icpc")) {
      NarUtil.runCommand("icc", new String[] {
          "--version"
      }, null, null, out, err, dbg, this.log);
      final Pattern p = Pattern.compile("\\d+\\.\\d+");
      final Matcher m = p.matcher(out.toString());
      if (m.find()) {
        version = m.group(0);
      }
    } else if (this.name.equals("icl")) {
      NarUtil.runCommand("icl", new String[] {
          "/QV"
      }, null, null, out, err, dbg, this.log);
      final Pattern p = Pattern.compile("\\d+\\.\\d+");
      final Matcher m = p.matcher(err.toString());
      if (m.find()) {
        version = m.group(0);
      }
    } else if (this.name.equals("CC")) {
      NarUtil.runCommand("CC", new String[] {
          "-V"
      }, null, null, out, err, dbg, this.log);
      final Pattern p = Pattern.compile("\\d+\\.\\d+");
      final Matcher m = p.matcher(err.toString());
      if (m.find()) {
        version = m.group(0);
      }
    } else if (this.name.equals("xlC")) {
      NarUtil.runCommand("/usr/vacpp/bin/xlC", new String[] {
          "-qversion"
      }, null, null, out, err, dbg, this.log);
      final Pattern p = Pattern.compile("\\d+\\.\\d+");
      final Matcher m = p.matcher(out.toString());
      if (m.find()) {
        version = m.group(0);
      }
    } else if (this.name.equals("xlC_r")) {
      NarUtil.runCommand("xlC_r", new String[] {
              "-qversion"
      }, null, null, out, err, dbg, this.log);
      final Pattern p = Pattern.compile("\\d+\\.\\d+");
      final Matcher m = p.matcher(out.toString());
      if (m.find()) {
        version = m.group(0);
      }
    } else if (name.equals("clang") || name.equals("clang++")) {
      NarUtil.runCommand("clang", new String[] {
          "--version"
      }, null, null, out, err, dbg, log);
      final Pattern p = Pattern.compile("\\d+\\.\\d+\\.\\d+");
      final Matcher m = p.matcher(out.toString());
      if (m.find()) {
        version = m.group(0);
      }
    } else {
      if (!linkerPrefix.isEmpty()) {
        NarUtil.runCommand(linkerPrefix+this.name, new String[] {
          "--version"
        }, null, null, out, err, dbg, this.log);
        final Pattern p = Pattern.compile("\\d+\\.\\d+\\.\\d+");
        final Matcher m = p.matcher(out.toString());
        if (m.find()) {
          version = m.group(0);
        }
      } else {
        throw new MojoFailureException("Cannot find version number for linker '" + this.name + "'");
      }
    }

    if (version == null) {
      if (!err.toString().isEmpty())
        mojo.getLog().debug("linker returned error stream: " + err.toString());
      throw new MojoFailureException("Cannot deduce version number from: " + out.toString());
    }
    return version;
  }

  public List getSysLibs() {
    return sysLibs;
  }

  public String getSysLibSet() {
    return sysLibSet;
  }
}
