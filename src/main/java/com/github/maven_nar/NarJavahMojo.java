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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.toolchain.ToolchainManager;

/**
 * Compiles class files into c/c++ headers using "javah". Any class file that contains methods that were declared
 * "native" will be run through javah.
 * 
 * @goal nar-javah
 * @phase compile
 * @requiresSession
 * @requiresDependencyResolution compile
 * @author Mark Donszelmann
 */
public class NarJavahMojo
    extends AbstractNarMojo
{
    /**
     * @component
     */
    private ToolchainManager toolchainManager;

    /**
     * The current build session instance.
     * 
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;
    
    protected final ToolchainManager getToolchainManager() {
        return toolchainManager;
    }
    
    protected final MavenSession getSession() {
        return session;
    }

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        getJavah().execute();
    }
}
