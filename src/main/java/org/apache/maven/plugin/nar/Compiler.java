// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.CompilerDef;
import net.sf.antcontrib.cpptasks.CompilerEnum;
import net.sf.antcontrib.cpptasks.OptimizationEnum;
import net.sf.antcontrib.cpptasks.types.CompilerArgument;
import net.sf.antcontrib.cpptasks.types.ConditionalFileSet;
import net.sf.antcontrib.cpptasks.types.DefineArgument;
import net.sf.antcontrib.cpptasks.types.DefineSet;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Abstract Compiler class
 * 
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/Compiler.java 0ee9148b7c6a 2007/09/20 18:42:29 duns $
 */
public abstract class Compiler {

	/**
	 * The name of the compiler Some choices are: "msvc", "g++", "gcc", "CC",
	 * "cc", "icc", "icpc", ... Default is Architecture-OS-Linker specific:
	 * FIXME: table missing
	 * 
	 * @parameter expression=""
	 */
	private String name;

	/**
	 * Source directory for native files
	 * 
	 * @parameter expression="${basedir}/src/main"
	 * @required
	 */
	private File sourceDirectory;

	/**
	 * Include patterns for sources
	 * 
	 * @parameter expression=""
	 * @required
	 */
	private Set includes = new HashSet();

	/**
	 * Exclude patterns for sources
	 * 
	 * @parameter expression=""
	 * @required
	 */
	private Set excludes = new HashSet();

	/**
	 * Compile with debug information.
	 * 
	 * @parameter expression="" default-value="false"
	 * @required
	 */
	private boolean debug = false;

	/**
	 * Enables generation of exception handling code.
	 * 
	 * @parameter expression="" default-value="true"
	 * @required
	 */
	private boolean exceptions = true;

	/**
	 * Enables run-time type information.
	 * 
	 * @parameter expression="" default-value="true"
	 * @required
	 */
	private boolean rtti = true;

	/**
	 * Sets optimization. Possible choices are: "none", "size", "minimal",
	 * "speed", "full", "aggressive", "extreme", "unsafe".
	 * 
	 * @parameter expression="" default-value="none"
	 * @required
	 */
	private String optimize = "none";

	/**
	 * Enables or disables generation of multi-threaded code. Default value:
	 * false, except on Windows.
	 * 
	 * @parameter expression="" default-value="false"
	 * @required
	 */
	private boolean multiThreaded = false;

	/**
	 * Defines
	 * 
	 * @parameter expression=""
	 */
	private List defines;

    /**
     * Defines for the compiler as a comma separated list of name[=value] pairs, where the value is optional.
     * Will work in combination with &lt;defines&gt;.
     *
     * @parameter expression=""
     */
    private String defineSet;

    /**
	 * Clears default defines
	 * 
	 * @parameter expression="" default-value="false"
	 * @required
	 */
	private boolean clearDefaultDefines;

	/**
	 * Undefines
	 * 
	 * @parameter expression=""
	 */
	private List undefines;

    /**
     * Undefines for the compiler as a comma separated list of name[=value] pairs where the value is optional.
     * Will work in combination with &lt;undefines&gt;.
     *
     * @parameter expression=""
     */
    private String undefineSet;

    /**
	 * Clears default undefines
	 * 
	 * @parameter expression="" default-value="false"
	 * @required
	 */
	private boolean clearDefaultUndefines;

	/**
	 * Include Paths. Defaults to "${sourceDirectory}/include"
	 * 
	 * @parameter expression=""
	 */
	private List includePaths;

	/**
	 * System Include Paths, which are added at the end of all include paths
	 * 
	 * @parameter expression=""
	 */
	private List systemIncludePaths;

	/**
	 * Additional options for the C++ compiler Defaults to
	 * Architecture-OS-Linker specific values. FIXME table missing
	 * 
	 * @parameter expression=""
	 */
	private List options;

    /**
     * Options for the compiler as a whitespace separated list.
     * Will work in combination with &lt;options&gt;.
     *
     * @parameter expression=""
     */
    private String optionSet;

    /**
	 * Clears default options
	 * 
	 * @parameter expression="" default-value="false"
	 * @required
	 */
	private boolean clearDefaultOptions;

	private AbstractCompileMojo mojo;

	protected Compiler() {
	}

	public void setAbstractCompileMojo(AbstractCompileMojo mojo) {
		this.mojo = mojo;
	}

	public File getSourceDirectory() {
		return getSourceDirectory("dummy");
	}

	protected File getSourceDirectory(String type) {
		if (sourceDirectory == null) {
			sourceDirectory = new File(mojo.getMavenProject().getBasedir(),
					"src/" + (type.equals("test") ? "test" : "main"));
		}
		return sourceDirectory;
	}

