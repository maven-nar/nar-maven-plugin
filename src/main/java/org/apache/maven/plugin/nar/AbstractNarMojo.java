// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;


/**
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/AbstractNarMojo.java 0ee9148b7c6a 2007/09/20 18:42:29 duns $
 */
public abstract class AbstractNarMojo extends AbstractMojo implements NarConstants {
    
    /**
     * Skip running of NAR plugins (any) altogether.
     * 
     * @parameter expression="${nar.skip}" default-value="false"
     */
    private boolean skip;
    
    /**
     * The Architecture for the nar,
     * Some choices are: "x86", "i386", "amd64", "ppc", "sparc", ...
     * Defaults to a derived value from ${os.arch}
     *
     * @parameter expression="${os.arch}"
     * @required
     */
    private String architecture;

    /**
     * The Operating System for the nar.
     * Some choices are: "Windows", "Linux", "MacOSX", "SunOS", ...
     * Defaults to a derived value from ${os.name}
     * FIXME table missing
     *
     * @parameter expression=""
     */
    private String os;

    /**
     * Architecture-OS-Linker name.
     * Defaults to: arch-os-linker.
     *
     * @parameter expression=""
     */
    private String aol;

    /**
     * Linker
     *
     * @parameter expression=""
     */
    private Linker linker;
     
    /**
     * @parameter expression="${project.build.directory}"
     * @readonly
     */
    private File outputDirectory;

    /**
     * @parameter expression="${project.build.finalName}"
     * @readonly
     */
    private String finalName;

    /**
     * Target directory for Nar file construction
     * Defaults to "${project.build.directory}/nar" for "nar-compile" goal
     * Defaults to "${project.build.directory}/test-nar" for "nar-testCompile" goal
     *
     * @parameter expression=""
     */
    private File targetDirectory;
     
    /**
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject mavenProject;
 
    
    protected boolean shouldSkip() {
    	return skip;
    }
    
    protected String getArchitecture() {
    	architecture = NarUtil.getArchitecture(architecture);
        return architecture;
    }
    
    protected String getOS() {
    	os = NarUtil.getOS(os);    	
        return os;
    }
    
    protected AOL getAOL() throws MojoFailureException {
    	return NarUtil.getAOL(architecture, os, linker, aol);    	
    }
    
    protected Linker getLinker() {
    	linker = NarUtil.getLinker(linker);
        return linker;
    }
    
    protected File getOutputDirectory() {
        return outputDirectory;
    }
   
    protected String getFinalName() {
        return finalName;
    }
    
    protected File getTargetDirectory() {
        if (targetDirectory == null) {
            targetDirectory = new File(mavenProject.getBuild().getDirectory(), "nar");
        }
        return targetDirectory;
    }
              
    protected MavenProject getMavenProject() {
        return mavenProject;
    }
}
