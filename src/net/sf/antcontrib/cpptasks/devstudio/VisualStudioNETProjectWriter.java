/*
 *
 * Copyright 2004-2005 The Ant-Contrib project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.antcontrib.cpptasks.devstudio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.TargetInfo;
import net.sf.antcontrib.cpptasks.compiler.CommandLineCompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import net.sf.antcontrib.cpptasks.ide.ProjectDef;
import net.sf.antcontrib.cpptasks.ide.ProjectWriter;

import org.apache.tools.ant.BuildException;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Writes a Visual Studio.NET project file.
 * (Visual Studio 5 and 6 project writer is substantially more
* complete at this point).
 * @author curta
 *
 */
public final class VisualStudioNETProjectWriter
    implements ProjectWriter {
  /**
   * Version of VisualStudio.NET.
   */
  private String version;

  /**
   * Constructor.
   * @param versionArg String VisualStudio.NET version
   */
  public VisualStudioNETProjectWriter(final String versionArg) {
    this.version = versionArg;
  }

  /**
   * Writes a project definition file.
   *
   * @param fileName
   *            project name for file, should has .cbx extension
   * @param task
   *            cc task for which to write project
   * @param projectDef
   *            project element
   * @param sources source files
   * @param targets compilation targets
   * @param linkTarget link target
   * @throws IOException if I/O error
   * @throws SAXException if XML serialization error
   */
  public void writeProject(final File fileName,
                           final CCTask task,
                           final ProjectDef projectDef,
                           final List sources,
                           final Hashtable targets,
                           final TargetInfo linkTarget) throws
      IOException,
      SAXException {

    boolean isDebug = task.getDebug();

    String projectName = projectDef.getName();
    if (projectName == null) {
      projectName = fileName.getName();
    }


    File dspFile = new File(fileName + ".dsp");
    if (!projectDef.getOverwrite() && dspFile.exists()) {
      throw new BuildException("Not allowed to overwrite project file "
                               + dspFile.toString());
    }
    File dswFile = new File(fileName + ".dsw");
    if (!projectDef.getOverwrite() && dswFile.exists()) {
        throw new BuildException("Not allowed to overwrite project file "
                                 + dswFile.toString());
      }

    CommandLineCompilerConfiguration compilerConfig =
        getBaseCompilerConfiguration(targets);
    if (compilerConfig == null) {
      throw new BuildException(
          "Unable to generate Visual Studio.NET project "
          + "when Microsoft C++ is not used.");
    }

    OutputStream outStream = new FileOutputStream(fileName + ".vcproj");
    OutputFormat format = new OutputFormat("xml", "UTF-8", true);
    Serializer serializer = new XMLSerializer(outStream, format);
    ContentHandler content = serializer.asContentHandler();
    String basePath = fileName.getParentFile().getAbsolutePath();
    content.startDocument();
    AttributesImpl emptyAttrs = new AttributesImpl();
    startElement(content, "VisualStudioProject", new String[] {"ProjectType",
                 "Version", "Name", "SccProjectName", "SccLocalPath"}
                 ,
                 new String[] {"Visual C++", this.version, projectName,
                 "", ""});
    content.startElement(null, "Platforms", "Platforms", emptyAttrs);
    startElement(content, "Platform", new String[] {"Name"}
                 , new String[] {"Win32"});
    content.endElement(null, "Platform", "Platform");
    content.endElement(null, "Platforms", "Platforms");
    content.startElement(null, "Configurations", "Configurations", emptyAttrs);

    String[] configValues = new String[] {
            "Debug|Win32",
            ".\\Debug",
            ".\\Debug",
            "2",
            "0",
            "FALSE"};
    if (!isDebug) {
        configValues[0] = "Release|Win32";
    }

    startElement(content, "Configuration",
                 new String[] {"Name", "OutputDirectory",
                 "IntermediateDirectory", "ConfigurationType", "UseOfMFC",
                 "ATLMinimizeCRunTimeLibraryUsage"}
                 , configValues);
    String[] clValues = new String[] {
        "VCCLCompilerTool", "0", null, null,
        "1", "2", ".\\Debug\\testdllproh.pch", ".\\Debug/",
        ".\\Debug/", ".\\Debug/", "3", "TRUE", "4"};
    StringBuffer includeDirs = new StringBuffer();
    StringBuffer defines = new StringBuffer();
    String[] args = compilerConfig.getPreArguments();
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("/I")) {
        includeDirs.append(args[i].substring(2));
        includeDirs.append(';');
      }
      if (args[i].startsWith("/D")) {
        defines.append(args[i].substring(2));
        defines.append(";");
      }
    }

    if (includeDirs.length() > 0) {
      includeDirs.setLength(includeDirs.length() - 1);
    }
    if (defines.length() > 0) {
        defines.setLength(defines.length() - 1);
    }
    clValues[2] = includeDirs.toString();
    clValues[3] = defines.toString();

    startElement(content, "Tool",
                 new String[] {"Name", "Optimization",
                 "AdditionalIncludeDirectories",
                 "PreprocessorDefinitions", "RuntimeLibrary",
                 "UsePrecompiledHeaders", "PrecompiledHeaderFile",
                 "AssemblerListingLocation", "ObjectFile", "WarningLevel",
                 "SuppressStartupBanner", "DebugInformationFormat"}
                 , clValues);
    content.endElement(null, "Tool", "Tool");


    String[] linkerValues = new String[] {"VCLinkerTool", null,
            ".\\Debug/testdllproj.dll", "1",
            "TRUE", "TRUE", ".\\Debug\\testdllproh.pdb", "2",
            ".\\Debug/testdllproj.lib", "1"};

    if (!isDebug) {
        linkerValues[5] = "FALSE";
    }

    ProcessorConfiguration config = linkTarget.getConfiguration();
    if (config instanceof CommandLineLinkerConfiguration) {
      CommandLineLinkerConfiguration linkConfig =
          (CommandLineLinkerConfiguration) config;

      File[] linkSources = linkTarget.getAllSources();
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < linkSources.length; i++) {
        //
        //   if file was not compiled or otherwise generated
        //
        if (targets.get(linkSources[i].getName()) == null) {
          String relPath = CUtil.getRelativePath(basePath, linkSources[i]);
          //
          //   if path has an embedded space then
          //      must quote
          if (relPath.indexOf(' ') > 0) {
            buf.append('\"');
            buf.append(relPath);
            buf.append('\"');
          } else {
             buf.append(relPath);
          }
          buf.append(';');
        }
      }
      if (buf.length() > 0) {
        buf.setLength(buf.length() - 1);
        linkerValues[1] = buf.toString();
      }
    }

    startElement(content, "Tool",
                 new String[] {"Name",
                               "AdditionalDependencies",
                               "OutputFile",
                               "LinkIncremental",
                               "SuppressStartupBanner",
                               "GenerateDebugInformation",
                               "ProgramDatabaseFile",
                               "SubSystem",
                               "ImportLibrary",
                               "TargetMachine"}
                 , linkerValues);
    content.endElement(null, "Tool", "Tool");
    content.endElement(null, "Configuration", "Configuration");
    content.endElement(null, "Configurations", "Configurations");
    content.startElement(null, "References", "References", emptyAttrs);
    content.endElement(null, "References", "References");
    content.startElement(null, "Files", "Files", emptyAttrs);


    File[] sortedSources = new File[sources.size()];
    sources.toArray(sortedSources);
    Arrays.sort(sortedSources, new Comparator() {
      public int compare(final Object o1, final Object o2) {
        return ((File) o1).getName().compareTo(((File) o2).getName());
      }
    });

    writeFilteredSources("Source Files",
       "cpp;c;cxx;def;odl;idl;hpj;bat;asm;asmx",
       basePath, sortedSources, content);

    writeFilteredSources("Header Files", "h;hpp;hxx;hm;inl;inc;xsd",
       basePath, sortedSources, content);

    writeFilteredSources("Resource Files",
       "rc;ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe;resx",
       basePath, sortedSources, content);

    content.endElement(null, "Files", "Files");
    content.startElement(null, "Globals", "Globals", emptyAttrs);
    content.endElement(null, "Globals", "Globals");
    content.endElement(null, "VisualStudioProject", "VisualStudioProject");
    content.endDocument();
  }

  /**
   * Writes a cluster of source files to the project.
   * @param name name of filter
   * @param filter file extensions
   * @param basePath base path for files
   * @param sortedSources array of source files
   * @param content generated project
   * @throws SAXException if invalid content
   */
  private void writeFilteredSources(final String name, final String filter,
        final String basePath,
        final File[] sortedSources,
        final ContentHandler content)
        throws SAXException {
    AttributesImpl filterAttrs = new AttributesImpl();
    filterAttrs.addAttribute(null, "Name", "Name", "#PCDATA", name);
    filterAttrs.addAttribute(null, "Filter", "Filter", "#PCDATA", filter);
    content.startElement(null, "Filter", "Filter", filterAttrs);


    AttributesImpl fileAttrs = new AttributesImpl();
    fileAttrs.addAttribute(null, "RelativePath", "RelativePath",
                           "#PCDATA", "");

    AttributesImpl fileConfigAttrs = new AttributesImpl();
    fileConfigAttrs.addAttribute(null, "Name", "Name",
                                 "#PCDATA", "Debug|Win32");

    AttributesImpl toolAttrs = new AttributesImpl();
    toolAttrs.addAttribute(null, "Name", "Name",
                           "#PCDATA", "VCCLCompilerTool");
    toolAttrs.addAttribute(null, "Optimization", "Optimization",
                           "#PCDATA", "0");
    toolAttrs.addAttribute(null, "PreprocessorDefinitions",
                           "PreprocessorDefinitions", "#PCDATA",
                           "WIN32;_DEBUG;_WINDOWS;$(NoInherit}");

    for (int i = 0; i < sortedSources.length; i++) {
       if (isGroupMember(filter, sortedSources[i])) {
            String relativePath = CUtil.getRelativePath(basePath,
                             sortedSources[i]);
            fileAttrs.setValue(0, relativePath);
            content.startElement(null, "File", "File", fileAttrs);
            content.startElement(null, "FileConfiguration", "FileConfiguration",
                             fileConfigAttrs);
            content.startElement(null, "Tool", "Tool", toolAttrs);
            content.endElement(null, "Tool", "Tool");
            content.endElement(null, "FileConfiguration", "FileConfiguration");
            content.endElement(null, "File", "File");
        }
     }
     content.endElement(null, "Filter", "Filter");

  }

  /**
   * Returns true if the file has an extension that appears in the group filter.
   * @param filter String group filter
   * @param candidate File file
   * @return boolean true if member of group
   */
  private boolean isGroupMember(final String filter, final File candidate) {
    String fileName = candidate.getName();
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot >= 0 && lastDot < fileName.length() - 1) {
      String extension =
           ";" + fileName.substring(lastDot + 1).toLowerCase() + ";";
      String semiFilter = ";" + filter + ";";
      return semiFilter.indexOf(extension) >= 0;
    }
    return false;
  }


  /**
   * Start an element.
   * @param content ContentHandler content handler
   * @param tagName String tag name
   * @param attributeNames String[] attribute names
   * @param attributeValues String[] attribute values
   * @throws SAXException if error writing element
   */
  private void startElement(final ContentHandler content,
                                  final String tagName,
                                  final String[] attributeNames,
                                  final String[] attributeValues)
      throws SAXException {
    AttributesImpl attributes = new AttributesImpl();
    for (int i = 0; i < attributeNames.length; i++) {
        if (attributeValues[i] != null) {
            attributes.addAttribute(null, attributeNames[i],
               attributeNames[i], "#PCDATA", attributeValues[i]);
        }
    }
    content.startElement(null, tagName, tagName, attributes);
  }

  /**
   * Gets the first recognized compiler from the
   * compilation targets.
   * @param targets compilation targets
   * @return representative (hopefully) compiler configuration
   */
  private CommandLineCompilerConfiguration
      getBaseCompilerConfiguration(final Hashtable targets) {
    //
    //   find first target with an DevStudio C compilation
    //
    CommandLineCompilerConfiguration compilerConfig = null;
    //
    //   get the first target and assume that it is representative
    //
    Iterator targetIter = targets.values().iterator();
    while (targetIter.hasNext()) {
      TargetInfo targetInfo = (TargetInfo) targetIter.next();
      ProcessorConfiguration config = targetInfo.getConfiguration();
      String identifier = config.getIdentifier();
      //
      //   for the first cl compiler
      //
      if (config instanceof CommandLineCompilerConfiguration) {
        compilerConfig = (CommandLineCompilerConfiguration) config;
        if (compilerConfig.getCompiler() instanceof DevStudioCCompiler) {
          return compilerConfig;
        }
      }
    }
    return null;
  }

}