	protected List/* <String> */getIncludePaths(String type) {
		if (includePaths == null || (includePaths.size() == 0)) {
			includePaths = new ArrayList();
			includePaths.add(new File(getSourceDirectory(type), "include")
					.getPath());
		}
		return includePaths;
	}

	public Set getIncludes() throws MojoFailureException {
		return getIncludes("main");
	}

	protected Set getIncludes(String type) throws MojoFailureException {
		Set result = new HashSet();
		if (!type.equals("test") && !includes.isEmpty()) {
			result.addAll(includes);
		} else {
			String defaultIncludes = NarUtil.getDefaults().getProperty(
					getPrefix() + "includes");
			if (defaultIncludes == null) {
				throw new MojoFailureException(
						"NAR: Please specify <Includes> as part of <Cpp>, <C> or <Fortran> for "
								+ getPrefix());
			}

			String[] include = defaultIncludes.split(" ");
			for (int i = 0; i < include.length; i++) {
				result.add(include[i].trim());
			}
		}
		return result;
	}

	protected Set getExcludes() throws MojoFailureException {
		Set result = new HashSet();

		// add all excludes
		if (excludes.isEmpty()) {
			String defaultExcludes = NarUtil.getDefaults().getProperty(
					getPrefix() + "excludes");
			if (defaultExcludes != null) {
				String[] exclude = defaultExcludes.split(" ");
				for (int i = 0; i < exclude.length; i++) {
					result.add(exclude[i].trim());
				}
			}
		} else {
			result.addAll(excludes);
		}

		return result;
	}

	protected String getPrefix() throws MojoFailureException {
		return mojo.getAOL().getKey() + "." + getName() + ".";
	}

