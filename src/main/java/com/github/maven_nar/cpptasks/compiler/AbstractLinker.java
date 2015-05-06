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
package com.github.maven_nar.cpptasks.compiler;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.TargetDef;
import com.github.maven_nar.cpptasks.TargetMatcher;
import com.github.maven_nar.cpptasks.VersionInfo;

/**
 * An abstract Linker implementation.
 *
 * @author Adam Murdoch
 */
public abstract class AbstractLinker extends AbstractProcessor implements Linker {
  public AbstractLinker(final String[] objExtensions, final String[] ignoredExtensions) {
    super(objExtensions, ignoredExtensions);
  }

  /**
   * Adds source or object files to the bidded fileset to
   * support version information.
   * 
   * @param versionInfo
   *          version information
   * @param linkType
   *          link type
   * @param isDebug
   *          true if debug build
   * @param outputFile
   *          name of generated executable
   * @param objDir
   *          directory for generated files
   * @param matcher
   *          bidded fileset
   */
  @Override
  public void addVersionFiles(final VersionInfo versionInfo, final LinkType linkType, final File outputFile,
      final boolean isDebug, final File objDir, final TargetMatcher matcher) throws IOException {
    if (versionInfo == null) {
      throw new NullPointerException("versionInfo");
    }
    if (linkType == null) {
      throw new NullPointerException("linkType");
    }
    if (outputFile == null) {
      throw new NullPointerException("outputFile");
    }
    if (objDir == null) {
      throw new NullPointerException("objDir");
    }
  }

  /**
   * Returns the bid of the processor for the file.
   * 
   * A linker will bid 1 on any unrecognized file type.
   * 
   * @param inputFile
   *          filename of input file
   * @return bid for the file, 0 indicates no interest, 1 indicates that the
   *         processor recognizes the file but doesn't process it (header
   *         files, for example), 100 indicates strong interest
   */
  @Override
  public int bid(final String inputFile) {
    final int bid = super.bid(inputFile);
    switch (bid) {
    //
    // unrecognized extension, take the file
    //
      case 0:
        return 1;
        //
        // discard the ignored extensions
        //
      case 1:
        return 0;
    }
    return bid;
  }

  @Override
  public Processor changeEnvironment(final boolean newEnvironment, final Environment env) {
    return this;
  }

  abstract protected LinkerConfiguration createConfiguration(CCTask task, LinkType linkType,
      ProcessorDef[] baseConfigs, LinkerDef specificConfig, TargetDef targetPlatform, VersionInfo versionInfo);

  @Override
  public ProcessorConfiguration createConfiguration(final CCTask task, final LinkType linkType,
      final ProcessorDef[] baseConfigs, final ProcessorDef specificConfig, final TargetDef targetPlatform,
      final VersionInfo versionInfo) {
    if (specificConfig == null) {
      throw new NullPointerException("specificConfig");
    }
    return createConfiguration(task, linkType, baseConfigs, (LinkerDef) specificConfig, targetPlatform, versionInfo);
  }

  @Override
  public String getLibraryKey(final File libfile) {
    return libfile.getName();
  }

  @Override
  public abstract String[] getOutputFileNames(String fileName, VersionInfo versionInfo);

}
