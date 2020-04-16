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
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils;

/**
 * Initial layout which expands a nar file into:
 *
 * <pre>
 * nar/includue
 * nar/bin
 * nar/lib
 * </pre>
 *
 * this layout was abandoned because there is no one-to-one relation between the
 * nar file and its directory structure.
 * Therefore SNAPSHOTS could not be fully deleted when replaced.
 *
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarLayout20 extends AbstractNarLayout {
  private final NarFileLayout fileLayout;

  public NarLayout20(final Log log) {
    super(log);
    this.fileLayout = new NarFileLayout10();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#attachNars(java.io.File,
   * org.apache.maven.project.MavenProjectHelper,
   * org.apache.maven.project.MavenProject, com.github.maven_nar.NarInfo)
   */
  @Override
  public final void attachNars(final File baseDir, final ArchiverManager archiverManager,
      final MavenProjectHelper projectHelper, final MavenProject project) throws MojoExecutionException {
    if (getIncludeDirectory(baseDir, project.getArtifactId(), project.getVersion()).exists()) {
      attachNar(archiverManager, projectHelper, project, "noarch", baseDir, "include/**");
    }

    final String[] binAOL = new File(baseDir, "bin").list();
    for (int i = 0; binAOL != null && i < binAOL.length; i++) {
      attachNar(archiverManager, projectHelper, project, binAOL[i] + "-" + Library.EXECUTABLE, baseDir, "bin/"
          + binAOL[i] + "/**");
    }

    final File libDir = new File(baseDir, "lib");
    final String[] libAOL = libDir.list();
    for (int i = 0; libAOL != null && i < libAOL.length; i++) {
      final String bindingType = null;
      final String[] libType = new File(libDir, libAOL[i]).list();
      for (int j = 0; libType != null && j < libType.length; j++) {
        attachNar(archiverManager, projectHelper, project, libAOL[i] + "-" + libType[j], baseDir, "lib/" + libAOL[i]
            + "/" + libType[j] + "/**");
      }

    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#getBinDirectory(java.io.File,
   * java.lang.String)
   */
  @Override
  public final File
      getBinDirectory(final File baseDir, final String artifactId, final String version, final String aol) {
    final File dir = new File(baseDir, this.fileLayout.getBinDirectory(aol));
    return dir;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#getIncludeDirectory(java.io.File)
   */
  @Override
  public final File getIncludeDirectory(final File baseDir, final String artifactId, final String version) {
    return new File(baseDir, this.fileLayout.getIncludeDirectory());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#getLibDir(java.io.File,
   * com.github.maven_nar.AOL, String type)
   */
  @Override
  public final File getLibDirectory(final File baseDir, final String artifactId, final String version,
      final String aol, final String type) throws MojoFailureException {
    if (type.equals(Library.EXECUTABLE)) {
      throw new MojoFailureException("INTERNAL ERROR, Replace call to getLibDirectory with getBinDirectory");
    }

    final File dir = new File(baseDir, this.fileLayout.getLibDirectory(aol, type));
    return dir;
  }

  /*
* (non-Javadoc)
*
* @see com.github.maven_nar.NarLayout#getNarInfoDirectory(java.io.File,
 * java.lang.String
 * java.lang.String
 * java.lang.String
 * com.github.maven_nar.AOL,
 * java.lang.String)
*/
  @Override
  public final File getNarInfoDirectory(final File baseDir, final String groupId, final String artifactId, final String version,
                                         final String aol, final String type) throws MojoExecutionException {

  // This functionality is not supported for older layouts, return an empty file to be passive.
    getLog().debug("NarLayout20 doesn't support writing NarInfo to project classifier directories,use NarLayout21 instead.");
    return new File("");
  }

  @Override
  public File getNarUnpackDirectory(final File baseUnpackDirectory, final File narFile) {
    final File dir = new File(baseUnpackDirectory, FileUtils.basename(narFile.getPath(), "."
        + NarConstants.NAR_EXTENSION));
    return dir;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#getNoArchDirectory(java.io.File)
   */
  @Override
  public File getNoArchDirectory(final File baseDir, final String artifactId, final String version)
      throws MojoExecutionException, MojoFailureException {
    return baseDir;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#attachNars(java.io.File,
   * org.apache.maven.project.MavenProjectHelper,
   * org.apache.maven.project.MavenProject, com.github.maven_nar.NarInfo)
   */
  @Override
  public final void prepareNarInfo(final File baseDir, final MavenProject project, final NarInfo narInfo,
      final AbstractCompileMojo mojo) throws MojoExecutionException {
    if (getIncludeDirectory(baseDir, project.getArtifactId(), project.getVersion()).exists()) {
      narInfo.setNar(null, "noarch", project.getGroupId() + ":" + project.getArtifactId() + ":" + NarConstants.NAR_TYPE
          + ":" + "noarch");
    }

    final String[] binAOL = new File(baseDir, "bin").list();
    for (int i = 0; binAOL != null && i < binAOL.length; i++) {// TODO: chose
                                                               // not to apply
                                                               // new file
                                                               // naming for
                                                               // outfile in
                                                               // case of
                                                               // backwards
                                                               // compatability,
                                                               // may need to
                                                               // reconsider
      narInfo.setNar(null, Library.EXECUTABLE, project.getGroupId() + ":" + project.getArtifactId() + ":"
          + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + Library.EXECUTABLE);
      narInfo.setBinding(new AOL(binAOL[i]), Library.EXECUTABLE);
      narInfo.setBinding(null, Library.EXECUTABLE);
    }

    final File libDir = new File(baseDir, "lib");
    final String[] libAOL = libDir.list();
    for (int i = 0; libAOL != null && i < libAOL.length; i++) {
      String bindingType = null;
      final String[] libType = new File(libDir, libAOL[i]).list();
      for (int j = 0; libType != null && j < libType.length; j++) {
        narInfo.setNar(null, libType[j], project.getGroupId() + ":" + project.getArtifactId() + ":"
            + NarConstants.NAR_TYPE + ":" + "${aol}" + "-" + libType[j]);

        // set if not set or override if SHARED
        if (bindingType == null || libType[j].equals(Library.SHARED)) {
          bindingType = libType[j];
        }
      }

      final AOL aol = new AOL(libAOL[i]);
      if (mojo.getLibsName() != null) {
        narInfo.setLibs(aol, mojo.getLibsName());
      }
      if (narInfo.getBinding(aol, null) == null) {
        narInfo.setBinding(aol, bindingType != null ? bindingType : Library.NONE);
      }
      if (narInfo.getBinding(null, null) == null) {
        narInfo.setBinding(null, bindingType != null ? bindingType : Library.NONE);
      }
    }
  }

  @Override
  public void unpackNar(final File unpackDir, final ArchiverManager archiverManager, final File file, final String os,
      final String linkerName, final AOL defaultAOL, final boolean skipRanlib) throws MojoExecutionException, MojoFailureException {
    final File flagFile = new File(unpackDir, FileUtils.basename(file.getPath(), "." + NarConstants.NAR_EXTENSION)
        + ".flag");

    boolean process = false;
    if (!unpackDir.exists()) {
      unpackDir.mkdirs();
      process = true;
    } else if (!flagFile.exists()) {
      process = true;
    } else if (file.lastModified() > flagFile.lastModified()) {
      process = true;
    }

    if (process) {
      try {
        unpackNarAndProcess(archiverManager, file, unpackDir, os, linkerName, defaultAOL, skipRanlib);
        FileUtils.fileDelete(flagFile.getPath());
        FileUtils.fileWrite(flagFile.getPath(), "");
      } catch (final IOException e) {
        throw new MojoFailureException("Cannot create flag file: " + flagFile.getPath(), e);
      }
    }
  }
}
