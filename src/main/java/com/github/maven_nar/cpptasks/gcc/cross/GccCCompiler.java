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
package com.github.maven_nar.cpptasks.gcc.cross;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.CompilerParam;
import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.compiler.ProgressMonitor;
import com.github.maven_nar.cpptasks.gcc.GccCompatibleCCompiler;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.FortranParser;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Adapter for the GCC C/C++ compiler
 *
 * @author Adam Murdoch
 */
public final class GccCCompiler extends GccCompatibleCCompiler {
  private final static String[] headerExtensions = new String[] {
      ".h", ".hpp", ".inl"
  };
  private final static String[] sourceExtensions = new String[] {
      ".c", /* C */
      ".cc", /* C++ */
      ".cpp", /* C++ */
      ".cxx", /* C++ */
      ".c++", /* C++ */
      ".i", /* preprocessed C */
      ".ii", /* preprocessed C++ */
      ".f", /* FORTRAN */
      ".for", /* FORTRAN */
      ".f90", /* FORTRAN */
      ".m", /* Objective-C */
      ".mm", /* Objected-C++ */
      ".s" /* Assembly */
  };
  private static final GccCCompiler cppInstance = new GccCCompiler("c++", sourceExtensions, headerExtensions, false,
      new GccCCompiler("c++", sourceExtensions, headerExtensions, true, null, false, null), false, null);
  private static final GccCCompiler g77Instance = new GccCCompiler("g77", sourceExtensions, headerExtensions, false,
      new GccCCompiler("g77", sourceExtensions, headerExtensions, true, null, false, null), false, null);
  private static final GccCCompiler gppInstance = new GccCCompiler("g++", sourceExtensions, headerExtensions, false,
      new GccCCompiler("g++", sourceExtensions, headerExtensions, true, null, false, null), false, null);
  private static final GccCCompiler instance = new GccCCompiler("gcc", sourceExtensions, headerExtensions, false,
      new GccCCompiler("gcc", sourceExtensions, headerExtensions, true, null, false, null), false, null);

  /**
   * Gets c++ adapter
   */
  public static GccCCompiler getCppInstance() {
    return cppInstance;
  }

  /**
   * Gets g77 adapter
   */
  public static GccCCompiler getG77Instance() {
    return g77Instance;
  }

  /**
   * Gets gpp adapter
   */
  public static GccCCompiler getGppInstance() {
    return gppInstance;
  }

  /**
   * Gets gcc adapter
   */
  public static GccCCompiler getInstance() {
    return instance;
  }

  private String identifier;
  private File[] includePath;
  private boolean isPICMeaningful = true;

  /**
   * Private constructor. Use GccCCompiler.getInstance() to get singleton
   * instance of this class.
   */
  private GccCCompiler(final String command, final String[] sourceExtensions, final String[] headerExtensions,
      final boolean isLibtool, final GccCCompiler libtoolCompiler, final boolean newEnvironment, final Environment env) {
    super(command, null, sourceExtensions, headerExtensions, isLibtool, libtoolCompiler, newEnvironment, env);
    this.isPICMeaningful = !System.getProperty("os.name").contains("Windows");
  }

