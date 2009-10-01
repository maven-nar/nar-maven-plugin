// Copyright FreeHEP, 2007.
package org.freehep.maven.nar;

import org.apache.maven.artifact.handler.ArtifactHandler;

/**
 * 
 * @author Mark Donszelmann
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarArtifactHandler.java 76e8ff7ad2b0 2007/07/24 04:15:54 duns $
 */
public class NarArtifactHandler implements ArtifactHandler {
	public String getPackaging() {
		return "nar";
	}

	public String getClassifier() {
		return null;
	}

	public String getDirectory() {
		return getExtension() + "s";
	}

	public String getExtension() {
		return "jar";
	}

	public String getLanguage() {
		return "java";
	}

	public boolean isAddedToClasspath() {
		return true;
	}

	public boolean isIncludesDependencies() {
		return false;
	}
}
