// Copyright FreeHEP, 2007.
package org.freehep.maven.nar;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.SelectorUtils;

/**
 * Copies any resources, including AOL specific distributions, to the target
 * area for packaging
 * 
 * @goal nar-resources
 * @phase process-resources
 * @requiresProject
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarResourcesMojo.java 2126b860c9c5 2007/07/31 23:19:30 duns $
 */
public class NarResourcesMojo extends AbstractCompileMojo {

	/**
	 * Directory for nar resources. Defaults to src/nar/resources
	 * 
	 * @parameter expression="${basedir}/src/nar/resources"
	 * @required
	 */
	private File resourceDirectory;

	/**
	 * Binary directory (relative to ${resourceDirectory}/aol/${aol}
	 * 
	 * @parameter expression="bin"
	 * @required
	 */
	private String resourceBinDir;
	
	/**
	 * Include directory (relative to ${resourceDirectory}/aol/${aol}
	 * 
	 * @parameter expression="include"
	 * @required
	 */
	private String resourceIncludeDir;
	
	/**
	 * Library directory (relative to ${resourceDirectory}/aol/${aol}
	 * 
	 * @parameter expression="lib"
	 * @required
	 */
	private String resourceLibDir;
	
    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     */
    private ArchiverManager archiverManager;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (shouldSkip())
			return;

		// scan for AOLs
		File aolDir = new File(resourceDirectory, "aol");
		if (aolDir.exists()) {
			String[] aols = aolDir.list();
			for (int i = 0; i < aols.length; i++) {
				boolean ignore = false;
				for (Iterator j = FileUtils.getDefaultExcludesAsList()
						.iterator(); j.hasNext();) {
					String exclude = (String)j.next();
					if (SelectorUtils.matchPath(exclude.replace('/', File.separatorChar), aols[i])) {
						ignore = true;
						break;
					}
				}
				if (!ignore) {
					copyResources(new File(aolDir, aols[i]));
				}
			}
		}
	}

	private void copyResources(File aolDir) throws MojoExecutionException,
			MojoFailureException {
		String aol = aolDir.getName();
		int copied = 0;
		try {
			// copy headers
			File includeDir = new File(aolDir, resourceIncludeDir);
			if (includeDir.exists()) {
				File includeDstDir = new File(getTargetDirectory(), "include");
				copied += NarUtil.copyDirectoryStructure(includeDir,
						includeDstDir, null, NarUtil.DEFAULT_EXCLUDES);
			}

			// copy binaries
			File binDir = new File(aolDir, resourceBinDir);
			if (binDir.exists()) {
				File binDstDir = new File(getTargetDirectory(), "bin");
				binDstDir = new File(binDstDir, aol);

				copied += NarUtil.copyDirectoryStructure(binDir, binDstDir,
						null, NarUtil.DEFAULT_EXCLUDES);
			}

			// copy libraries
			File libDir = new File(aolDir, resourceLibDir);
			if (libDir.exists()) {
				// create all types of libs
				for (Iterator i = getLibraries().iterator(); i.hasNext();) {
					Library library = (Library) i.next();
					String type = library.getType();
					File libDstDir = new File(getTargetDirectory(), "lib");
					libDstDir = new File(libDstDir, aol);
					libDstDir = new File(libDstDir, type);

					// filter files for lib
					String includes = "**/*."
							+ NarUtil.getDefaults().getProperty(
									NarUtil.getAOLKey(aol) + "." + type
											+ ".extension");
					copied += NarUtil.copyDirectoryStructure(libDir, libDstDir,
							includes, NarUtil.DEFAULT_EXCLUDES);
				}
			}
			
			// unpack jar files
			File classesDirectory = new File(getOutputDirectory(),"classes");
			classesDirectory.mkdirs();
			List jars = FileUtils.getFiles(aolDir, "**/*.jar", null);
			for (Iterator i=jars.iterator(); i.hasNext(); ) {
				File jar = (File)i.next();
				getLog().debug("Unpacking jar "+jar);
				UnArchiver unArchiver;
				unArchiver = archiverManager.getUnArchiver(AbstractNarMojo.NAR_ROLE_HINT);
				unArchiver.setSourceFile(jar);
				unArchiver.setDestDirectory(classesDirectory);
				unArchiver.extract();
			}
		} catch (IOException e) {
			throw new MojoExecutionException("NAR: Could not copy resources", e);
		} catch (NoSuchArchiverException e) {
			throw new MojoExecutionException("NAR: Could not find archiver", e);
		} catch (ArchiverException e) {
			throw new MojoExecutionException("NAR: Could not unarchive jar file", e);
		}
		getLog().info("Copied " + copied + " resources for " + aol);
	}

}
