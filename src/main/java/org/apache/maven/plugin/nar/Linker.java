// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.LinkedList;

import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.LinkerDef;
import net.sf.antcontrib.cpptasks.LinkerEnum;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;
import net.sf.antcontrib.cpptasks.types.LinkerArgument;
import net.sf.antcontrib.cpptasks.types.SystemLibrarySet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;

/**
 * Linker tag
 * 
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/Linker.java 22df3eb318cc 2007/09/06 18:55:15 duns $
 */
public class Linker {

	/**
	 * The Linker Some choices are: "msvc", "g++", "CC", "icpc", ... Default is
	 * Architecture-OS-Linker specific: FIXME: table missing
	 * 
	 * @parameter expression=""
	 */
	private String name;

	/**
	 * Enables or disables incremental linking.
	 * 
	 * @parameter expression="" default-value="false"
	 * @required
	 */
	private boolean incremental = false;

	/**
	 * Enables or disables the production of a map file.
	 * 
	 * @parameter expression="" default-value="false"
	 * @required
	 */
	private boolean map = false;

	/**
	 * Options for the linker Defaults to Architecture-OS-Linker specific
	 * values. FIXME table missing
	 * 
	 * @parameter expression=""
	 */
	private List options;

    /**
     * Options for the linker as a whitespace separated list.
     * Defaults to Architecture-OS-Linker specific values.
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

	/**
	 * Adds libraries to the linker.
	 * 
	 * @parameter expression=""
	 */
	private List/* <Lib> */libs;

    /**
     * Adds libraries to the linker. Will work in combination with &lt;libs&gt;.
     * The format is comma separated, colon-delimited values (name:type:dir),
     * like "myLib:shared:/home/me/libs/, otherLib:static:/some/path".
     *
     * @parameter expression=""
     */
    private String libSet;

    /**
	 * Adds system libraries to the linker.
	 * 
	 * @parameter expression=""
	 */
	private List/* <SysLib> */sysLibs;

    /**
     * Adds system libraries to the linker. Will work in combination with &lt;sysLibs&gt;.
     * The format is comma separated, colon-delimited values (name:type),
     * like "dl:shared, pthread:shared".
     *
     * @parameter expression=""
     */
    private String sysLibSet;

    /**
     * <p>
     * Specifies the link ordering of libraries that come from nar dependencies. The format is
     * a comma separated list of dependency names, given as groupId:artifactId.
     * </p>
     *
     * <p>
     * Example: &lt;narDependencyLibOrder&gt;someGroup:myProduct, other.group:productB&lt;narDependencyLibOrder&gt;
     * </p>
     *
     * @parameter expression="" 
     */
    private String narDependencyLibOrder;


    public Linker() {
		// default constructor for use as TAG
	}

	/**
	 * For use with specific named linker.
	 * 
	 * @param name
	 */
	public Linker(String name) {
		this.name = name;
	}

	public String getName(Properties defaults, String prefix)
			throws MojoFailureException {
		if ((name == null) && (defaults != null) && (prefix != null)) {
			name = defaults.getProperty(prefix + "linker");
		}
		if (name == null) {
			throw new MojoFailureException(
					"NAR: Please specify a <Name> as part of <Linker>");
		}
		return name;
	}

