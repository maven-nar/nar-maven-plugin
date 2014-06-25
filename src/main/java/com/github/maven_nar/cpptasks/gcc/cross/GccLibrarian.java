package com.github.maven_nar.cpptasks.gcc.cross;
import java.io.File;


import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.LinkerParam;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractArLibrarian;
/**
 * Adapter for the 'ar' archiver
 * 
 * @author Adam Murdoch
 */
public final class GccLibrarian extends AbstractArLibrarian {
    private static String[] objFileExtensions = new String[]{".o"};
    private static GccLibrarian instance = new GccLibrarian("ar",
            objFileExtensions, false, new GccLibrarian("ar", objFileExtensions,
                    true, null));
    public static GccLibrarian getInstance() {
        return instance;
    }
    private GccLibrarian(String command, String[] inputExtensions,
            boolean isLibtool, GccLibrarian libtoolLibrarian) {
        super(command, "V", inputExtensions, new String[0], "lib", ".a",
                isLibtool, libtoolLibrarian);
    }
    protected Object clone() throws CloneNotSupportedException {
        GccLibrarian clone = (GccLibrarian) super.clone();
        return clone;
    }
    public Linker getLinker(LinkType type) {
        return GccLinker.getInstance().getLinker(type);
    }
    public void link(CCTask task, File outputFile, String[] sourceFiles,
            CommandLineLinkerConfiguration config) throws BuildException {
        try {
            GccLibrarian clone = (GccLibrarian) this.clone();
            LinkerParam param = config.getParam("target");
            if (param != null)
                clone.setCommand(param.getValue() + "-" + this.getCommand());
            clone.superlink(task, outputFile, sourceFiles, config);
        } catch (CloneNotSupportedException e) {
            superlink(task, outputFile, sourceFiles, config);
        }
    }
    private void superlink(CCTask task, File outputFile, String[] sourceFiles,
            CommandLineLinkerConfiguration config) throws BuildException {
        super.link(task, outputFile, sourceFiles, config);
    }
}
