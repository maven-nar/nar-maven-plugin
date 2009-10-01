// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.types.CommandLineArgument;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LinkerArgument;

import org.apache.maven.plugin.MojoFailureException;

/**
 * Java specifications for NAR
 *
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/Java.java 0ee9148b7c6a 2007/09/20 18:42:29 duns $
 */
public class Java {

    /**
     * Add Java includes to includepath
     *
     * @parameter expression="" default-value="false"
     * @required
     */
    private boolean include = false;

    /**
     * Java Include Paths, relative to a derived ${java.home}.
     * Defaults to: "${java.home}/include" and "${java.home}/include/<i>os-specific</i>".
     *
     * @parameter expression=""
     */
    private List includePaths;

    /**
     * Add Java Runtime to linker
     *
     * @parameter expression="" default-value="false"
     * @required
     */
    private boolean link = false;

    /**
     * Relative path from derived ${java.home} to the java runtime to link with
     * Defaults to Architecture-OS-Linker specific value.
     * FIXME table missing
     *
     * @parameter expression=""
     */
    private String runtimeDirectory;
     
    /**
     * Name of the runtime
     *
     * @parameter expression="" default-value="jvm"
     */
    private String runtime = "jvm";
    
    private AbstractCompileMojo mojo;
    
    public Java() {
    }
    
	public void setAbstractCompileMojo(AbstractCompileMojo mojo) {
		this.mojo = mojo;
	}    
    
    public void addIncludePaths(CCTask task, String outType) throws MojoFailureException {
        if (include || mojo.getJavah().getJniDirectory().exists()) {
            if (includePaths != null) {
                for (Iterator i=includePaths.iterator(); i.hasNext(); ) {
                    String path = (String)i.next();
                    task.createIncludePath().setPath(new File(mojo.getJavaHome(mojo.getAOL()), path).getPath());
                }
            } else {
                String prefix = mojo.getAOL().getKey()+".java.";
                String includes = NarUtil.getDefaults().getProperty(prefix+"include");
                if (includes != null) {
                    String[] path = includes.split(";");
                    for (int i=0; i<path.length; i++) {
                        task.createIncludePath().setPath(new File(mojo.getJavaHome(mojo.getAOL()), path[i]).getPath());
                    }
                }
            }
        }
    }
    
    public void addRuntime(CCTask task, File javaHome, String os, String prefix) throws MojoFailureException {
        if (link) {
            if (os.equals(OS.MACOSX)) {
                CommandLineArgument.LocationEnum end = new CommandLineArgument.LocationEnum();
                end.setValue("end");
                
                // add as argument rather than library to avoid argument quoting
                LinkerArgument framework = new LinkerArgument();
                framework.setValue("-framework");
                framework.setLocation(end);
                task.addConfiguredLinkerArg(framework);

                LinkerArgument javavm = new LinkerArgument();
                javavm.setValue("JavaVM");
                javavm.setLocation(end);
                task.addConfiguredLinkerArg(javavm);                
            } else {
                if (runtimeDirectory == null) {
                    runtimeDirectory = NarUtil.getDefaults().getProperty(prefix+"runtimeDirectory");
                    if (runtimeDirectory == null) {
                        throw new MojoFailureException("NAR: Please specify a <RuntimeDirectory> as part of <Java>");
                    }
                }
                mojo.getLog().debug("Using Java Rumtime Directory: "+runtimeDirectory);
                
                LibrarySet libset = new LibrarySet();
                libset.setProject(mojo.getAntProject());
                libset.setLibs(new CUtil.StringArrayBuilder(runtime));
                libset.setDir(new File(javaHome, runtimeDirectory));
                task.addLibset(libset);
            }
        }
    }    
}
