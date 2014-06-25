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
import java.util.Iterator;
import java.util.List;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;

/**
 * Keeps info on a library
 * 
 * @author Mark Donszelmann
 */
public class Lib
{

	/**
     * Name of the library, or a dependency groupId:artifactId if this library contains sublibraries
	 * 
	 * @parameter default-value=""
	 * @required
	 */
	private String name;

	/**
	 * Type of linking for this library
	 * 
	 * @parameter default-value="shared"
	 * @required
	 */
	private String type = Library.SHARED;

	/**
	 * Location for this library
	 * 
	 * @parameter default-value=""
	 * @required
	 */
	private File directory;

	/**
	 * Sub libraries for this library
	 * 
	 * @parameter default-value=""
	 */
	private List/* <Lib> */libs;

    public final void addLibSet( AbstractDependencyMojo mojo, LinkerDef linker, Project antProject )
        throws MojoFailureException, MojoExecutionException
    {
        if ( name == null )
        {
            throw new MojoFailureException( "NAR: Please specify <Name> as part of <Lib> for library \"" + name + "\"");
        }
        addLibSet( mojo, linker, antProject, name, directory );
    }

    private void addLibSet( AbstractDependencyMojo mojo, LinkerDef linker, Project antProject, String name, File dir )
        throws MojoFailureException, MojoExecutionException
    {
        if ( libs == null )
        {
            addSingleLibSet( linker, antProject, name, dir );
        }
        else
        {
            addMultipleLibSets( mojo, linker, antProject, name );
        }
    }

    private void addSingleLibSet( LinkerDef linker, Project antProject, String name, File dir )
        throws MojoFailureException, MojoExecutionException
    {   
		if (!type.equals("framework") && (dir == null)) 
        {
			throw new MojoFailureException("NAR: Please specify <Directory> as part of <Lib> for library \"" + name + "\"");
		}
		LibrarySet libSet = new LibrarySet();
		libSet.setProject(antProject);
		libSet.setLibs(new CUtil.StringArrayBuilder(name));
		LibraryTypeEnum libType = new LibraryTypeEnum();
		libType.setValue(type);
		libSet.setType(libType);
		libSet.setDir(dir);
		linker.addLibset(libSet);
	}

    private void addMultipleLibSets( AbstractDependencyMojo mojo, LinkerDef linker, Project antProject, String name )
        throws MojoFailureException, MojoExecutionException
    {
		List dependencies = mojo.getNarArtifacts();
        for ( Iterator i = libs.iterator(); i.hasNext(); )
        {
			Lib lib = (Lib) i.next();
			String[] ids = name.split(":", 2);
            if ( ids.length != 2 )
            {
				throw new MojoFailureException(
						"NAR: Please specify <Name> as part of <Lib> in format 'groupId:artifactId'");
			}
            for ( Iterator j = dependencies.iterator(); j.hasNext(); )
            {
				Artifact dependency = (Artifact) j.next();
                if ( dependency.getGroupId().equals( ids[0] ) && dependency.getArtifactId().equals( ids[1] ) )
                {
					// FIXME NAR-90
                    File narDir =
                        new File( dependency.getFile().getParentFile(), "nar/lib/"
									+ mojo.getAOL() + "/" + lib.type);
                    String narName = dependency.getArtifactId() + "-" + lib.name + "-" + dependency.getBaseVersion();
					lib.addLibSet(mojo, linker, antProject, narName, narDir);
				}
			}
		}
	}
}
