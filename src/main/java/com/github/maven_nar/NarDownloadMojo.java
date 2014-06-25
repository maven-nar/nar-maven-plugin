package com.github.maven_nar;


/**
 * Downloads and unpacks any dependent NAR files. This includes the noarch and aol type NAR files.
 * 
 * @goal nar-download
 * @phase process-sources
 * @requiresProject
 * @requiresDependencyResolution compile
 * @author Mark Donszelmann
 */
public class NarDownloadMojo
    extends AbstractDependencyMojo
{
	
}
