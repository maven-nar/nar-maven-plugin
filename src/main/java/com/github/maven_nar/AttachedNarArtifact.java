package com.github.maven_nar;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * NarArtifact with its own type, classifier and artifactHandler.
 * 
 * @author Mark Donszelmann
 */
public class AttachedNarArtifact
    extends DefaultArtifact
{
    public AttachedNarArtifact( String groupId, String artifactId, String version, String scope, String type,
                                String classifier, boolean optional, File file )
        throws InvalidVersionSpecificationException
    {
        super( groupId, artifactId, VersionRange.createFromVersionSpec( version ), scope, type, classifier, null,
               optional );
        setArtifactHandler( new Handler( classifier ) );
        setFile(new File(file.getParentFile(), artifactId+"-"+VersionRange.createFromVersionSpec( version )+"-"+classifier+"."+type));
    }

    // NOTE: not used
    public AttachedNarArtifact( Artifact parent, String type, String classifier )
    {
        super( parent.getGroupId(), parent.getArtifactId(), parent.getVersionRange(), parent.getScope(), type,
               classifier, null, parent.isOptional() );
        setArtifactHandler( new Handler( classifier ) );
    }

    private class Handler
        implements ArtifactHandler
    {
        private String classifier;

        Handler( String classifier )
        {
            this.classifier = classifier;
        }

        public String getExtension()
        {
            return "nar";
        }

        public String getDirectory()
        {
            return "nars";
        }

        public String getClassifier()
        {
            return classifier;
        }

        public String getPackaging()
        {
            return "nar";
        }

        public boolean isIncludesDependencies()
        {
            return false;
        }

        public String getLanguage()
        {
            return "native";
        }

        public boolean isAddedToClasspath()
        {
            return true;
        }
    }
}