  @Override
  public void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
      final boolean exceptions, final LinkType linkType, final Boolean rtti, final OptimizationEnum optimization) {
    super.addImpliedArgs(args, debug, multithreaded, exceptions, linkType, rtti, optimization);
    if (this.isPICMeaningful && linkType.isSharedLibrary()) {
      args.addElement("-fPIC");
    }
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    if (newEnvironment || env != null) {
      return new GccCCompiler(getCommand(), this.getSourceExtensions(), this.getHeaderExtensions(), this.getLibtool(),
          (GccCCompiler) this.getLibtoolCompiler(), newEnvironment, env);
    }
    return this;
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    final GccCCompiler clone = (GccCCompiler) super.clone();
    return clone;
  }

  @Override
  public void compile(final CCTask task, final File outputDir, final String[] sourceFiles, final String[] args,
      final String[] endArgs, final boolean relentless, final CommandLineCompilerConfiguration config,
      final ProgressMonitor monitor) throws BuildException {
    try {
      final GccCCompiler clone = (GccCCompiler) this.clone();
      final CompilerParam param = config.getParam("target");
      if (param != null) {
        clone.setCommand(param.getValue() + "-" + this.getCommand());
      }
      clone.supercompile(task, outputDir, sourceFiles, args, endArgs, relentless, config, monitor);
    } catch (final CloneNotSupportedException e) {
      supercompile(task, outputDir, sourceFiles, args, endArgs, relentless, config, monitor);
    }
  }

  /**
   * Create parser to determine dependencies.
   * 
   * Will create appropriate parser (C++, FORTRAN) based on file extension.
   * 
   */
  @Override
  protected Parser createParser(final File source) {
    if (source != null) {
      final String sourceName = source.getName();
      final int lastDot = sourceName.lastIndexOf('.');
      if (lastDot >= 0 && lastDot + 1 < sourceName.length()) {
        final char afterDot = sourceName.charAt(lastDot + 1);
        if (afterDot == 'f' || afterDot == 'F') {
          return new FortranParser();
        }
      }
    }
    return new CParser();
  }

  @Override
  public File[] getEnvironmentIncludePath() {
    if (this.includePath == null) {
      //
      // construct default include path from machine id and version id
      //
      final String[] defaultInclude = new String[1];
      final String buf = "/lib/" + GccProcessor.getMachine() +
          '/' +
          GccProcessor.getVersion() +
          "/include";
      defaultInclude[0] = buf;
      //
      // read specs file and look for -istart and -idirafter
      //
      final String[] specs = GccProcessor.getSpecs();
      final String[][] optionValues = GccProcessor.parseSpecs(specs, "*cpp:", new String[] {
          "-isystem ", "-idirafter "
      });
      //
      // if no entries were found, then use a default path
      //
      if (optionValues[0].length == 0 && optionValues[1].length == 0) {
        optionValues[0] = new String[] {
            "/usr/local/include", "/usr/include", "/usr/include/win32api"
        };
      }
      //
      // remove mingw entries.
      // For MinGW compiles this will mean the
      // location of the sys includes will be
      // wrong in dependencies.xml
      // but that should have no significant effect
      for (int i = 0; i < optionValues.length; i++) {
        for (int j = 0; j < optionValues[i].length; j++) {
          if (optionValues[i][j].indexOf("mingw") > 0) {
            optionValues[i][j] = null;
          }
        }
      }
      //
      // if cygwin then
      // we have to prepend location of gcc32
      // and .. to start of absolute filenames to
      // have something that will exist in the
      // windows filesystem
      if (GccProcessor.isCygwin()) {
        GccProcessor.convertCygwinFilenames(optionValues[0]);
        GccProcessor.convertCygwinFilenames(optionValues[1]);
        GccProcessor.convertCygwinFilenames(defaultInclude);
      }
      int count = CUtil.checkDirectoryArray(optionValues[0]);
      count += CUtil.checkDirectoryArray(optionValues[1]);
      count += CUtil.checkDirectoryArray(defaultInclude);
      this.includePath = new File[count];
      int index = 0;
      for (final String[] optionValue : optionValues) {
        for (final String anOptionValue : optionValue) {
          if (anOptionValue != null) {
            this.includePath[index++] = new File(anOptionValue);
          }
        }
      }
      for (final String element : defaultInclude) {
        if (element != null) {
          this.includePath[index++] = new File(element);
        }
      }
    }
    return this.includePath.clone();
  }

  @Override
  public String getIdentifier() throws BuildException {
    if (this.identifier == null) {
      StringBuffer buf;
      if (getLibtool()) {
        buf = new StringBuffer("libtool ");
      } else {
        buf = new StringBuffer(" ");
      }
      buf.append(getCommand());
      buf.append(' ');
      buf.append(GccProcessor.getVersion());
      buf.append(' ');
      buf.append(GccProcessor.getMachine());
      this.identifier = buf.toString();
    }
    return this.identifier;
  }

  @Override
  public Linker getLinker(final LinkType linkType) {
    return GccLinker.getInstance().getLinker(linkType);
  }

  @Override
  public int getMaximumCommandLength() {
    return Integer.MAX_VALUE;
  }

  private void supercompile(final CCTask task, final File outputDir, final String[] sourceFiles, final String[] args,
      final String[] endArgs, final boolean relentless, final CommandLineCompilerConfiguration config,
      final ProgressMonitor monitor) throws BuildException {
    super.compile(task, outputDir, sourceFiles, args, endArgs, relentless, config, monitor);
  }
}
