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
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.msvc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.apache.tools.ant.BuildException;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

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
 * Writes a Visual Studio.NET project file.
 *
 * @author curta
 */
public final class VisualStudioNETProjectWriter
        implements ProjectWriter {
    /**
     * Version of VisualStudio.NET.
     */
    private final String version;

    /**
     * Literal to represent a true value.
     */
    private final String trueLiteral;

    /**
     * Literal to represent a false value.
     */
    private final String falseLiteral;

    /**
     * Constructor.
     *
     * @param versionArg String VisualStudio.NET version
     * @param trueArg    literal to represent true, "true" in VC 2005.
     * @param falseArg    literal to represent false, "false" in VC 2005.
     */
    public VisualStudioNETProjectWriter(final String versionArg,
                                        final String trueArg,
                                        final String falseArg) {
        if (versionArg == null) {
            throw new IllegalArgumentException("versionArg");
        }
        if (trueArg == null) {
            throw new IllegalArgumentException("trueArg");
        }
        if (falseArg == null) {
            throw new IllegalArgumentException("falseArg");
        }
        this.version = versionArg;
        this.trueLiteral = trueArg;
        this.falseLiteral = falseArg;
    }
    


    /**
     * Gets the configuration type.
     *
     * @param task cc task, may not be null.
     * @return configuration type
     */
    private String getConfigurationType(final CCTask task) {
        String outputType = task.getOuttype();
        String targtype = "2"; // Win32 (x86) Dynamic-Link Library";
        if ("executable".equals(outputType)) {
            targtype = "1"; // "Win32 (x86) Console Application";
        } else if ("static".equals(outputType)) {
            targtype = "4"; //"Win32 (x86) Static Library";
        }
        return targtype;
    }



    /**
     * Get character set for Windows API.
     * @param compilerConfig compiler configuration, may not be null.
     * @return "1" is TCHAR is unicode, "0" if TCHAR is multi-byte.
     */
    private String getCharacterSet(
            final CommandLineCompilerConfiguration compilerConfig) {
        String[] args = compilerConfig.getPreArguments();
        String charset = "0";
        for (int i = 0; i < args.length; i++) {
            if ("/D_UNICODE".equals(args[i]) || "/DUNICODE".equals(args[i])) {
                charset = "1";
            }
            if ("/D_MBCS".equals(args[i])) {
                charset = "2";
            }
        }
        return charset;
    }

    /**
     * Write the start tag of the Configuration element.
     * @param content serialization content handler.
     * @param isDebug if true, write a debug configuration.
     * @param task cc task.
     * @param compilerConfig compiler configuration.
     * @throws SAXException thrown if serialization error.
     */
    private void writeConfigurationStartTag(final ContentHandler content,
                                            final boolean isDebug,
                                            final CCTask task,
                  final CommandLineCompilerConfiguration compilerConfig)
            throws SAXException {
        AttributesImpl attributes = new AttributesImpl();
        if (isDebug) {
            addAttribute(attributes, "Name", "Debug|Win32");
            addAttribute(attributes, "OutputDirectory", "Debug");
            addAttribute(attributes, "IntermediateDirectory", "Debug");
        } else {
            addAttribute(attributes, "Name", "Release|Win32");
            addAttribute(attributes, "OutputDirectory", "Release");
            addAttribute(attributes, "IntermediateDirectory", "Release");

        }
        addAttribute(attributes, "ConfigurationType",
                getConfigurationType(task));
        addAttribute(attributes, "CharacterSet",
                getCharacterSet(compilerConfig));
        content.startElement(null,
                "Configuration", "Configuration", attributes);
    }

    /**
     * Get value of Optimization property.
     * @param compilerConfig compiler configuration, may not be null.
     * @return value of Optimization property.
     */
    private String getOptimization(
            final CommandLineCompilerConfiguration compilerConfig) {
        String[] args = compilerConfig.getPreArguments();
        String opt = "0";
        for (int i = 0; i < args.length; i++) {
            if ("/Od".equals(args[i])) {
                opt = "0";
            }
            if ("/O1".equals(args[i])) {
                opt = "1";
            }
            if ("/O2".equals(args[i])) {
                opt = "2";
            }
            if ("/Ox".equals(args[i])) {
                opt = "3";
            }
        }
        return opt;
    }

    /**
     * Get value of AdditionalIncludeDirectories property.
     * @param compilerConfig compiler configuration.
     * @param baseDir base for relative paths.
     * @return value of AdditionalIncludeDirectories property.
     */
    private String getAdditionalIncludeDirectories(
            final String baseDir,
            final CommandLineCompilerConfiguration compilerConfig) {
        File[] includePath = compilerConfig.getIncludePath();
        StringBuffer includeDirs = new StringBuffer();
		// Darren Sargent Feb 10 2010 -- reverted to older code to ensure sys
		// includes get, erm, included
		String[] args = compilerConfig.getPreArguments();

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("/I")) {
				includeDirs.append(args[i].substring(2));
          includeDirs.append(';');
        }

		}
		// end Darren

        if (includeDirs.length() > 0) {
            includeDirs.setLength(includeDirs.length() - 1);
        }
        return includeDirs.toString();
    }

    /**
     * Get value of PreprocessorDefinitions property.
     * @param compilerConfig compiler configuration.
     * @param isDebug true if generating debug configuration.
     * @return value of PreprocessorDefinitions property.
     */
    private String getPreprocessorDefinitions(
            final CommandLineCompilerConfiguration compilerConfig,
            final boolean isDebug) {
        StringBuffer defines = new StringBuffer();
        String[] args = compilerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("/D")) {
                String macro = args[i].substring(2);
                if (isDebug) {
                    if (macro.equals("NDEBUG")) {
                        macro = "_DEBUG";
                    }
                } else {
                    if (macro.equals("_DEBUG")) {
                        macro = "NDEBUG";
                    }
                }
                defines.append(macro);
                defines.append(";");
            }
        }

        if (defines.length() > 0) {
            defines.setLength(defines.length() - 1);
        }
        return defines.toString();
    }

    /**
     * Get value of RuntimeLibrary property.
     * @param compilerConfig compiler configuration.
     * @param isDebug true if generating debug configuration.
     * @return value of RuntimeLibrary property.
     */
    private String getRuntimeLibrary(
            final CommandLineCompilerConfiguration compilerConfig,
            final boolean isDebug) {
        String rtl = null;
        String[] args = compilerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("/MT")) {
                if (isDebug) {
                    rtl = "1";
                } else {
                    rtl = "0";
                }
            } else if (args[i].startsWith("/MD")) {
                if (isDebug) {
                    rtl = "3";
                } else {
                    rtl = "2";
                }
            }
        }
        return rtl;
    }

    /**
     * Get value of UsePrecompiledHeader property.
     * @param compilerConfig compiler configuration.
     * @return value of UsePrecompiledHeader property.
     */
    private String getUsePrecompiledHeader(
            final CommandLineCompilerConfiguration compilerConfig) {
        String usePCH = "0";
        String[] args = compilerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if ("/Yc".equals(args[i])) {
                usePCH = "1";
            }
            if ("/Yu".equals(args[i])) {
                usePCH = "2";
            }
        }
        return usePCH;
    }

    /**
     * Get value of PrecompiledHeaderFile property.
     * @param compilerConfig compiler configuration.
     * @return value of PrecompiledHeaderFile property.
     */
    private String getPrecompiledHeaderFile(
            final CommandLineCompilerConfiguration compilerConfig) {
        String pch = null;
        String[] args = compilerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("/Fp")) {
                pch = args[i].substring(3);
            }
        }
        return pch;
    }



    /**
     * Get value of BasicRuntimeChecks property.
     * @param compilerConfig compiler configuration.
     * @return value of BasicRuntimeChecks property.
     */
    private String getBasicRuntimeChecks(
            final CommandLineCompilerConfiguration compilerConfig) {
        String checks = "0";
        String[] args = compilerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if ("/RTCs".equals(args[i])) {
                checks = "1";
            }
            if ("/RTCu".equals(args[i])) {
                checks = "2";
            }
            if ("/RTC1".equals(args[i]) || "/GZ".equals(args[i])) {
                checks = "3";
            }
        }
        return checks;
    }

    /**
     * Get value of WarningLevel property.
     * @param compilerConfig compiler configuration.
     * @return value of WarningLevel property.
     */
    private String getWarningLevel(
            final CommandLineCompilerConfiguration compilerConfig) {
        String warn = null;
        String[] args = compilerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if ("/W0".equals(args[i])) {
                warn = "0";
            }
            if ("/W1".equals(args[i])) {
                warn = "1";
            }
            if ("/W2".equals(args[i])) {
                warn = "2";
            }
            if ("/W3".equals(args[i])) {
                warn = "3";
            }
			// Added by Darren Sargent, 2/26/2008
			if ("/W4".equals(args[i])) {
				warn = "4";
			}
			// end added
        }
        return warn;
    }

    /**
     * Get value of Detect64BitPortabilityProblems property.
     * @param compilerConfig compiler configuration.
     * @return value of Detect64BitPortabilityProblems property.
     */
    private String getDetect64BitPortabilityProblems(
            final CommandLineCompilerConfiguration compilerConfig) {
        String warn64 = null;
        String[] args = compilerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if ("/Wp64".equals(args[i])) {
                warn64 = trueLiteral;
            }
        }
        return warn64;
    }

    /**
     * Get value of DebugInformationFormat property.
     * @param compilerConfig compiler configuration.
     * @return value of DebugInformationFormat property.
     */
    private String getDebugInformationFormat(
            final CommandLineCompilerConfiguration compilerConfig) {
        String format = "0";
        String[] args = compilerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if ("/Z7".equals(args[i])) {
                format = "1";
            }
            if ("/Zd".equals(args[i])) {
                format = "2";
            }
            if ("/Zi".equals(args[i])) {
                format = "3";
            }
            if ("/ZI".equals(args[i])) {
                format = "4";
            }
        }
        return format;
    }

    /**
     * write the Compiler element.
     * @param content serialization content handler.
     * @param isDebug true if generating debug configuration.
     * @param basePath base for relative file paths.
     * @param compilerConfig compiler configuration.
     * @throws SAXException thrown if error during serialization.
     */
    private void writeCompilerElement(final ContentHandler content,
            final boolean isDebug,
            final String basePath,
            final CommandLineCompilerConfiguration compilerConfig)
            throws SAXException {
        AttributesImpl attributes = new AttributesImpl();
        addAttribute(attributes, "Name", "VCCLCompilerTool");
        String optimization = getOptimization(compilerConfig);
        String debugFormat = getDebugInformationFormat(compilerConfig);
        if(isDebug) {
            optimization = "0";
            if ("0".equals(debugFormat)) {
                debugFormat = "4";
            }
        } else {
            if ("0".equals(optimization)) {
                optimization = "2";
            }
            debugFormat = "0";
        }
        addAttribute(attributes, "Optimization", optimization);
        addAttribute(attributes, "AdditionalIncludeDirectories",
                getAdditionalIncludeDirectories(basePath, compilerConfig));
        addAttribute(attributes, "PreprocessorDefinitions",
                getPreprocessorDefinitions(compilerConfig, isDebug));
        addAttribute(attributes, "MinimalRebuild", trueLiteral);
        addAttribute(attributes, "BasicRuntimeChecks",
                getBasicRuntimeChecks(compilerConfig));
        addAttribute(attributes, "RuntimeLibrary",
                getRuntimeLibrary(compilerConfig, isDebug));
        addAttribute(attributes, "UsePrecompiledHeader",
                getUsePrecompiledHeader(compilerConfig));
        addAttribute(attributes, "PrecompiledHeaderFile",
                getPrecompiledHeaderFile(compilerConfig));
        addAttribute(attributes, "WarningLevel",
                getWarningLevel(compilerConfig));
        addAttribute(attributes, "Detect64BitPortabilityProblems",
                getDetect64BitPortabilityProblems(compilerConfig));
        addAttribute(attributes, "DebugInformationFormat",
                debugFormat);
        content.startElement(null, "Tool", "Tool", attributes);
        content.endElement(null, "Tool", "Tool");

    }


    /**
     * Get value of LinkIncremental property.
     * @param linkerConfig linker configuration.
     * @return value of LinkIncremental property
     */
    private String getLinkIncremental(
            final CommandLineLinkerConfiguration linkerConfig) {
        String incremental = "0";
        String[] args = linkerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if ("/INCREMENTAL:NO".equals(args[i])) {
                incremental = "1";
            }
            if ("/INCREMENTAL:YES".equals(args[i])) {
                incremental = "2";
            }
        }
        return incremental;
    }


    /**
     * Get value of Subsystem property.
     * @param linkerConfig linker configuration.
     * @return value of Subsystem property
     */
    private String getSubsystem(
            final CommandLineLinkerConfiguration linkerConfig) {
        String subsystem = "0";
        String[] args = linkerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if ("/SUBSYSTEM:CONSOLE".equals(args[i])) {
                subsystem = "1";
            }
            if ("/SUBSYSTEM:WINDOWS".equals(args[i])) {
                subsystem = "2";
            }
            if ("/SUBSYSTEM:WINDOWSCE".equals(args[i])) {
                subsystem = "9";
            }
        }
        return subsystem;
    }

    /**
     * Get value of TargetMachine property.
     * @param linkerConfig linker configuration.
     * @return value of TargetMachine property
     */
    private String getTargetMachine(
            final CommandLineLinkerConfiguration linkerConfig) {
        String subsystem = "0";
        String[] args = linkerConfig.getPreArguments();
        for (int i = 0; i < args.length; i++) {
            if ("/MACHINE:X86".equals(args[i])) {
                subsystem = "1";
            }
        }
        return subsystem;
    }

    /**
     * Get value of AdditionalDependencies property.
     * @param linkTarget link target.
     * @param projectDependencies dependencies declared in project.
     * @param targets all targets.
     * @param basePath path to directory containing project file.
     * @return value of AdditionalDependencies property.
     */
    private String getAdditionalDependencies(final TargetInfo linkTarget,
                                             final List<DependencyDef> projectDependencies,
                                             final Map<String, TargetInfo> targets,
                                             final String basePath) {
      String dependencies = null;
        File[] linkSources = linkTarget.getAllSources();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < linkSources.length; i++) {
          //
          //   if file was not compiled or otherwise generated
          //
          if (targets.get(linkSources[i].getName()) == null) {
            //
            //   if source appears to be a system library or object file
            //      just output the name of the file (advapi.lib for example)
            //      otherwise construct a relative path.
            //
            String relPath = linkSources[i].getName();
              //
              //   check if file comes from a project dependency
              //       if it does it should not be explicitly linked
            boolean fromDependency = false;
            if (relPath.indexOf(".") > 0) {
                  String baseName = relPath.substring(0, relPath.indexOf("."));
                  for(Iterator<DependencyDef> iter = projectDependencies.iterator(); iter.hasNext(); ) {
                    DependencyDef depend = iter.next();
                    if (baseName.compareToIgnoreCase(depend.getName()) == 0) {
                        fromDependency = true;
                    }
                }
            }

            if (!fromDependency) {
                if (!CUtil.isSystemPath(linkSources[i])) {
                    relPath = CUtil.getRelativePath(basePath, linkSources[i]);
                }
                //
                //   if path has an embedded space then
                //      must quote
                if (relPath.indexOf(' ') > 0) {
                    buf.append('\"');
                    buf.append(CUtil.toWindowsPath(relPath));
                    buf.append('\"');
                } else {
                    buf.append(relPath);
                }
                buf.append(' ');
            }
            }
        }
        if (buf.length() > 0) {
          buf.setLength(buf.length() - 1);
          dependencies = buf.toString();
        }
      return dependencies;

    }

    /**
     * Write Tool element for linker.
     * @param content serialization content handler.
     * @param isDebug true if generating debug configuration.
     * @param dependencies project dependencies.
     * @param basePath path to directory containing project file.
     * @param linkTarget link target.
     * @param targets  all targets.
     * @throws SAXException thrown if error during serialization.
     */
    private void writeLinkerElement(final ContentHandler content,
                                    final boolean isDebug,
                                    final List<DependencyDef> dependencies,
                                    final String basePath,
                                    final TargetInfo linkTarget,
                                    final Map<String, TargetInfo> targets) throws SAXException {
        AttributesImpl attributes = new AttributesImpl();
        addAttribute(attributes, "Name", "VCLinkerTool");

        ProcessorConfiguration config = linkTarget.getConfiguration();
        if (config instanceof CommandLineLinkerConfiguration) {
            CommandLineLinkerConfiguration linkerConfig =
                    (CommandLineLinkerConfiguration) config;
            if (linkerConfig.getLinker() instanceof MsvcCompatibleLinker) {
                addAttribute(attributes, "LinkIncremental",
                        getLinkIncremental(linkerConfig));
                if (isDebug) {
                    addAttribute(attributes, "GenerateDebugInformation", trueLiteral);
                } else {
                    addAttribute(attributes, "GenerateDebugInformation", falseLiteral);
                }
                addAttribute(attributes, "SubSystem",
                        getSubsystem(linkerConfig));
                addAttribute(attributes, "TargetMachine",
                        getTargetMachine(linkerConfig));
            }
        }
        addAttribute(attributes, "AdditionalDependencies",
                getAdditionalDependencies(linkTarget, dependencies, targets, basePath));
        content.startElement(null, "Tool", "Tool", attributes);
        content.endElement(null, "Tool", "Tool");
    }

    /**
     * Writes a project definition file.
     *
     * @param fileName   project name for file, should has .cbx extension
     * @param task       cc task for which to write project
     * @param projectDef project element
     * @param sources    source files
     * @param targets    compilation targets
     * @param linkTarget link target
     * @throws IOException  if I/O error
     * @throws SAXException if XML serialization error
     */
    public void writeProject(final File fileName,
                             final CCTask task,
                             final ProjectDef projectDef,
                             final List<File> sources,
                             final Map<String, TargetInfo> targets,
                             final TargetInfo linkTarget) throws
            IOException,
            SAXException {

        String projectName = projectDef.getName();
        if (projectName == null) {
            projectName = fileName.getName();
        }


        File vcprojFile = new File(fileName + ".vcproj");
        if (!projectDef.getOverwrite() && vcprojFile.exists()) {
            throw new BuildException("Not allowed to overwrite project file "
                    + vcprojFile.toString());
        }
        File slnFile = new File(fileName + ".sln");
        if (!projectDef.getOverwrite() && slnFile.exists()) {
            throw new BuildException("Not allowed to overwrite project file "
                    + slnFile.toString());
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
        XMLSerializer serializer = new XMLSerializer(outStream, format);
        ContentHandler content = serializer.asContentHandler();
        String basePath = fileName.getParentFile().getAbsolutePath();
        content.startDocument();
        
        for(Iterator<CommentDef> iter = projectDef.getComments().iterator(); iter.hasNext(); ) {
			String comment = iter.next().getText();
			serializer.comment(comment);
        }
        
        AttributesImpl emptyAttrs = new AttributesImpl();

        AttributesImpl attributes = new AttributesImpl();
        addAttribute(attributes, "ProjectType", "Visual C++");
        addAttribute(attributes, "Version", version);
        addAttribute(attributes, "Name", projectName);
        content.startElement(null, "VisualStudioProject",
                "VisualStudioProject", attributes);

        content.startElement(null, "Platforms", "Platforms", emptyAttrs);
        attributes.clear();
        addAttribute(attributes, "Name", "Win32");
        content.startElement(null, "Platform", "Platform", attributes);
        content.endElement(null, "Platform", "Platform");
        content.endElement(null, "Platforms", "Platforms");
        content.startElement(null, "Configurations",
                "Configurations", emptyAttrs);

        //
        //   write debug configuration
        //
        writeConfigurationStartTag(content, true, task, compilerConfig);
        writeCompilerElement(content, true, basePath, compilerConfig);
        writeLinkerElement(content, true, projectDef.getDependencies(), basePath, linkTarget, targets);
        content.endElement(null, "Configuration", "Configuration");

        //
        //    write release configuration
        //
        writeConfigurationStartTag(content, false, task, compilerConfig);
        writeCompilerElement(content, false, basePath, compilerConfig);
        writeLinkerElement(content, false, projectDef.getDependencies(), basePath, linkTarget, targets);
        content.endElement(null, "Configuration", "Configuration");

        content.endElement(null, "Configurations", "Configurations");
        content.startElement(null, "References", "References", emptyAttrs);
        content.endElement(null, "References", "References");
        content.startElement(null, "Files", "Files", emptyAttrs);


        File[] sortedSources = new File[sources.size()];
        sources.toArray(sortedSources);
        Arrays.sort(sortedSources, new Comparator<File>() {
            public int compare(final File o1, final File o2) {
                return o1.getName().compareTo(o2.getName());
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
     *
     * @param name          name of filter
     * @param filter        file extensions
     * @param basePath      base path for files
     * @param sortedSources array of source files
     * @param content       generated project
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


        for (int i = 0; i < sortedSources.length; i++) {
            if (isGroupMember(filter, sortedSources[i])) {
                String relativePath = CUtil.getRelativePath(basePath,
                        sortedSources[i]);
                fileAttrs.setValue(0, relativePath);
                content.startElement(null, "File", "File", fileAttrs);
                content.endElement(null, "File", "File");
            }
        }
        content.endElement(null, "Filter", "Filter");

    }

    /**
     * Returns true if the file has an extension that
     *   appears in the group filter.
     *
     * @param filter    String group filter
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
     * Adds an non-namespace-qualified attribute to attribute list.
     * @param attributes list of attributes.
     * @param attrName attribute name, may not be null.
     * @param attrValue attribute value, if null attribute is not added.
     */
    private static void addAttribute(final AttributesImpl attributes,
                                     final String attrName,
                                     final String attrValue) {
        if (attrName == null) {
            throw new IllegalArgumentException("attrName");
        }
        if (attrValue != null) {
            attributes.addAttribute(null, attrName, attrName,
                    "#PCDATA", attrValue);
        }
    }

    /**
     * Gets the first recognized compiler from the
     * compilation targets.
     * @param targets compilation targets
     * @return representative (hopefully) compiler configuration
     */
    private CommandLineCompilerConfiguration
            getBaseCompilerConfiguration(final Map<String, TargetInfo> targets) {
        //
        //   get the first target and assume that it is representative
        //
        Iterator<TargetInfo> targetIter = targets.values().iterator();
        while (targetIter.hasNext()) {
            TargetInfo targetInfo = targetIter.next();
            ProcessorConfiguration config = targetInfo.getConfiguration();
            //
            //   for the first cl compiler
            //
            if (config instanceof CommandLineCompilerConfiguration) {
                CommandLineCompilerConfiguration compilerConfig =
                        (CommandLineCompilerConfiguration) config;
                if (compilerConfig.getCompiler()
                        instanceof MsvcCCompiler) {
                    return compilerConfig;
                }
            }
        }
        return null;
  }
}

