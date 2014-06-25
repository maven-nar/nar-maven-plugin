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
package com.github.maven_nar;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Copies the GNU style source files to a target area, autogens and configures
 * them.
 * 
 * @goal nar-gnu-configure
 * @phase process-sources
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarGnuConfigureMojo extends AbstractGnuMojo {

	/**
	 * If true, we run <code>./configure</code> in the source directory instead of copying the
	 * source code to the <code>target/</code> directory first (this saves disk space but
	 * violates Maven's paradigm of keeping generated files inside the  <code>target/</code>
	 * directory structure.
	 *
	 * @parameter property="nar.gnu.configure.in-place" default-value="false"
	 */
	private boolean gnuConfigureInPlace;

	/**
	 * Skip running of autogen.sh (aka buildconf).
	 * 
	 * @parameter property="nar.gnu.autogen.skip" default-value="false"
	 */
	private boolean gnuAutogenSkip;

	/**
	 * Skip running of configure and therefore also autogen.sh
	 * 
	 * @parameter property="nar.gnu.configure.skip" default-value="false"
	 */
	private boolean gnuConfigureSkip;

	/**
	 * Arguments to pass to GNU configure.
	 * 
	 * @parameter property="nar.gnu.configure.args" default-value=""
	 */
	private String gnuConfigureArgs;

	/**
	 * Arguments to pass to GNU buildconf.
	 * 
	 * @parameter property="nar.gnu.buildconf.args" default-value=""
	 */
	private String gnuBuildconfArgs;

	private static final String AUTOGEN = "autogen.sh";

	private static final String BUILDCONF = "buildconf";

	private static final String CONFIGURE = "configure";

	public final void narExecute() throws MojoExecutionException,
			MojoFailureException {

		if (!useGnu()) {
			return;
		}

		File sourceDir = getGnuSourceDirectory();
		if (sourceDir.exists()) {
			File targetDir;

			if (!gnuConfigureInPlace) {
				targetDir = getGnuAOLSourceDirectory();

				getLog().info("Copying GNU sources");

				try {
					FileUtils.mkdir(targetDir.getPath());
					NarUtil.copyDirectoryStructure(sourceDir,
							targetDir, null, null);
				} catch (IOException e) {
					throw new MojoExecutionException("Failed to copy GNU sources",
						e);
				}

				if (!gnuConfigureSkip && !gnuAutogenSkip) {
					File autogen = new File(targetDir, AUTOGEN);
					File buildconf = new File(targetDir, BUILDCONF);
					if (autogen.exists()) {
						getLog().info("Running GNU " + AUTOGEN);
						runAutogen(autogen, targetDir, null);
					} else if (buildconf.exists()) {
						getLog().info("Running GNU " + BUILDCONF);
						String gnuBuildconfArgsArray[] = null;
						if (gnuBuildconfArgs != null) {
							gnuBuildconfArgsArray = gnuBuildconfArgs.split("\\s");
						}
						runAutogen(buildconf, targetDir, gnuBuildconfArgsArray);
					}
				}
			} else {
				targetDir = sourceDir;
			}

			File configure = new File(targetDir, CONFIGURE);
			if (!gnuConfigureSkip && configure.exists()) {
				getLog().info("Running GNU " + CONFIGURE);

				NarUtil.makeExecutable(configure, getLog());
				String[] args = null;

				// create the array to hold constant and additional args
				if (gnuConfigureArgs != null) {
					String[] a = gnuConfigureArgs.split(" ");
					args = new String[a.length + 2];

					for (int i = 0; i < a.length; i++) {
						args[i + 2] = a[i];
					}
				} else {
					args = new String[2];
				}

				// first 2 args are constant
				args[0] = configure.getAbsolutePath();
				args[1] = "--prefix="
						+ getGnuAOLTargetDirectory().getAbsolutePath();

				File buildDir = getGnuAOLSourceDirectory();
				FileUtils.mkdir(buildDir.getPath());

				getLog().info("args: " + arraysToString(args));
				int result = NarUtil.runCommand("sh", args, buildDir, null,
						getLog());
				if (result != 0) {
					throw new MojoExecutionException("'" + CONFIGURE
							+ "' errorcode: " + result);
				}
			}
		}
	}

	private void runAutogen(final File autogen, final File targetDir,
			final String args[]) throws MojoExecutionException,
			MojoFailureException {
		// fix missing config directory
		final File configDir = new File(targetDir, "config");
		if (!configDir.exists()) {
			configDir.mkdirs();
		}

		NarUtil.makeExecutable(autogen, getLog());
		getLog().debug("running sh ./" + autogen.getName());

		String arguments[] = null;
		if (args != null) {
			arguments = new String[1 + args.length];
			for (int i = 0; i < args.length; ++i) {
				arguments[i + 1] = args[i];
			}
		} else {
			arguments = new String[1];
		}
		arguments[0] = "./" + autogen.getName();

		getLog().info("args: " + arraysToString(arguments));

		final int result = NarUtil.runCommand("sh", arguments, targetDir, null,
				getLog());
		if (result != 0) {
			throw new MojoExecutionException("'" + autogen.getName()
					+ "' errorcode: " + result);
		}
	}

	// JDK 1.4 compatibility
	private static String arraysToString(Object[] a) {
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			b.append(String.valueOf(a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

}
