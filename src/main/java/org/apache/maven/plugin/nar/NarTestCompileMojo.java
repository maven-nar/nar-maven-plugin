// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.OutputTypeEnum;
import net.sf.antcontrib.cpptasks.RuntimeType;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Compiles native test source files.
 * 
 * @goal nar-testCompile
 * @phase test-compile
 * @requiresDependencyResolution test
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarTestCompileMojo.java 0ee9148b7c6a 2007/09/20 18:42:29 duns $
 */
public class NarTestCompileMojo extends AbstractCompileMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (shouldSkip())
			return;

		// make sure destination is there
		getTargetDirectory().mkdirs();

		for (Iterator i = getTests().iterator(); i.hasNext();) {
			createTest(getAntProject(), (Test) i.next());
		}
	}

	private void createTest(Project antProject, Test test)
			throws MojoExecutionException, MojoFailureException {
		String type = "test";

		// configure task
		CCTask task = new CCTask();
		task.setProject(antProject);

		// outtype
		OutputTypeEnum outTypeEnum = new OutputTypeEnum();
		outTypeEnum.setValue(Library.EXECUTABLE);
		task.setOuttype(outTypeEnum);

		// outDir
		File outDir = new File(getTargetDirectory(), "bin");
		outDir = new File(outDir, getAOL().toString());
		outDir.mkdirs();

		// outFile
		File outFile = new File(outDir, test.getName());
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
		task.addConfiguredCompiler(getCpp().getCompiler(type, test.getName()));

		// add C compiler
		task.addConfiguredCompiler(getC().getCompiler(type, test.getName()));

		// add Fortran compiler
		task.addConfiguredCompiler(getFortran().getCompiler(type,
				test.getName()));

		// add java include paths
		getJava().addIncludePaths(task, type);

		// add dependency include paths
		for (Iterator i = getNarManager().getNarDependencies("test").iterator(); i
				.hasNext();) {
			File include = new File(getNarManager().getNarFile(
					(Artifact) i.next()).getParentFile(), "nar/include");
			if (include.exists()) {
				task.createIncludePath().setPath(include.getPath());
			}
		}

		// add linker
		task.addConfiguredLinker(getLinker().getLinker(this, antProject,
				getOS(), getAOL().getKey() + "linker.", type));
		
		// FIXME hardcoded values
		String libName = getFinalName();
		File includeDir = new File(getMavenProject().getBuild().getDirectory(),
				"nar/include");
		File libDir = new File(getMavenProject().getBuild().getDirectory(),
				"nar/lib/" + getAOL() + "/" + test.getLink());

		// copy shared library
		// FIXME why do we do this ?
/* Removed in alpha-10
		if (test.getLink().equals(Library.SHARED)) {
			try {
				// defaults are Unix
				String libPrefix = NarUtil.getDefaults().getProperty(
						getAOLKey() + "shared.prefix", "lib");
				String libExt = NarUtil.getDefaults().getProperty(
						getAOLKey() + "shared.extension", "so");
				File copyDir = new File(getTargetDirectory(), (getOS().equals(
						"Windows") ? "bin" : "lib")
						+ "/" + getAOL() + "/" + test.getLink());
				FileUtils.copyFileToDirectory(new File(libDir, libPrefix
						+ libName + "." + libExt), copyDir);
				if (!getOS().equals(OS.WINDOWS)) {
					libDir = copyDir;
				}
			} catch (IOException e) {
				throw new MojoExecutionException(
						"NAR: Could not copy shared library", e);
			}
		}
*/
		// FIXME what about copying the other shared libs?

		// add include of this package
		if (includeDir.exists()) {
			task.createIncludePath().setLocation(includeDir);
		}

		// add library of this package
		if (libDir.exists()) {
			LibrarySet libSet = new LibrarySet();
			libSet.setProject(antProject);
			libSet.setLibs(new CUtil.StringArrayBuilder(libName));
			LibraryTypeEnum libType = new LibraryTypeEnum();
			libType.setValue(test.getLink());
			libSet.setType(libType);
			libSet.setDir(libDir);
			task.addLibset(libSet);
		}

		// add dependency libraries
        List depLibOrder = getDependencyLibOrder();
        List depLibs = getNarManager().getNarDependencies("test");

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

			Artifact dependency = (Artifact) i.next();
			// FIXME: this should be preferred binding
			File lib = new File(getNarManager().getNarFile(dependency)
					.getParentFile(), "nar/lib/" + getAOL() + "/"
					+ test.getLink());
			if (lib.exists()) {
				LibrarySet libset = new LibrarySet();
				libset.setProject(antProject);
				libset.setLibs(new CUtil.StringArrayBuilder(dependency
						.getArtifactId()
						+ "-" + dependency.getVersion()));
				libset.setDir(lib);
				task.addLibset(libset);
			}
		}

		// Add JVM to linker
		getJava().addRuntime(task, getJavaHome(getAOL()), getOS(),
				getAOL().getKey() + ".java.");

		// execute
		try {
			task.execute();
		} catch (BuildException e) {
			throw new MojoExecutionException("NAR: Test-Compile failed", e);
		}
	}

	protected File getTargetDirectory() {
		return new File(getMavenProject().getBuild().getDirectory(), "test-nar");
	}

}
