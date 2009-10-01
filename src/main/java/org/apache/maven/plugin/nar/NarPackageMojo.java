package org.apache.maven.plugin.nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * Jars up the NAR files.
 * 
 * @goal nar-package
 * @phase package
 * @requiresProject
 * @author Mark Donszelmann
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarPackageMojo.java 0ee9148b7c6a 2007/09/20 18:42:29 duns $
 */
public class NarPackageMojo extends AbstractCompileMojo {

	/**
	 * Used for attaching the artifact in the project
	 * 
	 * @component
	 */
	private MavenProjectHelper projectHelper;

	private File narDirectory;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (shouldSkip())
			return;

		// FIX for NARPLUGIN-??? where -DupdateReleaseInfo copies to a .nar file
		getMavenProject().getArtifact().setArtifactHandler(
				new NarArtifactHandler());

		narDirectory = new File(getOutputDirectory(), "nar");

		// noarch
		String include = "include";
		if (new File(narDirectory, include).exists()) {
			attachNar("include", null, NAR_NO_ARCH);
		}

		// create nar with binaries
		String bin = "bin";
		String[] binAOLs = new File(narDirectory, bin).list();
		for (int i = 0; i < (binAOLs != null ? binAOLs.length : 0); i++) {
			attachNar(bin + "/" + binAOLs[i], binAOLs[i], bin);
		}

		// create nars for each type of library (static, shared).
		String bindingType = null;
		for (Iterator i = getLibraries().iterator(); i.hasNext();) {
			Library library = (Library) i.next();
			String type = library.getType();
			if (bindingType == null)
				bindingType = type;

			// create nar with libraries
			String lib = "lib";
			String[] libAOLs = new File(narDirectory, lib).list();
			for (int j = 0; j < (libAOLs != null ? libAOLs.length : 0); j++) {
				attachNar(lib + "/" + libAOLs[j] + "/" + type, libAOLs[j], type);
			}
		}

		// override binding if not set
		if (getNarInfo().getBinding(null, null) == null) {
			getNarInfo().setBinding(null, bindingType != null ? bindingType
					: Library.NONE);
		}

		try {
			File propertiesDir = new File(getOutputDirectory(), "classes/META-INF/nar/"
					+ getMavenProject().getGroupId() + "/" + getMavenProject().getArtifactId());
			if (!propertiesDir.exists()) {
				propertiesDir.mkdirs();
			}
			File propertiesFile = new File(propertiesDir, NarInfo.NAR_PROPERTIES);
			getNarInfo().writeToFile(propertiesFile);
		} catch (IOException ioe) {
			throw new MojoExecutionException(
					"Cannot write nar properties file", ioe);
		}
	}

	private void attachNar(String dir, String aol, String type)
			throws MojoExecutionException {
		File libFile = new File(getOutputDirectory(), getFinalName() + "-"
				+ (aol != null ? aol + "-" : "") + type + "." + NAR_EXTENSION);
		nar(libFile, narDirectory, new String[] { dir });
		projectHelper.attachArtifact(getMavenProject(), NAR_TYPE,
				(aol != null ? aol + "-" : "") + type, libFile);
		getNarInfo().setNar(null, type, getMavenProject().getGroupId() + ":"
				+ getMavenProject().getArtifactId() + ":" + NAR_TYPE + ":"
				+ (aol != null ? "${aol}-" : "") + type);

	}

	private void nar(File nar, File dir, String[] dirs)
			throws MojoExecutionException {
		try {
			if (nar.exists()) {
				nar.delete();
			}

			Archiver archiver = new ZipArchiver();
			// seems to return same archiver all the time
			// archiverManager.getArchiver(NAR_ROLE_HINT);
			for (int i = 0; i < dirs.length; i++) {
				String[] includes = new String[] { dirs[i] + "/**" };
				archiver.addDirectory(dir, includes, null);
			}
			archiver.setDestFile(nar);
			archiver.createArchive();
		} catch (ArchiverException e) {
			throw new MojoExecutionException(
					"Error while creating NAR archive.", e);
			// } catch (NoSuchArchiverException e) {
			// throw new MojoExecutionException("Error while creating NAR
			// archive.", e );
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Error while creating NAR archive.", e);
		}
	}

}
