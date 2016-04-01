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
package com.github.maven_nar.cpptasks.msvc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.TargetInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;
import com.github.maven_nar.cpptasks.ide.CommentDef;
import com.github.maven_nar.cpptasks.ide.DependencyDef;
import com.github.maven_nar.cpptasks.ide.ProjectDef;
import com.github.maven_nar.cpptasks.ide.ProjectWriter;

/**
 * Writes a Microsoft Visual Studio 97 or Visual Studio 6 project file.
 *
 * Status: Collects file list but does not pick
 * up libraries and settings from project.
 *
 * @author curta
 */
public final class MsvcProjectWriter implements ProjectWriter {
  private static String toProjectName(final String name) {
    //
    // some characters are apparently not allowed in VS project names
    // but have not been able to find them documented
    // limiting characters to alphas, numerics and hyphens
    final StringBuffer projectNameBuf = new StringBuffer(name);
    for (int i = 0; i < projectNameBuf.length(); i++) {
      final char ch = projectNameBuf.charAt(i);
      if (!(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9')) {
        projectNameBuf.setCharAt(i, '_');
      }
    }
    return projectNameBuf.toString();

  }

  private static void writeComments(final Writer writer, final List<CommentDef> comments) throws IOException {
    for (final CommentDef commentDef : comments) {
      final String comment = commentDef.getText();
      if (comment != null) {
        int start = 0;
        for (int end = comment.indexOf('\n'); end != -1; end = comment.indexOf('\n', start)) {
          writer.write("#" + comment.substring(start, end) + "\r\n");
          start = end + 1;
        }
      }
    }
  }

  private static void writeWorkspaceProject(final Writer writer, final String projectName, final String projectFile,
      final List<String> dependsOn) throws IOException {
    writer.write("############################################");
    writer.write("###################################\r\n\r\n");
    String file = projectFile;
    if (!file.startsWith(".") && !file.startsWith("\\") && !file.startsWith("/")) {
      file = ".\\" + file;
    }
    writer.write("Project: \"" + projectName + "\"=\"" + file + "\" - Package Owner=<4>\r\n\r\n");

    writer.write("Package=<5>\r\n{{{\r\n}}}\r\n\r\n");
    writer.write("Package=<4>\r\n{{{\r\n");
    if (dependsOn != null) {
      for (final String string : dependsOn) {
        writer.write("    Begin Project Dependency\r\n");
        writer.write("    Project_Dep_Name " + toProjectName(String.valueOf(string)) + "\r\n");
        writer.write("    End Project Dependency\r\n");
      }
    }
    writer.write("}}}\r\n\r\n");

  }

  /**
   * Visual Studio version.
   */
  private final String version;

  /**
   * Constructor.
   * 
   * @param versionArg
   *          String Visual Studio version.
   */
  public MsvcProjectWriter(final String versionArg) {
    this.version = versionArg;
  }

  /**
   * Gets the first recognized compiler from the
   * compilation targets.
   * 
   * @param targets
   *          compilation targets
   * @return representative (hopefully) compiler configuration
   */
  private CommandLineCompilerConfiguration getBaseCompilerConfiguration(final Map<String, TargetInfo> targets) {
    //
    // find first target with an DevStudio C compilation
    //
    CommandLineCompilerConfiguration compilerConfig;
    //
    // get the first target and assume that it is representative
    //
    for (final TargetInfo targetInfo : targets.values()) {
      final ProcessorConfiguration config = targetInfo.getConfiguration();
      //
      // for the first cl compiler
      //
      if (config instanceof CommandLineCompilerConfiguration) {
        compilerConfig = (CommandLineCompilerConfiguration) config;
        if (compilerConfig.getCompiler() instanceof MsvcCCompiler) {
          return compilerConfig;
        }
      }
    }
    return null;
  }

  /**
   * Get alphabetized array of source files.
   * 
   * @param sourceList
   *          list of source files
   * @return File[] source files
   */
  private File[] getSources(final List<File> sourceList) {
    final File[] sortedSources = new File[sourceList.size()];
    sourceList.toArray(sortedSources);
    Arrays.sort(sortedSources, new Comparator<File>() {
      @Override
      public int compare(final File o1, final File o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    return sortedSources;
  }

  /**
   * Returns true if the file has an extension that appears in the group filter.
   * 
   * @param filter
   *          String group filter
   * @param candidate
   *          File file
   * @return boolean true if member of group
   */
  private boolean isGroupMember(final String filter, final File candidate) {
    final String fileName = candidate.getName();
    final int lastDot = fileName.lastIndexOf('.');
    if (lastDot >= 0 && lastDot < fileName.length() - 1) {
      final String extension = ";" + fileName.substring(lastDot + 1).toLowerCase() + ";";
      final String semiFilter = ";" + filter + ";";
      return semiFilter.contains(extension);
    }
    return false;
  }

  /**
   * Writes compiler options.
   * 
   * @param writer
   *          Writer writer
   * @param isDebug
   *          true if debug.
   * @param baseDir
   *          String base directory
   * @param compilerConfig
   *          compiler configuration
   * @throws IOException
   *           if error on writing project
   */
  private void writeCompileOptions(final Writer writer, final boolean isDebug, final String baseDir,
      final CommandLineCompilerConfiguration compilerConfig) throws IOException {
    final StringBuffer baseOptions = new StringBuffer(50);
    baseOptions.append("# ADD BASE CPP");
    final StringBuffer options = new StringBuffer(50);
    options.append("# ADD CPP");
    final File[] includePath = compilerConfig.getIncludePath();
    for (final File element : includePath) {
      options.append(" /I \"");
      final String relPath = CUtil.getRelativePath(baseDir, element);
      options.append(CUtil.toWindowsPath(relPath));
      options.append('"');
    }
    final Hashtable<String, String> optionMap = new Hashtable<>();

    if (isDebug) {
      //
      // release options that should be mapped to debug counterparts
      //
      optionMap.put("/MT", "/MTd");
      optionMap.put("/ML", "/MLd");
      optionMap.put("/MD", "/MDd");
      optionMap.put("/O2", "/Od");
      optionMap.put("/O3", "/Od");
    } else {
      //
      // debug options that should be mapped to release counterparts
      //
      optionMap.put("/MTD", "/MT");
      optionMap.put("/MLD", "/ML");
      optionMap.put("/MDD", "/MD");
      optionMap.put("/GM", "");
      optionMap.put("/ZI", "");
      optionMap.put("/OD", "/O2");
      optionMap.put("/GZ", "");
    }

    final String[] preArgs = compilerConfig.getPreArguments();
    for (final String preArg : preArgs) {
      if (preArg.startsWith("/D")) {
        options.append(" /D ");
        baseOptions.append(" /D ");
        final String body = preArg.substring(2);
        if (preArg.indexOf('=') >= 0) {
          options.append(body);
          baseOptions.append(body);
        } else {
          final StringBuffer buf = new StringBuffer("\"");
          if ("NDEBUG".equals(body) || "_DEBUG".equals(body)) {
            if (isDebug) {
              buf.append("_DEBUG");
            } else {
              buf.append("NDEBUG");
            }
          } else {
            buf.append(body);
          }
          buf.append("\"");
          options.append(buf);
          baseOptions.append(buf);
        }
      } else if (!preArg.startsWith("/I")) {
        String option = preArg;
        final String key = option.toUpperCase(Locale.US);
        if (optionMap.containsKey(key)) {
          option = optionMap.get(key);
        }
        options.append(" ");
        options.append(option);
        baseOptions.append(" ");
        baseOptions.append(option);
      }
    }
    baseOptions.append("\r\n");
    options.append("\r\n");
    writer.write(baseOptions.toString());
    writer.write(options.toString());

  }

  private void writeConfig(final Writer writer, final boolean isDebug, final List<DependencyDef> dependencies,
      final String basePath, final CommandLineCompilerConfiguration compilerConfig, final TargetInfo linkTarget,
      final Map<String, TargetInfo> targets) throws IOException {
    writer.write("# PROP BASE Use_MFC 0\r\n");

    String configType = "Release";
    String configInt = "0";
    String configMacro = "NDEBUG";
    if (isDebug) {
      configType = "Debug";
      configInt = "1";
      configMacro = "_DEBUG";
    }

    writer.write("# PROP BASE Use_Debug_Libraries ");
    writer.write(configInt);
    writer.write("\r\n# PROP BASE Output_Dir \"");
    writer.write(configType);
    writer.write("\"\r\n");
    writer.write("# PROP BASE Intermediate_Dir \"");
    writer.write(configType);
    writer.write("\"\r\n");
    writer.write("# PROP BASE Target_Dir \"\"\r\n");
    writer.write("# PROP Use_MFC 0\r\n");
    writer.write("# PROP Use_Debug_Libraries ");
    writer.write(configInt);
    writer.write("\r\n# PROP Output_Dir \"");
    writer.write(configType);
    writer.write("\"\r\n");
    writer.write("# PROP Intermediate_Dir \"");
    writer.write(configType);
    writer.write("\"\r\n");
    writer.write("# PROP Target_Dir \"\"\r\n");
    writeCompileOptions(writer, isDebug, basePath, compilerConfig);
    writer.write("# ADD BASE MTL /nologo /D \"" + configMacro + "\" /mktyplib203 /o NUL /win32\r\n");
    writer.write("# ADD MTL /nologo /D \"" + configMacro + "\" /mktyplib203 /o NUL /win32\r\n");
    writer.write("# ADD BASE RSC /l 0x409 /d \"" + configMacro + "\"\r\n");
    writer.write("# ADD RSC /l 0x409 /d \"" + configMacro + "\"\r\n");
    writer.write("BSC32=bscmake.exe\r\n");
    writer.write("# ADD BASE BSC32 /nologo\r\n");
    writer.write("# ADD BSC32 /nologo\r\n");
    writer.write("LINK32=link.exe\r\n");
    writeLinkOptions(writer, isDebug, dependencies, basePath, linkTarget, targets);
  }

  /**
   * Writes link options.
   * 
   * @param writer
   *          Writer writer
   * @param basePath
   *          String base path
   * @param dependencies
   *          project dependencies, used to suppress explicit linking.
   * @param linkTarget
   *          TargetInfo link target
   * @param targets
   *          Hashtable all targets
   * @throws IOException
   *           if unable to write to project file
   */
  private void writeLinkOptions(final Writer writer, final boolean isDebug, final List<DependencyDef> dependencies,
      final String basePath, final TargetInfo linkTarget, final Map<String, TargetInfo> targets) throws IOException {

    final StringBuffer baseOptions = new StringBuffer(100);
    final StringBuffer options = new StringBuffer(100);
    baseOptions.append("# ADD BASE LINK32");
    options.append("# ADD LINK32");

    final ProcessorConfiguration config = linkTarget.getConfiguration();
    if (config instanceof CommandLineLinkerConfiguration) {
      final CommandLineLinkerConfiguration linkConfig = (CommandLineLinkerConfiguration) config;

      final File[] linkSources = linkTarget.getAllSources();
      for (final File linkSource : linkSources) {
        //
        // if file was not compiled or otherwise generated
        //
        if (targets.get(linkSource.getName()) == null) {
          //
          // if source appears to be a system library or object file
          // just output the name of the file (advapi.lib for example)
          // otherwise construct a relative path.
          //
          String relPath = linkSource.getName();
          //
          // check if file comes from a project dependency
          // if it does it should not be explicitly linked
          boolean fromDependency = false;
          if (relPath.indexOf(".") > 0) {
            final String baseName = relPath.substring(0, relPath.indexOf("."));
            for (final DependencyDef depend : dependencies) {
              if (baseName.compareToIgnoreCase(depend.getName()) == 0) {
                fromDependency = true;
              }
            }
          }
          if (!fromDependency) {
            if (!CUtil.isSystemPath(linkSource)) {
              relPath = CUtil.getRelativePath(basePath, linkSource);
            }
            //
            // if path has an embedded space then
            // must quote
            if (relPath.indexOf(' ') > 0) {
              options.append(" \"");
              options.append(CUtil.toWindowsPath(relPath));
              options.append("\"");
            } else {
              options.append(' ');
              options.append(CUtil.toWindowsPath(relPath));
            }
          }
        }
      }
      final String[] preArgs = linkConfig.getPreArguments();
      for (final String preArg : preArgs) {
        if (isDebug || !preArg.equals("/DEBUG")) {
          options.append(' ');
          options.append(preArg);
          baseOptions.append(' ');
          baseOptions.append(preArg);
        }
      }
      final String[] endArgs = linkConfig.getEndArguments();
      for (final String endArg : endArgs) {
        options.append(' ');
        options.append(endArg);
        baseOptions.append(' ');
        baseOptions.append(endArg);
      }
    }
    baseOptions.append("\r\n");
    options.append("\r\n");
    writer.write(baseOptions.toString());
    writer.write(options.toString());
  }

  /**
   * Writes "This is not a makefile" warning.
   * 
   * @param writer
   *          Writer writer
   * @param projectName
   *          String project name
   * @param targtype
   *          String target type
   * @throws IOException
   *           if error writing project
   */

  private void writeMessage(final Writer writer, final String projectName, final String targtype) throws IOException {
    writer.write("!MESSAGE This is not a valid makefile. ");
    writer.write("To build this project using NMAKE,\r\n");
    writer.write("!MESSAGE use the Export Makefile command and run\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("!MESSAGE NMAKE /f \"");
    writer.write(projectName);
    writer.write(".mak\".\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("!MESSAGE You can specify a configuration when running NMAKE\r\n");
    writer.write("!MESSAGE by defining the macro CFG on the command line. ");
    writer.write("For example:\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("!MESSAGE NMAKE /f \"");
    writer.write(projectName);
    writer.write(".mak\" CFG=\"");
    writer.write(projectName);
    writer.write(" - Win32 Debug\"\r\n");
    writer.write("!MESSAGE \r\n");
    writer.write("!MESSAGE Possible choices for configuration are:\r\n");
    writer.write("!MESSAGE \r\n");
    final String pattern = "!MESSAGE \"{0} - Win32 {1}\" (based on \"{2}\")\r\n";
    writer.write(MessageFormat.format(pattern, new Object[] {
        projectName, "Release", targtype
    }));
    writer.write(MessageFormat.format(pattern, new Object[] {
        projectName, "Debug", targtype
    }));
    writer.write("!MESSAGE \r\n");
    writer.write("\r\n");

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
   * @param files
   *          source files
   * @param targets
   *          compilation targets
   * @param linkTarget
   *          link target
   * @throws IOException
   *           if error writing project file
   */
  @Override
  public void writeProject(final File fileName, final CCTask task, final ProjectDef projectDef, final List<File> files,
      final Map<String, TargetInfo> targets, final TargetInfo linkTarget) throws IOException {

    //
    // some characters are apparently not allowed in VS project names
    // but have not been able to find them documented
    // limiting characters to alphas, numerics and hyphens
    String projectName = projectDef.getName();
    if (projectName != null) {
      projectName = toProjectName(projectName);
    } else {
      projectName = toProjectName(fileName.getName());
    }

    final String basePath = fileName.getAbsoluteFile().getParent();

    final File dspFile = new File(fileName + ".dsp");
    if (!projectDef.getOverwrite() && dspFile.exists()) {
      throw new BuildException("Not allowed to overwrite project file " + dspFile.toString());
    }
    final File dswFile = new File(fileName + ".dsw");
    if (!projectDef.getOverwrite() && dswFile.exists()) {
      throw new BuildException("Not allowed to overwrite project file " + dswFile.toString());
    }

    final CommandLineCompilerConfiguration compilerConfig = getBaseCompilerConfiguration(targets);
    if (compilerConfig == null) {
      throw new BuildException("Unable to generate Visual Studio project " + "when Microsoft C++ is not used.");
    }

    Writer writer = new BufferedWriter(new FileWriter(dspFile));
    writer.write("# Microsoft Developer Studio Project File - Name=\"");
    writer.write(projectName);
    writer.write("\" - Package Owner=<4>\r\n");
    writer.write("# Microsoft Developer Studio Generated Build File, Format Version ");
    writer.write(this.version);
    writer.write("\r\n");
    writer.write("# ** DO NOT EDIT **\r\n\r\n");

    writeComments(writer, projectDef.getComments());

    final String outputType = task.getOuttype();
    final String subsystem = task.getSubsystem();
    String targtype = "Win32 (x86) Dynamic-Link Library";
    String targid = "0x0102";
    if ("executable".equals(outputType)) {
      if ("console".equals(subsystem)) {
        targtype = "Win32 (x86) Console Application";
        targid = "0x0103";
      } else {
        targtype = "Win32 (x86) Application";
        targid = "0x0101";
      }
    } else if ("static".equals(outputType)) {
      targtype = "Win32 (x86) Static Library";
      targid = "0x0104";
    }
    writer.write("# TARGTYPE \"");
    writer.write(targtype);
    writer.write("\" ");
    writer.write(targid);
    writer.write("\r\n\r\nCFG=");

    writer.write(projectName + " - Win32 Debug");
    writer.write("\r\n");

    writeMessage(writer, projectName, targtype);

    writer.write("# Begin Project\r\n");
    if (this.version.equals("6.00")) {
      writer.write("# PROP AllowPerConfigDependencies 0\r\n");
    }
    writer.write("# PROP Scc_ProjName \"\"\r\n");
    writer.write("# PROP Scc_LocalPath \"\"\r\n");
    writer.write("CPP=cl.exe\r\n");
    writer.write("MTL=midl.exe\r\n");
    writer.write("RSC=rc.exe\r\n");

    writer.write("\r\n!IF  \"$(CFG)\" == \"" + projectName + " - Win32 Release\"\r\n");

    writeConfig(writer, false, projectDef.getDependencies(), basePath, compilerConfig, linkTarget, targets);

    writer.write("\r\n!ELSEIF  \"$(CFG)\" == \"" + projectName + " - Win32 Debug\"\r\n");

    writeConfig(writer, true, projectDef.getDependencies(), basePath, compilerConfig, linkTarget, targets);

    writer.write("\r\n!ENDIF\r\n");

    writer.write("# Begin Target\r\n\r\n");
    writer.write("# Name \"" + projectName + " - Win32 Release\"\r\n");
    writer.write("# Name \"" + projectName + " - Win32 Debug\"\r\n");

    final File[] sortedSources = getSources(files);

    if (this.version.equals("6.00")) {
      final String sourceFilter = "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat";
      final String headerFilter = "h;hpp;hxx;hm;inl";
      final String resourceFilter = "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe";

      writer.write("# Begin Group \"Source Files\"\r\n\r\n");
      writer.write("# PROP Default_Filter \"" + sourceFilter + "\"\r\n");

      for (final File sortedSource1 : sortedSources) {
        if (!isGroupMember(headerFilter, sortedSource1) && !isGroupMember(resourceFilter, sortedSource1)) {
          writeSource(writer, basePath, sortedSource1);
        }
      }
      writer.write("# End Group\r\n");

      writer.write("# Begin Group \"Header Files\"\r\n\r\n");
      writer.write("# PROP Default_Filter \"" + headerFilter + "\"\r\n");

      for (final File sortedSource : sortedSources) {
        if (isGroupMember(headerFilter, sortedSource)) {
          writeSource(writer, basePath, sortedSource);
        }
      }
      writer.write("# End Group\r\n");

      writer.write("# Begin Group \"Resource Files\"\r\n\r\n");
      writer.write("# PROP Default_Filter \"" + resourceFilter + "\"\r\n");

      for (final File sortedSource : sortedSources) {
        if (isGroupMember(resourceFilter, sortedSource)) {
          writeSource(writer, basePath, sortedSource);
        }
      }
      writer.write("# End Group\r\n");

    } else {
      for (final File sortedSource : sortedSources) {
        writeSource(writer, basePath, sortedSource);
      }
    }

    writer.write("# End Target\r\n");
    writer.write("# End Project\r\n");
    writer.close();

    //
    // write workspace file
    //
    writer = new BufferedWriter(new FileWriter(dswFile));
    writeWorkspace(writer, projectDef, projectName, dspFile);
    writer.close();

  }

  /**
   * Writes the entry for one source file in the project.
   * 
   * @param writer
   *          Writer writer
   * @param basePath
   *          String base path for project
   * @param groupMember
   *          File project source file
   * @throws IOException
   *           if error writing project file
   */
  private void writeSource(final Writer writer, final String basePath, final File groupMember) throws IOException {
    writer.write("# Begin Source File\r\n\r\nSOURCE=");
    String relativePath = CUtil.getRelativePath(basePath, groupMember);
    //
    // if relative path is just a name (hello.c) then
    // make it .\hello.c
    if (!relativePath.startsWith(".") && !relativePath.contains(":") && !relativePath.startsWith("\\")) {
      relativePath = ".\\" + relativePath;
    }
    writer.write(CUtil.toWindowsPath(relativePath));
    writer.write("\r\n# End Source File\r\n");
  }

  private void writeWorkspace(final Writer writer, final ProjectDef project, final String projectName,
      final File dspFile) throws IOException {

    writer.write("Microsoft Developer Studio Workspace File, Format Version ");
    writer.write(this.version);
    writer.write("\r\n");
    writer.write("# WARNING: DO NOT EDIT OR DELETE");
    writer.write(" THIS WORKSPACE FILE!\r\n\r\n");

    writeComments(writer, project.getComments());

    final List<DependencyDef> dependencies = project.getDependencies();
    final List<String> projectDeps = new ArrayList<>();
    final String basePath = dspFile.getParent();
    for (final DependencyDef dep : dependencies) {
      if (dep.getFile() != null) {
        final String projName = toProjectName(dep.getName());
        projectDeps.add(projName);
        final String depProject = CUtil
            .toWindowsPath(CUtil.getRelativePath(basePath, new File(dep.getFile() + ".dsp")));
        writeWorkspaceProject(writer, projName, depProject, dep.getDependsList());
      }
    }

    writeWorkspaceProject(writer, projectName, dspFile.getName(), projectDeps);

    writer.write("############################################");
    writer.write("###################################\r\n\r\n");

    writer.write("Global:\r\n\r\nPackage=<5>\r\n{{{\r\n}}}");
    writer.write("\r\n\r\nPackage=<3>\r\n{{{\r\n}}}\r\n\r\n");

    writer.write("########################################");
    writer.write("#######################################\r\n\r\n");

  }
}
