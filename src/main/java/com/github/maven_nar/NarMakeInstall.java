package com.github.maven_nar;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Runs 'make install' to install.
 * 
 * @goal nar-make-install
 * @requiresProject
 * @author Jeremy Nguyen-Xuan (CERN)
 */
public class NarMakeInstall extends AbstractGnuMojo {
	/**
	 * Makefile name
	 * 
	 * @parameter expression="${makefile}"
	 * @readonly
	 */
	protected String makefile;

	public final void narExecute() throws MojoExecutionException, MojoFailureException {
		NarMakeMakefileCall.runMake("install", getBasedir(), makefile, getLog(), false);
	}
}