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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.FileUtils;

/**
 * Layout which expands a nar file into:
 *
 * <pre>
 * nar/noarch/include
 * nar/aol/<aol>-<type>/bin
 * nar/aol/<aol>-<type>/lib
 * </pre>
 *
 * This loayout has a one-to-one relation with the aol-type version of the nar.
 *
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarLayout21 extends AbstractNarLayout {
  private final NarFileLayout fileLayout;

  public NarLayout21(final Log log) {
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
    if (getNoArchDirectory(baseDir, project.getArtifactId(), project.getVersion()).exists()) {
      attachNar(archiverManager, projectHelper, project, NarConstants.NAR_NO_ARCH,
          getNoArchDirectory(baseDir, project.getArtifactId(), project.getVersion()), "*/**");
    }

    // list all directories in basedir, scan them for classifiers
    final String[] subDirs = baseDir.list();
    for (int i = 0; subDirs != null && i < subDirs.length; i++) {
      final String artifactIdVersion = project.getArtifactId() + "-" + project.getVersion();

      // skip entries not belonging to this project
      if (!subDirs[i].startsWith(artifactIdVersion)) {
        continue;
      }

      final String classifier = subDirs[i].substring(artifactIdVersion.length() + 1);

      // skip noarch here
      if (classifier.equals(NarConstants.NAR_NO_ARCH)) {
        continue;
      }

      final File dir = new File(baseDir, subDirs[i]);
      attachNar(archiverManager, projectHelper, project, classifier, dir, "*/**");
    }
  }

  private File getAolDirectory(final File baseDir, final String artifactId, final String version, final String aol,
      final String type) {
    return new File(baseDir, artifactId + "-" + version + "-" + aol + "-" + type);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#getLibDir(java.io.File,
   * com.github.maven_nar.AOL,
   * java.lang.String)
   */
  @Override
  public final File
      getBinDirectory(final File baseDir, final String artifactId, final String version, final String aol) {
    File dir = getAolDirectory(baseDir, artifactId, version, aol, Library.EXECUTABLE);
    dir = new File(dir, this.fileLayout.getBinDirectory(aol));
    return dir;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#getIncludeDirectory(java.io.File)
   */
  @Override
  public final File getIncludeDirectory(final File baseDir, final String artifactId, final String version) {
    return new File(getNoArchDirectory(baseDir, artifactId, version), this.fileLayout.getIncludeDirectory());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.maven_nar.NarLayout#getLibDir(java.io.File,
   * com.github.maven_nar.AOL,
   * java.lang.String)
   */
  @Override
  public final File getLibDirectory(final File baseDir, final String artifactId, final String version,
      final String aol, final String type) throws MojoExecutionException {
    if (type.equals(Library.EXECUTABLE)) {
      throw new MojoExecutionException("NAR: for type EXECUTABLE call getBinDirectory instead of getLibDirectory");
    }

    File dir = getAolDirectory(baseDir, artifactId, version, aol, type);
    dir = new File(dir, this.fileLayout.getLibDirectory(aol, type));
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

    File aolDirectory = getAolDirectory(baseDir, artifactId, version, aol, type);
    return new File(aolDirectory, this.fileLayout.getNarInfoFile(groupId, artifactId, type));
  }

  @Override
  public File getNarUnpackDirectory(final File baseUnpackDirectory, final File narFile) {
    final File dir = new File(baseUnpackDirectory, FileUtils.basename(narFile.getPath(), "."
        + NarConstants.NAR_EXTENSION));
    return dir;
  }

  @Override
  public File getNoArchDirectory(final File baseDir, final String artifactId, final String version) {
    return new File(baseDir, artifactId + "-" + version + "-" + NarConstants.NAR_NO_ARCH);
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
      final AbstractCompileMojo mojo) throws MojoExecutionException, MojoFailureException {
    if (getNoArchDirectory(baseDir, project.getArtifactId(), project.getVersion()).exists()) {
      narInfo.setNar(null, NarConstants.NAR_NO_ARCH, project.getGroupId() + ":" + project.getArtifactId() + ":"
          + NarConstants.NAR_TYPE + ":" + NarConstants.NAR_NO_ARCH);
    }

    final String artifactIdVersion = project.getArtifactId() + "-" + project.getVersion();
    // list all directories in basedir, scan them for classifiers
    final String[] subDirs = baseDir.list();
    final ArrayList<String> classifiers = new ArrayList<>();
    for (int i = 0; subDirs != null && i < subDirs.length; i++) {
      // skip entries not belonging to this project
      if (!subDirs[i].startsWith(artifactIdVersion)) {
        continue;
      }

      final String classifier = subDirs[i].substring(artifactIdVersion.length() + 1);

      // skip noarch here
      if (classifier.equals(NarConstants.NAR_NO_ARCH)) {
        continue;
      }

      classifiers.add(classifier);
    }

    if (!classifiers.isEmpty()) {

      for (final String classifier : classifiers) {
        final int lastDash = classifier.lastIndexOf('-');
        final String type = classifier.substring(lastDash + 1);
        final AOL aol = new AOL(classifier.substring(0, lastDash));

        if (narInfo.getOutput(aol, null) == null) {
          narInfo.setOutput(aol, mojo.getOutput(!Library.EXECUTABLE.equals(type)));
        }

        if (mojo.getLibsName() != null) {
          narInfo.setLibs(aol, mojo.getLibsName());
        }
        
        narInfo.setIncludesType(null, mojo.getIncludesType());

        // We prefer shared to jni/executable/static/none,
        if (type.equals(Library.SHARED)) // overwrite whatever we had
        {
          narInfo.setBinding(aol, type);
          narInfo.setBinding(null, type);
        } else {
          // if the binding is already set, then don't write it for
          // jni/executable/none.
		  String current = narInfo.getBinding(aol, null);
          if ( current == null) {
            narInfo.setBinding(aol, type);
          } else if (!Library.SHARED.equals(current)
			  && type.equals(Library.STATIC)) {
            //static lib is preferred over other remaining types; see #231
            narInfo.setBinding(aol, type);
          }

          if (narInfo.getBinding(null, null) == null) {
            narInfo.setBinding(null, type);
          }
        }

        narInfo.setNar(null, type, project.getGroupId() + ":" + project.getArtifactId() + ":" + NarConstants.NAR_TYPE
            + ":" + "${aol}" + "-" + type);
        
        // set the system includes
        Set<String> flattenedSysLibs = new LinkedHashSet<>();
        String sysLibSet = mojo.getLinker().getSysLibSet();
        List<SysLib> sysLibList = mojo.getLinker().getSysLibs();
        if (sysLibList == null) sysLibList = new ArrayList<>();
        
        Set<SysLib> dependencySysLibSet = new HashSet<>();
        // add syslibs from all attached artifacts
        for (NarArtifact artifact : mojo.getNarArtifacts()) {
          dependencySysLibSet.addAll(mojo.getDependecySysLib(artifact.getNarInfo()));
        }
        sysLibList.addAll(dependencySysLibSet);
        
        if (sysLibSet != null) {
          String[] split = sysLibSet.split(",");
          
          for (String s : split) {
            flattenedSysLibs.add(s.trim());
          }
        }
        
        if (sysLibList != null && !sysLibList.isEmpty()) {
          for (SysLib s : sysLibList) {
            flattenedSysLibs.add(s.getName() + ":" + s.getType());
          }
        }
        
        if (!flattenedSysLibs.isEmpty()) {
          Iterator<String> iter = flattenedSysLibs.iterator();
          
          StringBuilder b = new StringBuilder();
          while (iter.hasNext()) {
            b.append(iter.next());
            if (iter.hasNext()) b.append(", ");
          }
          
          getLog().debug("Added syslib to narInfo: " + b.toString());
          narInfo.setSysLibs(aol, b.toString());
        }
      }

      // setting this first stops the per type config because getOutput check
      // for aol defaults to this generic one...
      if (mojo != null && narInfo.getOutput(null, null) == null) {
        narInfo.setOutput(null, mojo.getOutput(true));
      }
    }
  }

  @Override
  public void unpackNar(final File unpackDirectory, final ArchiverManager archiverManager, final File file,
      final String os, final String linkerName, final AOL defaultAOL, final boolean skipRanlib)
      throws MojoExecutionException, MojoFailureException {
    final File dir = getNarUnpackDirectory(unpackDirectory, file);

    boolean process = false;

    if (!unpackDirectory.exists()) {
      unpackDirectory.mkdirs();
      process = true;
    } else if (!dir.exists()) {
      process = true;
    } else if (file.lastModified() > dir.lastModified()) {
      NarUtil.deleteDirectory(dir);
      process = true;
    } else if (dir.list().length == 0) {
      // a previously failed cleanup which failed deleting all may have left a
      // state where dir modified > file modified but not unpacked.
      process = true;
    }

    if (process) {
      unpackNarAndProcess(archiverManager, file, dir, os, linkerName, defaultAOL, skipRanlib);
    }
  }

}
