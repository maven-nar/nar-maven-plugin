package org.apache.maven.plugin.nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Iterator;
import java.util.List;

import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.LinkerDef;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;

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
	 * @parameter expression=""
	 * @required
	 */
	private String name;

	/**
	 * Type of linking for this library
	 * 
	 * @parameter expression="" default-value="shared"
	 * @required
	 */
	private String type = Library.SHARED;

	/**
	 * Location for this library
	 * 
	 * @parameter expression=""
	 * @required
	 */
	private File directory;

	/**
	 * Sub libraries for this library
	 * 
	 * @parameter expression=""
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
		List dependencies = mojo.getNarManager().getNarDependencies("compile");
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
                    String narName = dependency.getArtifactId() + "-" + lib.name + "-" + dependency.getVersion();
					lib.addLibSet(mojo, linker, antProject, narName, narDir);
				}
			}
		}
	}
}
