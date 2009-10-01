// Copyright 2006, FreeHEP.
package org.freehep.maven.nar;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;

/**
 * 
 * @author duns
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarArtifact.java eda4d0bbde3d 2007/07/03 16:52:10 duns $
 */
public class NarArtifact extends DefaultArtifact {

    private NarInfo narInfo;

    public NarArtifact(Artifact dependency, NarInfo narInfo) {
        super(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersionRange(), 
              dependency.getScope(), dependency.getType(), dependency.getClassifier(), 
              dependency.getArtifactHandler(), dependency.isOptional());
        this.narInfo = narInfo;
    }
    
    public NarInfo getNarInfo() {
        return narInfo;
    }
}
