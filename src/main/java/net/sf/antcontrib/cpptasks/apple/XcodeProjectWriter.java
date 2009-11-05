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
package net.sf.antcontrib.cpptasks.apple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;

import org.apache.tools.ant.BuildException;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.TargetInfo;
import net.sf.antcontrib.cpptasks.compiler.CommandLineCompilerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.CommandLineLinkerConfiguration;
import net.sf.antcontrib.cpptasks.compiler.ProcessorConfiguration;
import net.sf.antcontrib.cpptasks.gcc.GccCCompiler;
import net.sf.antcontrib.cpptasks.ide.ProjectDef;
import net.sf.antcontrib.cpptasks.ide.ProjectWriter;

/**
 * Writes a Apple Xcode project directory.
 *
 * Status: Collects file list but does not pick up libraries and settings from
 * project.
 *
 * @author curta
 */
public final class XcodeProjectWriter
    implements ProjectWriter {

  /**
   * Next object identifier for project file.
   */
  private int nextID = 0x08FB7796;

  /**
   * Constructor.
   *
   */
  public XcodeProjectWriter() {
  }

  /**
   * Get next unique ID.
   * @return unique ID
   */
  private String getUniqueID() {
    return Integer.toString(nextID++, 16) + "FE84155DC02AAC07";
  }

  /**
   * Writes a project definition file.
   *
   * @param fileName
   *            File name base, writer may append appropriate extension
   * @param task
   *            cc task for which to write project
   * @param projectDef
   *            project element
   * @param targets
   *            compilation targets
   * @param linkTarget
   *            link target
   * @throws IOException
   *             if error writing project file
   */
  public void writeProject(final File fileName, 
  		                   final CCTask task,
                           final ProjectDef projectDef,
						   final List sources,
						   final Hashtable targets,
                           final TargetInfo linkTarget) throws IOException {

    File xcodeDir = new File(fileName + ".xcode");
    if (!projectDef.getOverwrite() && xcodeDir.exists()) {
      throw new BuildException("Not allowed to overwrite project file "
                               + xcodeDir.toString());
    }

    CommandLineCompilerConfiguration compilerConfig =
        getBaseCompilerConfiguration(targets);
    if (compilerConfig == null) {
      throw new BuildException(
          "Unable to find compilation target using GNU C++ compiler");
    }

    String projectName = projectDef.getName();
    if (projectName == null) {
    	projectName = fileName.getName();
    }
    final String basePath = fileName.getAbsoluteFile().getParent();

    xcodeDir.mkdir();

    File xcodeProj = new File(xcodeDir, "project.pbxproj");

    //
    //   assume that all C++ compiles can use the
    //    same settings
    //
    CommandLineCompilerConfiguration gccConfig = null;

    Vector sourceList = new Vector(targets.size());
    Iterator targetIter = targets.values().iterator();
    while (targetIter.hasNext()) {
      TargetInfo info = (TargetInfo) targetIter.next();
      File[] targetsources = info.getSources();
      for (int i = 0; i < targetsources.length; i++) {
        sourceList.addElement(new SourceEntry(targetsources[i],
                                              getUniqueID(), getUniqueID()));
      }
      ProcessorConfiguration procConfig = info.getConfiguration();
      if (procConfig instanceof CommandLineCompilerConfiguration
          && gccConfig == null) {
        gccConfig = (CommandLineCompilerConfiguration) procConfig;
      }
    }
    SourceEntry[] sortedSources = new SourceEntry[sourceList.size()];
    sourceList.copyInto(sortedSources);
    Arrays.sort(sortedSources, new Comparator() {
      public int compare(final Object o1, final Object o2) {
        return ( (SourceEntry) o1).getFile().getName().compareTo( ( (
            SourceEntry) o2).getFile().getName());
      }
    });
    File outFile = task.getOutfile();

    Writer writer = new BufferedWriter(new FileWriter(xcodeProj));

    writer.write("// !$*UTF8*$!\n");
    writer.write("   {\n");
    writer.write("   	archiveVersion = 1;\n");
    writer.write("   	classes = {\n");
    writer.write("   	};\n");
    writer.write("   	objectVersion = 39;\n");
    writer.write("   	objects = {\n");
    writer.write("   		014CEA520018CE5811CA2923 = {\n");
    writer.write("   			buildRules = (\n");
    writer.write("   			);\n");
    writer.write("   			buildSettings = {\n");
    writer.write("   				COPY_PHASE_STRIP = NO;\n");
    writer.write("   				DEBUGGING_SYMBOLS = YES;\n");
    writer.write("   				GCC_DYNAMIC_NO_PIC = NO;\n");
    writer.write("   				GCC_ENABLE_FIX_AND_CONTINUE = YES;\n");
    writer.write("   				GCC_GENERATE_DEBUGGING_SYMBOLS = YES;\n");
    writer.write("   				GCC_OPTIMIZATION_LEVEL = 0;\n");
    writer.write("   				OPTIMIZATION_CFLAGS = \"-O0\";\n");
    writer.write("   				ZERO_LINK = YES;\n");
    writer.write("   			};\n");
    writer.write("   			isa = PBXBuildStyle;\n");
    writer.write("   			name = Development;\n");
    writer.write("   		};\n");
    writer.write("   		014CEA530018CE5811CA2923 = {\n");
    writer.write("   			buildRules = (\n");
    writer.write("   			);\n");
    writer.write("   			buildSettings = {\n");
    writer.write("   				COPY_PHASE_STRIP = YES;\n");
    writer.write("   				GCC_ENABLE_FIX_AND_CONTINUE = NO;\n");
    writer.write("   				ZERO_LINK = NO;\n");
    writer.write("   			};\n");
    writer.write("   			isa = PBXBuildStyle;\n");
    writer.write("   			name = Deployment;\n");
    writer.write("   		};\n");
    writer.write("//   010\n");
    writer.write("//   011\n");
    writer.write("//   012\n");
    writer.write("//   013\n");
    writer.write("//   014\n");
    writer.write("//   080\n");
    writer.write("//   081\n");
    writer.write("//   082\n");
    writer.write("//   083\n");
    writer.write("//   084\n");
    writer.write("   		08FB7793FE84155DC02AAC07 = {\n");
    writer.write("   			buildSettings = {\n");
    writer.write("   			};\n");
    writer.write("   			buildStyles = (\n");
    writer.write("   				014CEA520018CE5811CA2923,\n");
    writer.write("   				014CEA530018CE5811CA2923,\n");
    writer.write("   			);\n");
    writer.write("   			hasScannedForEncodings = 1;\n");
    writer.write("   			isa = PBXProject;\n");
    writer.write("   			mainGroup = 08FB7794FE84155DC02AAC07;\n");
    writer.write("   			projectDirPath = \"\";\n");
    writer.write("   			targets = (\n");
    writer.write("   				D2AAC0620554660B00DB518D,\n");
    writer.write("   			);\n");
    writer.write("   		};\n");
    writer.write("   		08FB7794FE84155DC02AAC07 = {\n");
    writer.write("   			children = (\n");
    writer.write("   				08FB7795FE84155DC02AAC07,\n");
    writer.write("   				1AB674ADFE9D54B511CA2CBB,\n");
    writer.write("   			);\n");
    writer.write("   			isa = PBXGroup;\n");
    writer.write("   			name = ");
    writer.write(outFile.getName());
    writer.write(";\n");
    writer.write("   			refType = 4;\n");
    writer.write("   			sourceTree = \"<group>\";\n");
    writer.write("   		};\n");
    writer.write("   		08FB7795FE84155DC02AAC07 = {\n");
    writer.write("   			children = (\n");

    //
    //   source ID's go here
    //
    for (int i = 0; i < sortedSources.length; i++) {
      writer.write("   				");
      writer.write(sortedSources[i].getSourceID());
      writer.write(",\n");
    }

    writer.write("   			);\n");
    writer.write("   			isa = PBXGroup;\n");
    writer.write("   			name = Source;\n");
    writer.write("   			refType = 4;\n");
    writer.write("   			sourceTree = \"<group>\";\n");
    writer.write("   		};\n");

    for (int i = 0; i < sortedSources.length; i++) {
      //
      //   source definition
      //
      SourceEntry entry = sortedSources[i];
      writer.write("   		");
      writer.write(entry.getSourceID());
      writer.write(" = {\n");
      writer.write("   			fileEncoding = 4;\n");
      writer.write("   			isa = PBXFileReference;\n");
      String sourceName = entry.getFile().getName();
      if (sourceName.endsWith(".c")) {
        writer.write("   			lastKnownFileType = sourcecode.c.c;\n");
      } else {
        writer.write("   			lastKnownFileType = sourcecode.cpp.cpp;\n");
      }

      String relativePath = CUtil.getRelativePath(basePath, entry.getFile());
      if (!relativePath.equals(sourceName)) {
        writer.write("              name = ");
        writer.write(sourceName);
        writer.write(";\n");
      }
      writer.write("   			path = ");
      writer.write(relativePath);
      writer.write(";\n");
      writer.write("   			refType = 4;\n");
      writer.write("   			sourceTree = \"<group>\";\n");
      writer.write("   		};\n");

      //
      //   build definition
      //
      writer.write("   		");
      writer.write(entry.getBuildID());
      writer.write(" = {\n");
      writer.write("   			fileRef = ");
      writer.write(entry.getSourceID());
      writer.write(";\n");
      writer.write("   			isa = PBXBuildFile;\n");
      writer.write("   			settings = {\n");
      writer.write("   			};\n");
      writer.write("   		};\n");

    }
    writer.write("//   080\n");
    writer.write("//   081\n");
    writer.write("//   082\n");
    writer.write("//   083\n");
    writer.write("//   084\n");
    writer.write("//   1A0\n");
    writer.write("//   1A1\n");
    writer.write("//   1A2\n");
    writer.write("//   1A3\n");
    writer.write("//   1A4\n");
    writer.write("   		1AB674ADFE9D54B511CA2CBB = {\n");
    writer.write("   			children = (\n");
    writer.write("   				D2AAC0630554660B00DB518D,\n");
    writer.write("   			);\n");
    writer.write("   			isa = PBXGroup;\n");
    writer.write("   			name = Products;\n");
    writer.write("   			refType = 4;\n");
    writer.write("   			sourceTree = \"<group>\";\n");
    writer.write("   		};\n");
    writer.write("//   1A0\n");
    writer.write("//   1A1\n");
    writer.write("//   1A2\n");
    writer.write("//   1A3\n");
    writer.write("//   1A4\n");
    writer.write("//   D20\n");
    writer.write("//   D21\n");
    writer.write("//   D22\n");
    writer.write("//   D23\n");
    writer.write("//   D24\n");
    writer.write("   		D2AAC0600554660B00DB518D = {\n");
    writer.write("   			buildActionMask = 2147483647;\n");
    writer.write("   			files = (\n");
    writer.write("   			);\n");
    writer.write("   			isa = PBXHeadersBuildPhase;\n");
    writer.write("   			runOnlyForDeploymentPostprocessing = 0;\n");
    writer.write("   		};\n");
    writer.write("   		D2AAC0610554660B00DB518D = {\n");
    writer.write("   			buildActionMask = 2147483647;\n");
    writer.write("   			files = (\n");

    //
    //   build ID's
    //
    for (int i = 0; i < sortedSources.length; i++) {
      writer.write("   				");
      writer.write(sortedSources[i].getBuildID());
      writer.write(",\n");
    }
    writer.write("   			);\n");
    writer.write("   			isa = PBXSourcesBuildPhase;\n");
    writer.write("   			runOnlyForDeploymentPostprocessing = 0;\n");
    writer.write("   		};\n");
    writer.write("   		D2AAC0620554660B00DB518D = {\n");
    writer.write("   			buildPhases = (\n");
    writer.write("   				D2AAC0600554660B00DB518D,\n");
    writer.write("   				D2AAC0610554660B00DB518D,\n");
    writer.write("   			);\n");
    writer.write("   			buildRules = (\n");
    writer.write("   			);\n");
    writer.write("   			buildSettings = {\n");
    writer.write("   				DYLIB_COMPATIBILITY_VERSION = 1;\n");
    writer.write("   				DYLIB_CURRENT_VERSION = 1;\n");
    //
    //   write preprocessor macros
    //
    if (gccConfig != null) {
      String[] options = gccConfig.getPreArguments();
      boolean hasD = false;
      for (int i = 0; i < options.length; i++) {
        if (options[i].startsWith("-D")) {
          if (!hasD) {
            writer.write("   				GCC_PREPROCESSOR_DEFINITIONS = \"");
            hasD = true;
          } else {
            writer.write(" ");
          }
          writer.write(options[i].substring(2));
        }
      }
      if (hasD) {
        writer.write("\";\n");
      }
    }
    writer.write("   				GCC_WARN_FOUR_CHARACTER_CONSTANTS = NO;\n");
    writer.write("   				GCC_WARN_UNKNOWN_PRAGMAS = NO;\n");
    if (gccConfig != null) {
      File[] includes = gccConfig.getIncludePath();
      if (includes.length > 0) {
        writer.write("   				HEADER_SEARCH_PATHS = \"");
        for (int i = 0; i < includes.length; i++) {
          if (i > 0) {
            writer.write(" ");
          }
          writer.write(CUtil.getRelativePath(basePath, includes[i]));
        }
        writer.write("\";\n");
      }
    }
    
    
    String[] linkerArgs = null;
    ProcessorConfiguration linkConfig = linkTarget.getConfiguration();
    if (linkConfig instanceof CommandLineLinkerConfiguration) {
      linkerArgs = ((CommandLineLinkerConfiguration) linkConfig).getPreArguments();
    }

    
    writer.write("   				INSTALL_PATH = /usr/local/lib;\n");
    if (linkerArgs != null) {
        boolean hasLibPath = false;
        for (int i = 0; i < linkerArgs.length; i++) {
            if (linkerArgs[i].startsWith("-L")) {
                if (!hasLibPath) {
        		   writer.write("   				LIBRARY_SEARCH_PATHS = \"");
        		   hasLibPath = true;
        		} else {
        		   writer.write(" ");
        		}
        		writer.write(linkerArgs[i].substring(2));
            }
        }
        if (hasLibPath) {
            writer.write("\";\n");
        }
    } 
    writer.write("   				LIBRARY_STYLE = DYNAMIC;\n");
    writer.write("   				OTHER_CFLAGS = \"\";\n");
    writer.write("   				OTHER_LDFLAGS = \"");
    if (linkerArgs != null) {
    	 String prepend = "";
    	 for (int i = 0; i < linkerArgs.length; i++) {
    	     if (!linkerArgs[i].startsWith("-L")) {
    	         writer.write(prepend);
    	         writer.write(linkerArgs[i]);
    	         prepend = "  ";
    	     }
    	}
    }
    writer.write("\";\n");
    writer.write("   				OTHER_REZFLAGS = \"\";\n");
    writer.write("   				PRODUCT_NAME = testbsd;\n");
    writer.write("   				SECTORDER_FLAGS = \"\";\n");
    writer.write("   				WARNING_CFLAGS = \"-Wmost\";\n");
    writer.write("   			};\n");
    writer.write("   			dependencies = (\n");
    writer.write("   			);\n");
    writer.write("   			isa = PBXNativeTarget;\n");

    writer.write("   			name = ");
    writer.write(outFile.getName());
    writer.write(";\n");
    writer.write("   			productName = ");
    writer.write(outFile.getName());
    writer.write(";\n");
    writer.write("   			productReference = D2AAC0630554660B00DB518D;\n");

    String productType = "com.apple.product-type.library.dynamic";
    String prefix = "lib";
    String suffix = ".dylib";
    String explicitFileType = "compiled.mach-o.dylib";
    String outType = task.getOuttype();
    if ("executable".equals(outType)) {
      productType = "com.apple.product-type.tool";
      prefix = "";
      suffix = "";
      explicitFileType = "compiled.mach-o.executable";
    } else if ("static".equals(outType)) {
      productType = "com.apple.product-type.library.static";
      suffix = ".a";
      explicitFileType = "archive.ar";
    }
    writer.write("   			productType = \"");
    writer.write(productType);
    writer.write("\";\n");
    writer.write("   		};\n");
    writer.write("   		D2AAC0630554660B00DB518D = {\n");

    writer.write("   			explicitFileType = \"");
    writer.write(explicitFileType);
    writer.write("\";\n");
    writer.write("   			includeInIndex = 0;\n");
    writer.write("   			isa = PBXFileReference;\n");
    writer.write("   			path = ");
    writer.write(outFile.getName());
    writer.write(suffix);
    writer.write(";\n");
    writer.write("   			refType = 3;\n");
    writer.write("   			sourceTree = BUILT_PRODUCTS_DIR;\n");
    writer.write("   		};\n");
    writer.write("   	};\n");
    writer.write("   	rootObject = 08FB7793FE84155DC02AAC07;\n");
    writer.write("   }\n");

    writer.close();
  }

  /**
   * Gets the first recognized compiler from the
   * compilation targets.
   * @param targets compilation targets
   * @return representative (hopefully) compiler configuration
   */
  private CommandLineCompilerConfiguration
      getBaseCompilerConfiguration(Hashtable targets) {
    //
    //   find first target with an GNU C++ compilation
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
        if (compilerConfig.getCompiler() instanceof GccCCompiler) {
          return compilerConfig;
        }
      }
    }
    return null;
  }

  /**
   * Source file with 96-bit source and build ID's.
   */
  private static final class SourceEntry {
    /**
     * Source file.
     */
    private final File file;

    /**
     * Source ID.
     */
    private final String sourceID;

    /**
     * Build step ID.
     */
    private final String buildID;

    /**
     * Constructor.
     * @param fileArg source file
     * @param sourceIDArg source ID
     * @param buildIDArg build step ID
     */
    public SourceEntry(final File fileArg,
                       final String sourceIDArg,
                       final String buildIDArg) {
      file = fileArg;
      sourceID = sourceIDArg;
      buildID = buildIDArg;
    }

    /**
     * Get source file.
     * @return source file
     */
    public File getFile() {
      return file;
    }

    /**
     * Get source ID.
     * @return source ID
     */
    public String getSourceID() {
      return sourceID;
    }

    /**
     * Get build step ID.
     * @return build step ID
     */
    public String getBuildID() {
      return buildID;
    }
  }
}
