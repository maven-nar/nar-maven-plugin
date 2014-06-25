package com.github.maven_nar.cpptasks.arm;
import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;


/**
 * Adapter for the ARM Linker
 * 
 * @author CurtA
 */
public class ADSLinker extends CommandLineLinker {
    private static final ADSLinker dllInstance = new ADSLinker(".o");
    private static final ADSLinker instance = new ADSLinker(".axf");
    public static ADSLinker getDllInstance() {
        return dllInstance;
    }
    public static ADSLinker getInstance() {
        return instance;
    }
    private ADSLinker(String outputSuffix) {
        super("armlink", "-vsn", new String[]{".o", ".lib", ".res"},
                new String[]{".map", ".pdb", ".lnk"}, outputSuffix, false, null);
    }
    protected void addBase(long base, Vector args) {
        // TODO Auto-generated method stub
    }
    protected void addFixed(Boolean fixed, Vector args) {
        // TODO Auto-generated method stub
    }
    protected void addImpliedArgs(boolean debug, LinkType linkType, Vector args) {
        if (debug) {
            args.addElement("-debug");
        }
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
        // TODO Auto-generated method stub

    }
    
    /**
     * May have to make this String array return
     * 
     * @see com.github.maven_nar.cpptasks.compiler.CommandLineLinker#getCommandFileSwitch(java.lang.String)
     */
    protected String getCommandFileSwitch(String commandFile) {
        return "-via" + commandFile;
    }
    public File[] getLibraryPath() {
        return CUtil.getPathFromEnvironment("ARMLIB", ";");
    }
    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
    	//
    	//  TODO: looks like bad extension
    	//
        return new String[]{".o"};
    }
    public Linker getLinker(LinkType linkType) {
        return this;
    }
    protected int getMaximumCommandLength() {
        return 1024;
    }
    protected String[] getOutputFileSwitch(String outputFile) {
        return new String[]{"-output", outputFile};
    }
    public boolean isCaseSensitive() {
        return false;
    }
}
