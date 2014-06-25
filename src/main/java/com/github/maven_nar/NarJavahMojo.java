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
