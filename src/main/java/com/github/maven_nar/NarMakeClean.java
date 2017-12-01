package com.github.maven_nar;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Runs 'make clean'.
 * 
 * @goal nar-make-clean
 * @requiresProject
 * @author Jeremy Nguyen-Xuan (CERN)
 */
public class NarMakeClean extends AbstractGnuMojo {

	/**
	 * Makefile name
	 * 
	 * @parameter expression="${makefile}"
	 * @readonly
	 */
	protected String makefile;

	public final void narExecute() throws MojoExecutionException, MojoFailureException {
		NarMakeMakefileCall.runMake("clean", getBasedir(), makefile, getLog());
	}
}
