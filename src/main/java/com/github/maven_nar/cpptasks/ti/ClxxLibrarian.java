package com.github.maven_nar.cpptasks.ti;
import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;


/**
 * 
 * Adapter for TI DSP librarian
 *  *
 * @author CurtA
 */
public class ClxxLibrarian extends CommandLineLinker {
    private static final ClxxLibrarian cl55Instance = new ClxxLibrarian("ar55");
    private static final ClxxLibrarian cl6xInstance = new ClxxLibrarian("ar6x");
    public static final ClxxLibrarian getCl55Instance() {
        return cl55Instance;
    }
    public static final ClxxLibrarian getCl6xInstance() {
        return cl6xInstance;
    }
    private ClxxLibrarian(String command) {
        super(command, null, new String[]{".o"}, new String[0], ".lib", false,
                null);
    }
    protected void addBase(long base, Vector args) {
        // TODO Auto-generated method stub
    }
    protected void addFixed(Boolean fixed, Vector args) {
        // TODO Auto-generated method stub
    }
    protected void addImpliedArgs(boolean debug, LinkType linkType, Vector args) {
        // TODO Auto-generated method stub
    }
    protected void addIncremental(boolean incremental, Vector args) {
        // TODO Auto-generated method stub
    }
    protected void addMap(boolean map, Vector args) {
        // TODO Auto-generated method stub
    }
    protected void addStack(int stack, Vector args) {
        // TODO Auto-generated method stub
    }
    protected void addEntry(String entry, Vector args) {
    }
    
    protected String getCommandFileSwitch(String commandFile) {
        return "@" + commandFile;
    }
    public File[] getLibraryPath() {
        return new File[0];
    }
    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        return new String[0];
    }
    public Linker getLinker(LinkType linkType) {
        return null;
    }
    protected int getMaximumCommandLength() {
        return 1024;
    }
    protected String[] getOutputFileSwitch(String outputFile) {
        return new String[]{"-o", outputFile};
    }
    public boolean isCaseSensitive() {
        // TODO Auto-generated method stub
        return false;
    }
}
