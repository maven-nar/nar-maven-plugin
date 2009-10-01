// Copyright FreeHEP, 2005-2006.
package org.freehep.maven.nar;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/AbstractDependencyMojo.java c867ab546be1 2007/07/05 21:26:30 duns $
 */
public abstract class AbstractDependencyMojo extends AbstractNarMojo {

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    protected ArtifactRepository getLocalRepository() {
        return localRepository;
    }
	
	protected NarManager getNarManager() throws MojoFailureException {
		return new NarManager(getLog(), getLocalRepository(), getMavenProject(), getArchitecture(), getOS(), getLinker());
	}
}
