package com.github.maven_nar;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Runs 'make' to compile.
 * 
 * @goal nar-make-compile
 * @requiresProject
 * @author Mark Donszelmann
 * @author Jeremy Nguyen-Xuan (CERN)
 */
public class NarMakeCompileMojo extends AbstractGnuMojo {

	/**
	 * Makefile name
	 * 
	 * @parameter expression="${makefile}"
	 * @readonly
	 */
	protected String makefile;

	private final File manifest = new File("build/include/manifest");

	public final void narExecute() throws MojoExecutionException, MojoFailureException {
		NarMakeMakefileCall.runMake("", getBasedir(), makefile, getLog());
		if (manifest.exists()) {
			manifest.renameTo(new File(manifest.toString()));
		}
	}
}
