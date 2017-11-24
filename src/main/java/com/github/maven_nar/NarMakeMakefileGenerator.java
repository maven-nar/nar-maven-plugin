package com.github.maven_nar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.maven.project.MavenProject;

/**
 * Buffers the include and lib lists and generates the file Makefile.dep.${CPU}
 * 
 * @author Jeremy Nguyen-Xuan (CERN)
 * 
 */
public class NarMakeMakefileGenerator {
	private String includesList = ""; // -I
	private String libPathsList = ""; // -L
	private String libsList = ""; // -l

	private String testIncludesList = ""; // -I
	private String testLibPathsList = ""; // -L
	private String testLibsList = ""; // -l

	public void addInclude(String include) {
		includesList += "-I" + include + " ";
	}

	public void addLibPath(String libPath) {
		libPathsList += "-L" + libPath + " ";
	}

	public void addLib(String lib) {
		libsList += "-l" + lib + " ";
	}

	public void addTestInclude(String testInclude) {
		testIncludesList += "-I" + testInclude + " ";
	}

	public void addTestLibPath(String libPath) {
		testLibPathsList += "-L" + libPath + " ";
	}

	public void addTestLib(String testLib) {
		testLibsList += "-l" + testLib + " ";
	}

	/**
	 * Generates the Makefile.dep.${CPU} with the compiler and linker options.
	 * 
	 * @throws IOException
	 */
	public void generateMakefile(MavenProject mavenProject) throws IOException {
		String MakefileName = "Makefile.dep" ;

		// Extract the basedir in a variable to keep the generated makefile cleaner
		String projectBaseDir = mavenProject.getBasedir().toString() + "/target/nar/";
		libPathsList = libPathsList.replaceAll(projectBaseDir, "\\$\\(R\\)");
		includesList = includesList.replaceAll(projectBaseDir, "\\$\\(R\\)");

		// Generates the Makefile
		Writer output = null;
		File file = new File(mavenProject.getBasedir().toString() + "/" + MakefileName);
		output = new BufferedWriter(new FileWriter(file));

		// Output the version for backward compatibility with the Makefiles for /acc/local
		// VERSION = X.Y.Z
		output.write("VERSION = " + mavenProject.getVersion());

		doubleNewlines(output);

		output.write("R = " + projectBaseDir);

		doubleNewlines(output);

		output.write("DEPENDENT_COMPILER_OPTIONS += ");
		output.write(includesList);

		doubleNewlines(output);

		output.write("DEPENDENT_LINKER_OPTIONS += ");
		output.write(libPathsList);
		output.write(libsList);

		// Unit Test dependencies
		String testProjectBaseDir = mavenProject.getBasedir().toString() + "/target/test-nar/";

		doubleNewlines(output);
		output.write("TR = " + testProjectBaseDir);
		doubleNewlines(output);

		// Points to the test-nar folder
		testLibPathsList = testLibPathsList.replaceAll(projectBaseDir, "\\$\\(TR\\)");
		testIncludesList = testIncludesList.replaceAll(projectBaseDir, "\\$\\(TR\\)");

		output.write("ADDITIONAL_TEST_INCLUDES += ");
		output.write(testIncludesList);

		doubleNewlines(output);

		output.write("ADDITIONAL_TEST_LIBS += ");
		output.write(testLibPathsList);
		output.write(testLibsList);

		output.close();
	}

	/**
	 * Prints an empty line
	 * 
	 * @param output
	 * @throws IOException
	 */
	public void newline(Writer output) throws IOException {
		((BufferedWriter) output).newLine();
	}

	/**
	 * Prints two empty lines
	 * 
	 * @param output
	 * @throws IOException
	 */
	public void doubleNewlines(Writer output) throws IOException {
		newline(output);
		newline(output);
	}

}
