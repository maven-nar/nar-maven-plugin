package com.github.maven_nar;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;

/**
 * @author Mark Donszelmann
 */
public class NarArtifact
    extends DefaultArtifact
{

    private NarInfo narInfo;

    public NarArtifact( Artifact dependency, NarInfo narInfo )
    {
        super( dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersionRange(),
               dependency.getScope(), dependency.getType(), dependency.getClassifier(),
               dependency.getArtifactHandler(), dependency.isOptional() );
        this.setFile( dependency.getFile() );
        this.narInfo = narInfo;
    }

    public final NarInfo getNarInfo()
    {
        return narInfo;
    }
    
    public String getBaseFilename() {
        return getArtifactId()+"-"+getBaseVersion()+"-"+getClassifier();
    }
}
