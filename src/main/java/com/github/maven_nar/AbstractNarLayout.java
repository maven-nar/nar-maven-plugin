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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 * @version $Id$
 */
public abstract class AbstractNarLayout implements NarLayout, NarConstants {
  /**
   * @return
   * @throws MojoExecutionException
   */
  public static NarLayout getLayout(final String layoutName, final Log log) throws MojoExecutionException {
    final String className = layoutName.indexOf('.') < 0 ? NarLayout21.class.getPackage().getName() + "." + layoutName
        : layoutName;
    log.debug("Using " + className);
    Class cls;
    try {
      cls = Class.forName(className);
      final Constructor ctor = cls.getConstructor(Log.class);
      return (NarLayout) ctor.newInstance(log);
    } catch (final ClassNotFoundException e) {
      throw new MojoExecutionException("Cannot find class for layout " + className, e);
    } catch (final InstantiationException e) {
      throw new MojoExecutionException("Cannot instantiate class for layout " + className, e);
    } catch (final IllegalAccessException | SecurityException e) {
      throw new MojoExecutionException("Cannot access class for layout " + className, e);
    } catch (final NoSuchMethodException e) {
      throw new MojoExecutionException("Cannot find ctor(Log) for layout " + className, e);
    } catch (final IllegalArgumentException e) {
      throw new MojoExecutionException("Wrong arguments ctor(Log) for layout " + className, e);
    } catch (final InvocationTargetException e) {
      throw new MojoExecutionException("Cannot invokector(Log) for layout " + className, e);
    }
  }

  private final Log log;

  protected AbstractNarLayout(final Log log) {
    this.log = log;
  }

  protected final void attachNar(final ArchiverManager archiverManager, final MavenProjectHelper projectHelper,
      final MavenProject project, final String classifier, final File dir, final String include)
      throws MojoExecutionException {
    final File narFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + "-"
        + classifier + "." + NarConstants.NAR_EXTENSION);
    if (narFile.exists()) {
      narFile.delete();
    }
    try {
      final Archiver archiver = archiverManager.getArchiver(NarConstants.NAR_ROLE_HINT);
      archiver.addDirectory(dir, new String[] {
        include
      }, null);
      archiver.setDestFile(narFile);
      archiver.createArchive();
    } catch (final NoSuchArchiverException e) {
      throw new MojoExecutionException("NAR: cannot find archiver", e);
    } catch (final ArchiverException | IOException e) {
      throw new MojoExecutionException("NAR: cannot create NAR archive '" + narFile + "'", e);
    }
    projectHelper.attachArtifact(project, NarConstants.NAR_TYPE, classifier, narFile);
  }

  protected Log getLog() {
    return this.log;
  }

  protected void unpackNarAndProcess(final ArchiverManager archiverManager, final File file, final File narLocation,
      final String os, final String linkerName, final AOL defaultAOL, final boolean skipRanlib)
      throws MojoExecutionException, MojoFailureException {

    final String gpp = "g++";
    final String gcc = "gcc";

    narLocation.mkdirs();

    // unpack
    try {
      UnArchiver unArchiver;
      unArchiver = archiverManager.getUnArchiver(NarConstants.NAR_ROLE_HINT);
      unArchiver.setSourceFile(file);
      unArchiver.setDestDirectory(narLocation);
      unArchiver.extract();
    } catch (final NoSuchArchiverException | ArchiverException e) {
      throw new MojoExecutionException("Error unpacking file: " + file + " to: " + narLocation, e);
    }

    // process
    if (!NarUtil.getOS(os).equals(OS.WINDOWS)) {
      NarUtil.makeExecutable(new File(narLocation, "bin/" + defaultAOL), this.log);
      // FIXME clumsy
      if (defaultAOL.hasLinker(gpp)) {
        NarUtil.makeExecutable(new File(narLocation, "bin/" + NarUtil.replace(gpp, gcc, defaultAOL.toString())),
            this.log);
      }
      // add link to versioned so files
      NarUtil.makeLink(new File(narLocation, "lib/" + defaultAOL), this.log);
    }
    if (linkerName.equals(gcc) || linkerName.equals(gpp)) {
      if (!skipRanlib) {
        NarUtil.runRanlib(new File(narLocation, "lib/" + defaultAOL), this.log);
  	  }
      // FIXME clumsy
      if (defaultAOL.hasLinker(gpp)) {
        if (!skipRanlib) {
          NarUtil.runRanlib(new File(narLocation, "lib/" + NarUtil.replace(gpp, gcc, defaultAOL.toString())), this.log);
    	}
      }
    }
    // TODO: Find replacement action to install name tool
    // install name tool adjusts the internal lookup directory for the
    // libraries,
    // this isn't really appropriate, removing signatures for one.
    // however don't have a replacement action currently... having commented
    // this,
    // may break some usage, perhaps if don't find solution make configurable.
    // if ( NarUtil.getOS( os ).equals( OS.MACOSX ) )
    // {
    // File[] dylibDirs = new File[2];
    // dylibDirs[0] = new File( narLocation, "lib/" + defaultAOL + "/" +
    // Library.SHARED );
    // dylibDirs[1] = new File( narLocation, "lib/" + defaultAOL + "/" +
    // Library.JNI );
    //
    // NarUtil.runInstallNameTool( dylibDirs, log );
    // }
  }

}
