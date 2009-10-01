// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.LinkerDef;
import net.sf.antcontrib.cpptasks.OutputTypeEnum;
import net.sf.antcontrib.cpptasks.RuntimeType;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LinkerArgument;
import net.sf.antcontrib.cpptasks.types.SystemLibrarySet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Compiles native source files.
 * 
 * @goal nar-compile
 * @phase compile
 * @requiresDependencyResolution compile
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarCompileMojo.java 0ee9148b7c6a 2007/09/20 18:42:29 duns $
 */
public class NarCompileMojo extends AbstractCompileMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (shouldSkip())
			return;

		// make sure destination is there
		getTargetDirectory().mkdirs();

		// check for source files
		int noOfSources = 0;
		noOfSources += getSourcesFor(getCpp()).size();
		noOfSources += getSourcesFor(getC()).size();
		noOfSources += getSourcesFor(getFortran()).size();
		if (noOfSources > 0) {
			for (Iterator i = getLibraries().iterator(); i.hasNext();) {
				createLibrary(getAntProject(), (Library) i.next());
			}
		}

		try {
			// FIXME, should the include paths be defined at a higher level ?
			getCpp().copyIncludeFiles(getMavenProject(),
					new File(getTargetDirectory(), "include"));
		} catch (IOException e) {
			throw new MojoExecutionException(
					"NAR: could not copy include files", e);
		}
	}

	private List getSourcesFor(Compiler compiler) throws MojoFailureException {
		try {
			File srcDir = compiler.getSourceDirectory();
			return srcDir.exists() ? FileUtils.getFiles(srcDir, StringUtils
					.join(compiler.getIncludes().iterator(), ","), null)
					: Collections.EMPTY_LIST;
		} catch (IOException e) {
			return Collections.EMPTY_LIST;
		}
	}

	private void createLibrary(Project antProject, Library library)
			throws MojoExecutionException, MojoFailureException {
		// configure task
		CCTask task = new CCTask();
		task.setProject(antProject);

		// set max cores
		task.setMaxCores(getMaxCores(getAOL()));
		
		// outtype
		OutputTypeEnum outTypeEnum = new OutputTypeEnum();
		String type = library.getType();
		outTypeEnum.setValue(type);
		task.setOuttype(outTypeEnum);

		// stdc++
		task.setLinkCPP(library.linkCPP());

		// fortran
		task.setLinkFortran(library.linkFortran());

		// outDir
		File outDir = new File(getTargetDirectory(), type
				.equals(Library.EXECUTABLE) ? "bin" : "lib");
		outDir = new File(outDir, getAOL().toString());
		if (!type.equals(Library.EXECUTABLE))
			outDir = new File(outDir, type);
		outDir.mkdirs();

		// outFile
		File outFile;
		if (type.equals(Library.EXECUTABLE)) {
			// executable has no version number
			outFile = new File(outDir, getMavenProject().getArtifactId());
		} else {
			outFile = new File(outDir, getOutput(getAOL()));
		}
		getLog().debug("NAR - output: '" + outFile + "'");
		task.setOutfile(outFile);

		// object directory
		File objDir = new File(getTargetDirectory(), "obj");
		objDir = new File(objDir, getAOL().toString());
		objDir.mkdirs();
		task.setObjdir(objDir);

		// failOnError, libtool
		task.setFailonerror(failOnError(getAOL()));
		task.setLibtool(useLibtool(getAOL()));

		// runtime
		RuntimeType runtimeType = new RuntimeType();
		runtimeType.setValue(getRuntime(getAOL()));
		task.setRuntime(runtimeType);

		// add C++ compiler
		task.addConfiguredCompiler(getCpp().getCompiler(type, getOutput(getAOL())));

		// add C compiler
		task.addConfiguredCompiler(getC().getCompiler(type, getOutput(getAOL())));

		// add Fortran compiler
		task.addConfiguredCompiler(getFortran().getCompiler(type, getOutput(getAOL())));

		// add javah include path
		File jniDirectory = getJavah().getJniDirectory();
		if (jniDirectory.exists())
			task.createIncludePath().setPath(jniDirectory.getPath());

		// add java include paths
		getJava().addIncludePaths(task, type);

		// add dependency include paths
		for (Iterator i = getNarManager().getNarDependencies("compile")
				.iterator(); i.hasNext();) {
			// FIXME, handle multiple includes from one NAR
			NarArtifact narDependency = (NarArtifact) i.next();
			String binding = narDependency.getNarInfo().getBinding(getAOL(),
					Library.STATIC);
			getLog().debug(
					"Looking for " + narDependency + " found binding "
							+ binding);
			if (!binding.equals(Library.JNI)) {
				File include = new File(getNarManager().getNarFile(
						narDependency).getParentFile(), "nar/include");
				getLog().debug("Looking for for directory: " + include);
				if (include.exists()) {
					task.createIncludePath().setPath(include.getPath());
				}
			}
		}

		// add linker
		LinkerDef linkerDefinition = getLinker().getLinker(this, antProject,
				getOS(), getAOL().getKey() + ".linker.", type);
		task.addConfiguredLinker(linkerDefinition);

		// add dependency libraries
		// FIXME: what about PLUGIN and STATIC, depending on STATIC, should we
		// not add all libraries, see NARPLUGIN-96
		if (type.equals(Library.SHARED) || type.equals(Library.JNI) || type.equals(Library.EXECUTABLE)) {

            List depLibOrder = getDependencyLibOrder();
            List depLibs = getNarManager().getNarDependencies("compile");

            // reorder the libraries that come from the nar dependencies
            // to comply with the order specified by the user
            if ((depLibOrder != null) && !depLibOrder.isEmpty()) {

                List tmp = new LinkedList();

                for (Iterator i = depLibOrder.iterator(); i.hasNext();) {
                    
                    String depToOrderName = (String)i.next();

                    for (Iterator j = depLibs.iterator(); j.hasNext();) {

                        NarArtifact dep = (NarArtifact)j.next();
                        String depName = dep.getGroupId() + ":" + dep.getArtifactId();

                        if (depName.equals(depToOrderName)) {

                            tmp.add(dep);
                            j.remove();
                        }
                    }
                }

                tmp.addAll(depLibs);
                depLibs = tmp;
            }

            for (Iterator i = depLibs.iterator(); i.hasNext();) {

			    NarArtifact dependency = (NarArtifact) i.next();

				// FIXME no handling of "local"

				// FIXME, no way to override this at this stage
				String binding = dependency.getNarInfo().getBinding(getAOL(),
						Library.STATIC);
				getLog().debug("Using Binding: " + binding);
				AOL aol = getAOL();
				aol = dependency.getNarInfo().getAOL(getAOL());
				getLog().debug("Using Library AOL: " + aol.toString());

				if (!binding.equals(Library.JNI)) {
					File dir = new File(getNarManager().getNarFile(dependency)
							.getParentFile(), "nar/lib/" + aol.toString() + "/" + binding);
					getLog().debug("Looking for Library Directory: " + dir);
					if (dir.exists()) {
						LibrarySet libSet = new LibrarySet();
						libSet.setProject(antProject);

						// FIXME, no way to override
						String libs = dependency.getNarInfo().getLibs(getAOL());
						if ((libs != null) && !libs.equals("")) {
							getLog().debug("Using LIBS = " + libs);
							libSet.setLibs(new CUtil.StringArrayBuilder(libs));
							libSet.setDir(dir);
							task.addLibset(libSet);
						}
					} else {
						getLog()
								.debug(
										"Library Directory " + dir
												+ " does NOT exist.");
					}

					// FIXME, look again at this, for multiple dependencies we may need to remove duplicates
					String options = dependency.getNarInfo().getOptions(getAOL());
					if ((options != null) && !options.equals("")) {
						getLog().debug("Using OPTIONS = " + options);
						LinkerArgument arg = new LinkerArgument();
						arg.setValue(options);
						linkerDefinition.addConfiguredLinkerArg(arg);
					}
					
					String sysLibs = dependency.getNarInfo().getSysLibs(
							getAOL());
					if ((sysLibs != null) && !sysLibs.equals("")) {
						getLog().debug("Using SYSLIBS = " + sysLibs);
						SystemLibrarySet sysLibSet = new SystemLibrarySet();
						sysLibSet.setProject(antProject);

						sysLibSet
								.setLibs(new CUtil.StringArrayBuilder(sysLibs));
						task.addSyslibset(sysLibSet);
					}
				}
			}
		}

		// Add JVM to linker
		getJava().addRuntime(task, getJavaHome(getAOL()), getOS(),
				getAOL().getKey() + "java.");

		// execute
		try {
			task.execute();
		} catch (BuildException e) {
			throw new MojoExecutionException("NAR: Compile failed", e);
		}
		
		// FIXME, this should be done in CPPTasks at some point
		if (getRuntime(getAOL()).equals("dynamic") &&
			getOS().equals(OS.WINDOWS) && 
		    getLinker().getName(null, null).equals("msvc") && 
		    NarUtil.getEnv("MSVCVer", "MSVCVer", "6.0").startsWith("8.")) {
			String libType = library.getType();
			if (libType.equals(Library.JNI) || libType.equals(Library.SHARED)) {
				String dll = outFile.getPath()+".dll";
				String manifest = dll+".manifest";
				int result = NarUtil.runCommand("mt.exe", new String[] {"/manifest", manifest, "/outputresource:"+dll+";#2"}, null, getLog());
				if (result != 0)
					throw new MojoFailureException("MT.EXE failed with exit code: " + result);
			}
		}
	}
}
