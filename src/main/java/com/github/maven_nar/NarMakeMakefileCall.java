package com.github.maven_nar;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * Runs make with given arguments and flag MAVEN_BUILD=true
 * 
 * @author Jeremy Nguyen-Xuan (CERN)
 */
public class NarMakeMakefileCall {

	public static void runMake(String makeArgs, File srcDir,  String makefile, Log logger)
			throws MojoExecutionException, MojoFailureException {
		runMake(makeArgs, srcDir, makefile, logger, true);
	}

	public static void runMake(String makeArgs, File srcDir, String makefile, Log logger,
			boolean mavenBuild) throws MojoExecutionException, MojoFailureException {
		String[] args = null;
		String[] env = null;
		String defaultMakefile = "Makefile";


		if (mavenBuild) {
			makeArgs += " MAVEN_BUILD=true";
		}

		// Alternative Makefile
		logger.debug("-Dmakefile property: " + makefile);
		if (makefile != null && !makefile.equals("")) {
			// Check if Makefile exists
			File mk = new File(makefile);
			if (!mk.exists()) {
				throw new MojoExecutionException("Specified Makefile " + makefile + "not found!");
			}
			logger.info("Using makefile: " + makefile);
			makeArgs += " -f " + makefile;
		}

		// splitting args to pass them as an arguments
		if (makeArgs != null) {
			args = makeArgs.split(" ");
		}

		if ((makefile == null || makefile.equals("")) && !(new File(srcDir + "/" + defaultMakefile)).exists()) {
			logger.warn(srcDir + "/" + "Makefile does not exist, Calling Makefile step is skipped!");
		} else {
			logger.info("Running make with arguments: ");
			logger.info(makeArgs);
			int result = NarUtil.runCommand("make", args, srcDir, env, logger);
			if (result != 0) {
				throw new MojoExecutionException("'make' errorcode: " + result);
			}
		}

	}
}
