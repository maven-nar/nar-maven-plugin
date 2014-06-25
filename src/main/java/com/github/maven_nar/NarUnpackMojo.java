package com.github.maven_nar;

import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Unpacks NAR files. Unpacking happens in the local repository, and also sets flags on binaries and corrects static
 * libraries.
 * 
 * @goal nar-unpack
 * @phase process-sources
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarUnpackMojo
    extends NarDownloadMojo
{

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {

            unpackAttachedNars( getAllAttachedNarArtifacts(getNarArtifacts()));

    }
}