	public LinkerDef getLinker(AbstractCompileMojo mojo, Project antProject,
			String os, String prefix, String type) throws MojoFailureException,
			MojoExecutionException {
		if (name == null) {
			throw new MojoFailureException(
					"NAR: Please specify a <Name> as part of <Linker>");
		}

		LinkerDef linker = new LinkerDef();
		linker.setProject(antProject);
		LinkerEnum linkerEnum = new LinkerEnum();
		linkerEnum.setValue(name);
		linker.setName(linkerEnum);

		// incremental, map
		linker.setIncremental(incremental);
		linker.setMap(map);

		// Add definitions (Window only)
		if (os.equals(OS.WINDOWS)
				&& (type.equals(Library.SHARED) || type.equals(Library.JNI))) {
			Set defs = new HashSet();
			try {
				File cSrcDir = mojo.getC().getSourceDirectory();
				if (cSrcDir.exists())
					defs.addAll(FileUtils.getFiles(cSrcDir, "**/*.def", null));
			} catch (IOException e) {
			}
			try {
				File cppSrcDir = mojo.getCpp().getSourceDirectory();
				if (cppSrcDir.exists())
					defs
							.addAll(FileUtils.getFiles(cppSrcDir, "**/*.def",
									null));
			} catch (IOException e) {
			}
			try {
				File fortranSrcDir = mojo.getFortran().getSourceDirectory();
				if (fortranSrcDir.exists())
					defs.addAll(FileUtils.getFiles(fortranSrcDir, "**/*.def",
							null));
			} catch (IOException e) {
			}

			for (Iterator i = defs.iterator(); i.hasNext();) {
				LinkerArgument arg = new LinkerArgument();
				arg.setValue("/def:" + (File) i.next());
				linker.addConfiguredLinkerArg(arg);
			}
		}

		// Add options to linker
		if (options != null) {
			for (Iterator i = options.iterator(); i.hasNext();) {
				LinkerArgument arg = new LinkerArgument();
				arg.setValue((String) i.next());
				linker.addConfiguredLinkerArg(arg);
			}
		}

        if (optionSet != null) {

            String[] opts = optionSet.split("\\s");

            for (int i = 0; i < opts.length; i++) {

                LinkerArgument arg = new LinkerArgument();

                arg.setValue(opts[i]);
                linker.addConfiguredLinkerArg(arg);
            }
        }

        if (!clearDefaultOptions) {
			String options = NarUtil.getDefaults().getProperty(
					prefix + "options");
			if (options != null) {
				String[] option = options.split(" ");
				for (int i = 0; i < option.length; i++) {
					LinkerArgument arg = new LinkerArgument();
					arg.setValue(option[i]);
					linker.addConfiguredLinkerArg(arg);
				}
			}
		}

        // record the preference for nar dependency library link order
        if (narDependencyLibOrder != null) {

            List libOrder = new LinkedList();

            String[] libs = narDependencyLibOrder.split(",");

            for (int i = 0; i < libs.length; i++) {
                libOrder.add(libs[i].trim());
            }

            mojo.setDependencyLibOrder(libOrder);
        }

        // Add Libraries to linker
		if ((libs != null) || (libSet != null)) {

            if (libs != null) {

                for (Iterator i = libs.iterator(); i.hasNext();) {

                    Lib lib = (Lib) i.next();
                    lib.addLibSet(mojo, linker, antProject);
                }
            }

            if (libSet != null) {
                addLibraries(libSet, linker, antProject, false);
            }
        }
        else {

            String libsList = NarUtil.getDefaults()
					.getProperty(prefix + "libs");

            addLibraries(libsList, linker, antProject, false);
        }

        // Add System Libraries to linker
        if ((sysLibs != null) || (sysLibSet != null)) {

            if (sysLibs != null) {

                for (Iterator i = sysLibs.iterator(); i.hasNext();) {

                    SysLib sysLib = (SysLib) i.next();
                    linker.addSyslibset(sysLib.getSysLibSet(antProject));
                }
            }

            if (sysLibSet != null) {
                addLibraries(sysLibSet, linker, antProject, true);
            }
        }
        else {

            String sysLibsList = NarUtil.getDefaults().getProperty(
                    prefix + "sysLibs");

            addLibraries(sysLibsList, linker, antProject, true);
        }

		return linker;
	}


    private void addLibraries(String libraryList, LinkerDef linker, Project antProject, boolean isSystem) {

        if (libraryList == null) {
            return;
        }

        String[] lib = libraryList.split(",");

        for (int i = 0; i < lib.length; i++) {

            String[] libInfo = lib[i].trim().split(":", 3);

            LibrarySet librarySet = new LibrarySet();

            if (isSystem) {
                librarySet = new SystemLibrarySet();
            }

            librarySet.setProject(antProject);
            librarySet.setLibs(new CUtil.StringArrayBuilder(libInfo[0]));

            if (libInfo.length > 1) {

                LibraryTypeEnum libType = new LibraryTypeEnum();

                libType.setValue(libInfo[1]);
                librarySet.setType(libType);

                if (!isSystem && (libInfo.length > 2)) {
                    librarySet.setDir(new File(libInfo[2]));
                }
            }

            if (!isSystem) {
                linker.addLibset(librarySet);
            }
            else {
                linker.addSyslibset((SystemLibrarySet)librarySet);
            }
        }
    }
}