	public CompilerDef getCompiler(String type, String output)
			throws MojoFailureException {

		// adjust default values
		if (name == null)
			name = NarUtil.getDefaults().getProperty(getPrefix() + "compiler");
		if (name == null) {
			throw new MojoFailureException(
					"NAR: Please specify <Name> as part of <Cpp>, <C> or <Fortran> for "
							+ getPrefix());
		}

		CompilerDef compiler = new CompilerDef();
		compiler.setProject(mojo.getAntProject());
		CompilerEnum compilerName = new CompilerEnum();
		compilerName.setValue(name);
		compiler.setName(compilerName);

		// debug, exceptions, rtti, multiThreaded
		compiler.setDebug(debug);
		compiler.setExceptions(exceptions);
		compiler.setRtti(rtti);
		compiler.setMultithreaded(mojo.getOS().equals("Windows") ? true
				: multiThreaded);

		// optimize
		OptimizationEnum optimization = new OptimizationEnum();
		optimization.setValue(optimize);
		compiler.setOptimize(optimization);

		// add options
		if (options != null) {
			for (Iterator i = options.iterator(); i.hasNext();) {
				CompilerArgument arg = new CompilerArgument();
				arg.setValue((String) i.next());
				compiler.addConfiguredCompilerArg(arg);
			}
		}

        if (optionSet != null) {

            String[] opts = optionSet.split("\\s");

            for (int i = 0; i < opts.length; i++) {

                CompilerArgument arg = new CompilerArgument();

                arg.setValue(opts[i]);
                compiler.addConfiguredCompilerArg(arg);
            }
        }

        if (!clearDefaultOptions) {
			String optionsProperty = NarUtil.getDefaults().getProperty(
					getPrefix() + "options");
			if (optionsProperty != null) {
				String[] option = optionsProperty.split(" ");
				for (int i = 0; i < option.length; i++) {
					CompilerArgument arg = new CompilerArgument();
					arg.setValue(option[i]);
					compiler.addConfiguredCompilerArg(arg);
				}
			}
		}

		// add defines
		if (defines != null) {
			DefineSet defineSet = new DefineSet();
			for (Iterator i = defines.iterator(); i.hasNext();) {
				DefineArgument define = new DefineArgument();
				String[] pair = ((String) i.next()).split("=", 2);
				define.setName(pair[0]);
				define.setValue(pair.length > 1 ? pair[1] : null);
				defineSet.addDefine(define);
			}
			compiler.addConfiguredDefineset(defineSet);
		}

        if (defineSet != null) {

            String[] defList = defineSet.split(",");
            DefineSet defSet = new DefineSet();

            for (int i = 0; i < defList.length; i++) {

                String[] pair = defList[i].trim().split("=", 2);
                DefineArgument def = new DefineArgument();

                def.setName(pair[0]);
                def.setValue(pair.length > 1 ? pair[1] : null);

                defSet.addDefine(def);
            }

            compiler.addConfiguredDefineset(defSet);
        }

        if (!clearDefaultDefines) {
			DefineSet defineSet = new DefineSet();
			String defaultDefines = NarUtil.getDefaults().getProperty(
					getPrefix() + "defines");
			if (defaultDefines != null) {
				defineSet
						.setDefine(new CUtil.StringArrayBuilder(defaultDefines));
			}
			compiler.addConfiguredDefineset(defineSet);
		}

		// add undefines
		if (undefines != null) {
			DefineSet undefineSet = new DefineSet();
			for (Iterator i = undefines.iterator(); i.hasNext();) {
				DefineArgument undefine = new DefineArgument();
				String[] pair = ((String) i.next()).split("=", 2);
				undefine.setName(pair[0]);
				undefine.setValue(pair.length > 1 ? pair[1] : null);
				undefineSet.addUndefine(undefine);
			}
			compiler.addConfiguredDefineset(undefineSet);
		}

        if (undefineSet != null) {

            String[] undefList = undefineSet.split(",");
            DefineSet undefSet = new DefineSet();

            for (int i = 0; i < undefList.length; i++) {

                String[] pair = undefList[i].trim().split("=", 2);
                DefineArgument undef = new DefineArgument();

                undef.setName(pair[0]);
                undef.setValue(pair.length > 1 ? pair[1] : null);

                undefSet.addUndefine(undef);
            }

            compiler.addConfiguredDefineset(undefSet);
        }

        if (!clearDefaultUndefines) {
			DefineSet undefineSet = new DefineSet();
			String defaultUndefines = NarUtil.getDefaults().getProperty(
					getPrefix() + "undefines");
			if (defaultUndefines != null) {
				undefineSet.setUndefine(new CUtil.StringArrayBuilder(
						defaultUndefines));
			}
			compiler.addConfiguredDefineset(undefineSet);
		}

		// add include path
		for (Iterator i = getIncludePaths(type).iterator(); i.hasNext();) {
			String path = (String) i.next();
			compiler.createIncludePath().setPath(path);
		}

		// add system include path (at the end)
		if (systemIncludePaths != null) {
			for (Iterator i = systemIncludePaths.iterator(); i.hasNext();) {
				String path = (String) i.next();
				compiler.createSysIncludePath().setPath(path);
			}
		}

		// Add default fileset (if exists)
		File srcDir = getSourceDirectory(type);
		Set includes = getIncludes();
		Set excludes = getExcludes();

		// now add all but the current test to the excludes
		for (Iterator i = mojo.getTests().iterator(); i.hasNext();) {
			Test test = (Test) i.next();
			if (!test.getName().equals(output)) {
				excludes.add("**/" + test.getName() + ".*");
			}
		}

		mojo.getLog().debug(
				"Checking for existence of " + getName() + " sourceDirectory: "
						+ srcDir);
		if (srcDir.exists()) {
			ConditionalFileSet fileSet = new ConditionalFileSet();
			fileSet.setProject(mojo.getAntProject());
			fileSet.setIncludes(StringUtils.join(includes.iterator(), ","));
			fileSet.setExcludes(StringUtils.join(excludes.iterator(), ","));
			fileSet.setDir(srcDir);
			compiler.addFileset(fileSet);
		}

		// add other sources
		if (!type.equals("test")) {
			for (Iterator i = mojo.getMavenProject().getCompileSourceRoots()
					.iterator(); i.hasNext();) {
				File dir = new File((String) i.next());
				mojo.getLog().debug(
						"Checking for existence of " + getName()
								+ " sourceCompileRoot: " + dir);
				if (dir.exists()) {
					ConditionalFileSet otherFileSet = new ConditionalFileSet();
					otherFileSet.setProject(mojo.getAntProject());
					otherFileSet.setIncludes(StringUtils.join(includes
							.iterator(), ","));
					otherFileSet.setExcludes(StringUtils.join(excludes
							.iterator(), ","));
					otherFileSet.setDir(dir);
					compiler.addFileset(otherFileSet);
				}
			}
		}
		return compiler;
	}

	protected abstract String getName();

	public void copyIncludeFiles(MavenProject mavenProject, File targetDirectory)
			throws IOException {
		for (Iterator i = getIncludePaths("dummy").iterator(); i.hasNext();) {
			File path = new File((String) i.next());
			if (path.exists()) {
				NarUtil.copyDirectoryStructure(path, targetDirectory, null,
						NarUtil.DEFAULT_EXCLUDES);
			}
		}
	}
}
