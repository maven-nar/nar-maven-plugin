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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarManager
{

	private Log log;

	private MavenProject project;

	private ArtifactRepository repository;

	private AOL defaultAOL;

	private String linkerName;

    private String[] narTypes =
        { NarConstants.NAR_NO_ARCH, Library.STATIC, Library.SHARED, Library.JNI, Library.PLUGIN };

    public NarManager( Log log, ArtifactRepository repository, MavenProject project, String architecture, String os,
                       Linker linker )
        throws MojoFailureException, MojoExecutionException
    {
		this.log = log;
		this.repository = repository;
		this.project = project;
		this.defaultAOL = NarUtil.getAOL(project, architecture, os, linker, null, log );
		this.linkerName = NarUtil.getLinkerName(project, architecture, os, linker, log );
	}

	/**
     * Returns dependencies which are dependent on NAR files (i.e. contain NarInfo)
	 */
	public final List/* <NarArtifact> */getNarDependencies(String scope)
        throws MojoExecutionException
    {
		List narDependencies = new LinkedList();
        for ( Iterator i = getDependencies( scope ).iterator(); i.hasNext(); )
        {
			Artifact dependency = (Artifact) i.next();
			log.debug("Examining artifact for NarInfo: " + dependency);

			NarInfo narInfo = getNarInfo(dependency);
            if ( narInfo != null )
            {
				log.debug("    - added as NarDependency");
				narDependencies.add(new NarArtifact(dependency, narInfo));
			}
		}
		return narDependencies;
	}

	/**
     * Returns all NAR dependencies by type: noarch, static, dynamic, jni, plugin.
	 * 
	 * @throws MojoFailureException
	 */
    public final Map/* <String, List<AttachedNarArtifact>> */getAttachedNarDependencyMap( String scope )
        throws MojoExecutionException, MojoFailureException
    {
		Map attachedNarDependencies = new HashMap();
        for ( Iterator i = getNarDependencies( scope ).iterator(); i.hasNext(); )
        {
			Artifact dependency = (Artifact) i.next();
            for ( int j = 0; j < narTypes.length; j++ )
            {
                List artifactList = getAttachedNarDependencies( dependency, defaultAOL, narTypes[j] );
                if ( artifactList != null )
                {
					attachedNarDependencies.put(narTypes[j], artifactList);
				}
			}
		}
		return attachedNarDependencies;
	}

    public final List/* <AttachedNarArtifact> */getAttachedNarDependencies( List/* <NarArtifacts> */narArtifacts )
        throws MojoExecutionException, MojoFailureException
    {
		return getAttachedNarDependencies(narArtifacts, (String) null);
	}

    public final List/* <AttachedNarArtifact> */getAttachedNarDependencies( List/* <NarArtifacts> */narArtifacts,
                                                                            String classifier )
        throws MojoExecutionException, MojoFailureException
    {
		AOL aol = null;
		String type = null;
        if ( classifier != null )
        {
			int dash = classifier.lastIndexOf('-');
            if ( dash < 0 )
            {
				aol = new AOL(classifier);
				type = null;
            }
            else
            {
				aol = new AOL(classifier.substring(0, dash));
				type = classifier.substring(dash + 1);
			}
		}
		return getAttachedNarDependencies(narArtifacts, aol, type);
	}

	public final List/* <AttachedNarArtifact> */getAttachedNarDependencies(
			List/* <NarArtifacts> */narArtifacts, String[] classifiers)
                throws MojoExecutionException, MojoFailureException
    {

		List artifactList = new ArrayList();

        if( classifiers != null && classifiers.length > 0 )
        {

            for ( int j = 0; j < classifiers.length; j++ )
            {
                if ( artifactList != null )
                {
                    artifactList.addAll( getAttachedNarDependencies( narArtifacts, classifiers[j] ));
                }
            }
        }
        else
        {
            artifactList.addAll( getAttachedNarDependencies( narArtifacts, ( String )null ));
		}

		return artifactList;
	}

	/**
     * Returns a list of all attached nar dependencies for a specific binding and "noarch", but not where "local" is
     * specified
	 * 
     * @param scope compile, test, runtime, ....
     * @param aol either a valid aol, noarch or null. In case of null both the default getAOL() and noarch dependencies
     *            are returned.
     * @param type noarch, static, shared, jni, or null. In case of null the default binding found in narInfo is used.
	 * @return
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
    public final List/* <AttachedNarArtifact> */getAttachedNarDependencies( List/* <NarArtifacts> */narArtifacts,
                                                                            AOL archOsLinker, String type )
        throws MojoExecutionException, MojoFailureException
    {
		boolean noarch = false;
		AOL aol = archOsLinker;
        if ( aol == null )
        {
			noarch = true;
			aol = defaultAOL;
		}

		List artifactList = new ArrayList();
        for ( Iterator i = narArtifacts.iterator(); i.hasNext(); )
        {
			Artifact dependency = (Artifact) i.next();
			NarInfo narInfo = getNarInfo(dependency);
            if ( noarch )
            {
                artifactList.addAll( getAttachedNarDependencies( dependency, null, NarConstants.NAR_NO_ARCH ) );
			}

			// use preferred binding, unless non existing.
            String binding = narInfo.getBinding( aol, type != null ? type : Library.STATIC );

			// FIXME kludge, but does not work anymore since AOL is now a class
            if ( aol.equals( NarConstants.NAR_NO_ARCH ) )
            {
				// FIXME no handling of local
                artifactList.addAll( getAttachedNarDependencies( dependency, null, NarConstants.NAR_NO_ARCH ) );
            }
            else
            {
                artifactList.addAll( getAttachedNarDependencies( dependency, aol, binding ) );
			}
		}
		return artifactList;
	}

    private List/* <AttachedNarArtifact> */getAttachedNarDependencies( Artifact dependency, AOL archOsLinker,
                                                                       String type )
        throws MojoExecutionException, MojoFailureException
    {
		AOL aol = archOsLinker;
        log.debug( "GetNarDependencies for " + dependency + ", aol: " + aol + ", type: " + type );
		List artifactList = new ArrayList();
		NarInfo narInfo = getNarInfo(dependency);
		String[] nars = narInfo.getAttachedNars(aol, type);
		// FIXME Move this to NarInfo....
        if ( nars != null )
        {
            for ( int j = 0; j < nars.length; j++ )
            {
				log.debug("    Checking: " + nars[j]);
                if ( nars[j].equals( "" ) )
                {
					continue;
				}
				String[] nar = nars[j].split(":", 5);
                if ( nar.length >= 4 )
                {
                    try
                    {
						String groupId = nar[0].trim();
						String artifactId = nar[1].trim();
						String ext = nar[2].trim();
						String classifier = nar[3].trim();
						// translate for instance g++ to gcc...
						aol = narInfo.getAOL(aol);
                        if ( aol != null )
                        {
                            classifier = NarUtil.replace( "${aol}", aol.toString(), classifier );
                        }
                        String version = nar.length >= 5 ? nar[4].trim() : dependency.getBaseVersion();
                        artifactList.add( new AttachedNarArtifact( groupId, artifactId, version, dependency.getScope(),
                                                                   ext, classifier, dependency.isOptional(), dependency.getFile() ));
                    }
                    catch ( InvalidVersionSpecificationException e )
                    {
                        throw new MojoExecutionException( "Error while reading nar file for dependency " + dependency,
                                                          e );
                    }
                }
                else
                {
                    log.warn( "nars property in " + dependency.getArtifactId() + " contains invalid field: '" + nars[j]
							+ "' for type: " + type);
				}
			}
		}
		return artifactList;
	}

	public final NarInfo getNarInfo(Artifact dependency)
        throws MojoExecutionException
    {
		// FIXME reported to maven developer list, isSnapshot changes behaviour
		// of getBaseVersion, called in pathOf.
		dependency.isSnapshot();

        File file = new File( repository.getBasedir(), repository.pathOf( dependency ) );
        if ( !file.exists() )
        {
			return null;
		}

		JarFile jar = null;
        try
        {
			jar = new JarFile(file);
            NarInfo info =
                new NarInfo( dependency.getGroupId(), dependency.getArtifactId(), dependency.getBaseVersion(), log );
            if ( !info.exists( jar ) )
            {
				return null;
			}
			info.read(jar);
			return info;
        }
        catch ( IOException e )
        {
			throw new MojoExecutionException("Error while reading " + file, e);
        }
        finally
        {
            if ( jar != null )
            {
                try
                {
					jar.close();
                }
                catch ( IOException e )
                {
					// ignore
				}
			}
		}
	}

	public final File getNarFile(Artifact dependency)
        throws MojoFailureException
    {
		// FIXME reported to maven developer list, isSnapshot changes behaviour
		// of getBaseVersion, called in pathOf.
		dependency.isSnapshot();
        return new File( repository.getBasedir(), NarUtil.replace( "${aol}", defaultAOL.toString(),
                                                                   repository.pathOf( dependency ) ) );
	}

    private List getDependencies( String scope )
    {
        Set<Artifact> artifacts = project.getArtifacts();
        List<Artifact> returnArtifact = new ArrayList<Artifact>();
        for(Artifact a : artifacts) {
            if(scope.equals(a.getScope()))
                returnArtifact.add(a);
        }
        return returnArtifact;
	}

    public final void downloadAttachedNars( List/* <NarArtifacts> */narArtifacts, List remoteRepositories,
			ArtifactResolver resolver, String classifier)
        throws MojoExecutionException, MojoFailureException
    {
		// FIXME this may not be the right way to do this.... -U ignored and
		// also SNAPSHOT not used
		List dependencies = getAttachedNarDependencies(narArtifacts, classifier);

        log.debug( "Download called with classifier: " + classifier + " for NarDependencies {" );
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
			log.debug("  - " + (i.next()));
		}
		log.debug("}");

        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
			Artifact dependency = (Artifact) i.next();
            try
            {
				log.debug("Resolving " + dependency);
				resolver.resolve(dependency, remoteRepositories, repository);
            }
            catch ( ArtifactNotFoundException e )
            {
				String message = "nar not found " + dependency.getId();
				throw new MojoExecutionException(message, e);
            }
            catch ( ArtifactResolutionException e )
            {
				String message = "nar cannot resolve " + dependency.getId();
				throw new MojoExecutionException(message, e);
			}
		}
	}

    public final void unpackAttachedNars( List/* <NarArtifacts> */narArtifacts, ArchiverManager archiverManager,
                                          String classifier, String os, NarLayout layout, File unpackDir )
        throws MojoExecutionException, MojoFailureException
    {
        log.debug( "Unpack called for OS: " + os + ", classifier: " + classifier + " for NarArtifacts {" );
        for ( Iterator i = narArtifacts.iterator(); i.hasNext(); )
        {
			log.debug("  - " + (i.next()));
		}
		log.debug("}");
		// FIXME, kludge to get to download the -noarch, based on classifier
		List dependencies = getAttachedNarDependencies(narArtifacts, classifier);
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
			Artifact dependency = (Artifact) i.next();
            log.debug("Unpack " + dependency + " to " + unpackDir);
			File file = getNarFile(dependency);

            layout.unpackNar(unpackDir, archiverManager, file, os, linkerName, defaultAOL);            
		}
	}
}
