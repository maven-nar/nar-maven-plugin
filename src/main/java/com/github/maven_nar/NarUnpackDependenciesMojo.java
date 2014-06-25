package com.github.maven_nar;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Goal that unpacks the project dependencies from the repository to a defined location.
 * Unpacking happens in the local repository, and also sets flags on binaries and corrects static libraries.
 * 
 * @goal nar-unpack-dependencies
 * @phase process-test-sources
 * @requiresProject
 * @requiresDependencyResolution test
 * @author Mark Donszelmann
 */
public class NarUnpackDependenciesMojo
    extends NarDownloadDependenciesMojo
{
	// get them all
}
