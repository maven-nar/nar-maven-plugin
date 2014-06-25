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
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractDependencyMojo extends AbstractNarMojo {

	/**
	 * @parameter default-value="${localRepository}"
	 * @required
	 * @readonly
	 */
	private ArtifactRepository localRepository;

	/**
	 * Artifact resolver, needed to download the attached nar files.
	 * 
	 * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
	 * @required
	 * @readonly
	 */
	protected ArtifactResolver artifactResolver;

	/**
	 * Remote repositories which will be searched for nar attachments.
	 * 
	 * @parameter default-value="${project.remoteArtifactRepositories}"
	 * @required
	 * @readonly
	 */
	protected List remoteArtifactRepositories;

    /**
     * To look up Archiver/UnArchiver implementations
     * 
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    protected ArchiverManager archiverManager;

	/**
     * The plugin remote repositories declared in the pom.
     * 
     * @parameter default-value="${project.pluginArtifactRepositories}"
     * @since 2.2
     */
    // private List remotePluginRepositories;

	protected final ArtifactRepository getLocalRepository() {
		return localRepository;
	}

	protected final List/*<ArtifactRepository>*/ getRemoteRepositories() {
		return remoteArtifactRepositories;
	}

	protected final ArchiverManager getArchiverManager() {
		return archiverManager;
	}

	protected final NarManager getNarManager() throws MojoFailureException,
			MojoExecutionException {
		return new NarManager(getLog(), getLocalRepository(),
				getMavenProject(), getArchitecture(), getOS(), getLinker());
	}

    protected List<Artifact> getArtifacts() {
        try {
            return getNarManager().getNarDependencies(Artifact.SCOPE_COMPILE);
        } catch (MojoExecutionException e) {
            e.printStackTrace();
        } catch (MojoFailureException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

	/**
	 * Returns dependencies which are dependent on NAR files (i.e. contain
	 * NarInfo)
	 */
	public final List<NarArtifact> getNarArtifacts()
			throws MojoExecutionException {
		List<NarArtifact> narDependencies = new LinkedList<NarArtifact>();
		for (Iterator i = getArtifacts().iterator(); i.hasNext();) {
			Artifact dependency = (Artifact) i.next();
			getLog().debug("Examining artifact for NarInfo: " + dependency);

			NarInfo narInfo = getNarInfo(dependency);
			if (narInfo != null) {
				getLog().debug("    - added as NarDependency");
				narDependencies.add(new NarArtifact(dependency, narInfo));
			}
		}
		getLog().debug(
				"Dependencies contained " + narDependencies.size()
						+ " NAR artifacts.");
		return narDependencies;
	}

	public final NarInfo getNarInfo(Artifact dependency)
			throws MojoExecutionException {
		// FIXME reported to maven developer list, isSnapshot changes behaviour
		// of getBaseVersion, called in pathOf.
		dependency.isSnapshot();

        if (dependency.getFile().isDirectory()) {
            getLog().debug("Dependency is not packaged: " + dependency.getFile());

            return new NarInfo(dependency.getGroupId(), dependency.getArtifactId(), dependency.getBaseVersion(),
                    getLog(), dependency.getFile());
        }

		File file = new File(getLocalRepository().getBasedir(),
				getLocalRepository().pathOf(dependency));
		if (!file.exists()) {
			getLog().debug("Dependency nar file does not exist: " + file);
			return null;
		}

		JarFile jar = null;
		try {
			jar = new JarFile(file);
			NarInfo info = new NarInfo(dependency.getGroupId(),
					dependency.getArtifactId(), dependency.getBaseVersion(),
					getLog());
			if (!info.exists(jar)) {
				getLog().debug(
						"Dependency nar file does not contain this artifact: "
								+ file);
				return null;
			}
			info.read(jar);
			return info;
		} catch (IOException e) {
			throw new MojoExecutionException("Error while reading " + file, e);
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public final List<AttachedNarArtifact> getAllAttachedNarArtifacts(
			List<NarArtifact> narArtifacts/*, Library library*/)
			throws MojoExecutionException, MojoFailureException {
		List<AttachedNarArtifact> artifactList = new ArrayList<AttachedNarArtifact>();
		for (Iterator<NarArtifact> i = narArtifacts.iterator(); i.hasNext();) {
			NarArtifact dependency = i.next();

			String binding = getBinding(/*library,*/ dependency);

			// TODO: dependency.getFile(); find out what the stored pom says
			// about this - what nars should exist, what layout are they
			// using...
			artifactList.addAll(getAttachedNarArtifacts(dependency, /* library. */
					getAOL(), binding));
			artifactList.addAll(getAttachedNarArtifacts(dependency, null,
					NarConstants.NAR_NO_ARCH));
		}
		return artifactList;
	}

	protected String getBinding(/*Library library,*/ NarArtifact dependency)
			throws MojoFailureException, MojoExecutionException {
		// how does this project specify the dependency is used
		// - library.getLinker().getLibs();
		// - if it is specified but the artifact is not available should fail.
		//   otherwise how does the artifact specify it should be used by default
		//
		// - what is the preference for this type of library to use (shared - shared, static - static...)

		// library.getType()
		String binding = dependency.getNarInfo().getBinding(
		/* library. */getAOL(), /* type != null ? type : */
		Library.STATIC);
		return binding;
	}

	private List<AttachedNarArtifact> getAttachedNarArtifacts(
			NarArtifact dependency, AOL aol, String type)
			throws MojoExecutionException, MojoFailureException {
		getLog().debug(
				"GetNarDependencies for " + dependency + ", aol: " + aol
						+ ", type: " + type);
		List<AttachedNarArtifact> artifactList = new ArrayList<AttachedNarArtifact>();
		NarInfo narInfo = dependency.getNarInfo();
		String[] nars = narInfo.getAttachedNars(aol, type);
		// FIXME Move this to NarInfo....
		if (nars != null) {
			for (int j = 0; j < nars.length; j++) {
				getLog().debug("    Checking: " + nars[j]);
				if (nars[j].equals("")) {
					continue;
				}
				String[] nar = nars[j].split(":", 5);
				if (nar.length >= 4) {
					try {
						String groupId = nar[0].trim();
						String artifactId = nar[1].trim();
						String ext = nar[2].trim();
						String classifier = nar[3].trim();
						// translate for instance g++ to gcc...
						AOL aolString = narInfo.getAOL(aol);
						if (aolString != null) {
							classifier = NarUtil.replace("${aol}",
									aolString.toString(), classifier);
						}
						String version = nar.length >= 5 ? nar[4].trim()
								: dependency.getBaseVersion();
						artifactList.add(new AttachedNarArtifact(groupId,
								artifactId, version, dependency.getScope(),
								ext, classifier, dependency.isOptional(),
								dependency.getFile()));
					} catch (InvalidVersionSpecificationException e) {
						throw new MojoExecutionException(
								"Error while reading nar file for dependency "
										+ dependency, e);
					}
				} else {
					getLog().warn(
							"nars property in " + dependency.getArtifactId()
									+ " contains invalid field: '" + nars[j]
					// + "' for type: " + type
					);
				}
			}
		}
		return artifactList;
	}

	public final void downloadAttachedNars(
			List<AttachedNarArtifact> dependencies)
			throws MojoExecutionException, MojoFailureException {
		getLog().debug( "Download for NarDependencies {" );
		for (Iterator<AttachedNarArtifact> i = dependencies.iterator(); i.hasNext();) {
			getLog().debug("  - " + (i.next()));
		}
		getLog().debug("}");

		for (Iterator<AttachedNarArtifact> i = dependencies.iterator(); i.hasNext();) {
			Artifact dependency = (Artifact) i.next();
			try {
				getLog().debug("Resolving " + dependency);
				artifactResolver.resolve(dependency, remoteArtifactRepositories, getLocalRepository());
			} catch (ArtifactNotFoundException e) {
				String message = "nar not found " + dependency.getId();
				throw new MojoExecutionException(message, e);
			} catch (ArtifactResolutionException e) {
				String message = "nar cannot resolve " + dependency.getId();
				throw new MojoExecutionException(message, e);
			}
		}
	}

    public final void unpackAttachedNars( List<AttachedNarArtifact> dependencies )
        	throws MojoExecutionException, MojoFailureException
        {
        	File unpackDir = getUnpackDirectory();

    	    getLog().info( String.format( "Unpacking %1$d dependencies to %2$s", dependencies.size(), unpackDir) );

    	    for ( Iterator i = dependencies.iterator(); i.hasNext(); )
    	    {
    	    	AttachedNarArtifact dependency = (AttachedNarArtifact) i.next();
    		    File file = getNarManager().getNarFile( dependency ); //dependency.getNarFile();
    		    getLog().debug( String.format( "Unpack %1$s (%2$s) to %3$s", dependency, file, unpackDir) );
    		
    		    // TODO: each dependency may have it's own (earlier) version of layout - if it is unknown then we should report an error to update the nar package
    		    // NarLayout layout = AbstractNarLayout.getLayout( "NarLayout21"/* TODO: dependency.getLayout() */, getLog() );
    		    // we should then target the layout to match the layout for this nar which is the workspace we are in.
    		    NarLayout layout = getLayout();
    		    // TODO: the dependency may be specified against a different linker (version)?
    		    // AOL aol = dependency.getClassifier();  Trim
    		    layout.unpackNar(unpackDir, archiverManager, file, getOS(), getLinker().getName(), getAOL() );            
    	    }
        }    

	public void narExecute() throws MojoFailureException ,MojoExecutionException {
		getLog().info("Preparing Nar dependencies");
		List<NarArtifact> narArtifacts = getNarArtifacts( );
		List<AttachedNarArtifact> dependencies = getAllAttachedNarArtifacts( narArtifacts/*, library*/ );
		downloadAttachedNars( dependencies );
		unpackAttachedNars( dependencies );
	};

}
