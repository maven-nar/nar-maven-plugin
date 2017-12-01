package com.github.maven_nar;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Runs 'make test' to compile/link the tests and copy them in the right
 * directory where NAR will run them
 * 
 * @goal nar-make-test
 * @requiresProject
 * @author Jeremy Nguyen-Xuan
 */
public class NarMakeCompileTestsMojo extends AbstractGnuMojo {
	/**
	 * Makefile name
	 * 
	 * @parameter expression="${makefile}"
	 * @readonly
	 */
	private String makefile;

	/**
	 * Skip tests
	 * 
	 * @parameter expression="${maven.test.skip}"
	 * @readonly
	 */
	private boolean skiptests;

	public final void narExecute() throws MojoExecutionException, MojoFailureException {
		boolean targetExists = true;
		try {
			NarMakeMakefileCall.runMake("test -n", getBasedir(), makefile, getLog());
		} catch (MojoExecutionException ex) {
			targetExists = false;
		}
		
		if (skiptests || !targetExists) {
			getLog().info("Skipping tests compilation...");
		} else {
			NarMakeMakefileCall.runMake("test", getBasedir(), makefile, getLog());
		}
	}

}
