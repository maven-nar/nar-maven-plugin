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

import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Downloads any dependent NAR files. This includes the noarch and aol type NAR files.
 *
 * @goal nar-download
 * @phase generate-sources
 * @requiresProject
 * @requiresDependencyResolution
 * @author Mark Donszelmann
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarDownloadMojo.java c867ab546be1 2007/07/05 21:26:30 duns $
 */
public class NarDownloadMojo extends AbstractDependencyMojo {

	/**
	 * Artifact resolver, needed to download source jars for inclusion in
	 * classpath.
	 * 
	 * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
	 * @required
	 * @readonly
	 */
	private ArtifactResolver artifactResolver;

	/**
	 * Remote repositories which will be searched for source attachments.
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @required
	 * @readonly
	 */
	private List remoteArtifactRepositories;

	/**
	 * List of classifiers which you want download. Example ppc-MacOSX-g++,
	 * x86-Windows-msvc, i386-Linux-g++.
	 * 
	 * @parameter expression=""
	 */
	private List classifiers;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Using AOL: "+getAOL());
		
		if (shouldSkip()) {
    		getLog().info("***********************************************************************");
    		getLog().info("NAR Plugin SKIPPED, no NAR Libraries will be produced.");
    		getLog().info("***********************************************************************");
    		
    		return;
		}
		
		List narArtifacts = getNarManager().getNarDependencies("compile");
		if (classifiers == null) {
			getNarManager().downloadAttachedNars(narArtifacts, remoteArtifactRepositories,
					artifactResolver, null);
		} else {
			for (Iterator j = classifiers.iterator(); j.hasNext();) {
				getNarManager().downloadAttachedNars(narArtifacts, remoteArtifactRepositories,
						artifactResolver, (String) j.next());
			}
		}
	}
}
