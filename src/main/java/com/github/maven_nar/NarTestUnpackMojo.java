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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Unpacks NAR files needed for tests compilation and execution. Unpacking happens in the project target folder, and
 * also sets flags on binaries and corrects static libraries.
 * 
 * @author Mark Donszelmann
 */
@Mojo(name = "nar-test-unpack", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class NarTestUnpackMojo
    extends AbstractDependencyMojo
{

	/**
	 * List the dependencies needed for tests compilations and executions.
	 */
	@Override
    protected List<Artifact> getArtifacts() {
		try {
			List<String> scopes = new ArrayList<String>();
    		scopes.add(Artifact.SCOPE_COMPILE);
    		scopes.add(Artifact.SCOPE_PROVIDED);
    		scopes.add(Artifact.SCOPE_RUNTIME);
    		scopes.add(Artifact.SCOPE_SYSTEM);
    		scopes.add(Artifact.SCOPE_TEST);
    		return getNarManager().getDependencies(scopes);
        } catch (MojoExecutionException e) {
            e.printStackTrace();
        } catch (MojoFailureException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
	}

	@Override
    protected File getUnpackDirectory()
    {
        return getTestUnpackDirectory() == null ? super.getUnpackDirectory() : getTestUnpackDirectory();
    }

    @Override
    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
    	List<AttachedNarArtifact> attachedNarArtifacts = getAttachedNarArtifacts();
    	unpackAttachedNars( attachedNarArtifacts );
    }
}
