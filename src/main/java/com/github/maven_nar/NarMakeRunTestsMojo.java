package com.github.maven_nar;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Runs 'make test' to compile/link the tests and copy them in the right
 * directory where NAR will run them
 * 
 * @goal nar-make-run-test
 * @requiresProject
 * @author Jeremy Nguyen-Xuan (CERN)
 */
public class NarMakeRunTestsMojo extends AbstractGnuMojo {
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
			NarMakeMakefileCall.runMake("run-test -n", getBasedir(), makefile, getLog());
		} catch (MojoExecutionException ex) {
			targetExists = false;
		}

		if (skiptests || !targetExists) {
			getLog().info("Skipping tests...");
		} else {
			String xmlOutput = getBasedir() + "/target/unit-test";
			NarMakeMakefileCall.runMake("run-test GTEST_XML_OUTPUT=" + xmlOutput, getBasedir(), makefile,
					getLog());
		}
	}

}
