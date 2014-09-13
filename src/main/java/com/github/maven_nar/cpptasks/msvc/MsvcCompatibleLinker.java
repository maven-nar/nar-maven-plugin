/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.msvc;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.TargetMatcher;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.platforms.WindowsPlatform;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * Abstract base class for linkers that try to mimic the command line arguments
 * for the Microsoft (r) Incremental Linker
 * 
 * @author Curt Arnold
 */
public abstract class MsvcCompatibleLinker extends CommandLineLinker {
    public MsvcCompatibleLinker(String command, String identifierArg,
            String outputSuffix) {
        super(command, identifierArg, new String[]{".obj", ".lib", ".res"},
                new String[]{".map", ".pdb", ".lnk", ".dll", ".tlb", ".rc", ".h"}, outputSuffix,
                false, null);
    }
    protected void addBase(CCTask task, long base, Vector<String> args) {
        if (base >= 0) {
            String baseAddr = Long.toHexString(base);
            args.addElement("/BASE:0x" + baseAddr);
        }
    }
    protected void addFixed(CCTask task, Boolean fixed, Vector<String> args) {
        if (fixed != null) {
            if (fixed.booleanValue()) {
                args.addElement("/FIXED");
            } else {
                args.addElement("/FIXED:NO");
            }
        }
    }
    protected void addImpliedArgs(CCTask task, boolean debug, LinkType linkType, Vector<String> args) {
        args.addElement("/NOLOGO");
        if (debug) {
            args.addElement("/DEBUG");
        }
        if (linkType.isSharedLibrary()) {
            args.addElement("/DLL");
        }
        //
        //  The following lines were commented out
        //   from v 1.5 to v 1.12 with no explanation
        //
         if(linkType.isSubsystemGUI()) {
           args.addElement("/SUBSYSTEM:WINDOWS"); } else {
         if(linkType.isSubsystemConsole()) {
           args.addElement("/SUBSYSTEM:CONSOLE"); } }
    }
    protected void addIncremental(CCTask task, boolean incremental, Vector<String> args) {
        if (incremental) {
            args.addElement("/INCREMENTAL:YES");
        } else {
            args.addElement("/INCREMENTAL:NO");
        }
    }
    protected void addMap(CCTask task, boolean map, Vector<String> args) {
        if (map) {
            args.addElement("/MAP");
        }
    }
    protected void addStack(CCTask task, int stack, Vector<String> args) {
        if (stack >= 0) {
            String stackStr = Integer.toHexString(stack);
            args.addElement("/STACK:0x" + stackStr);
        }
    }
    protected void addEntry(CCTask task, String entry, Vector<String> args) {
    	if (entry != null) {
    		args.addElement("/ENTRY:" + entry);
    	}
    }
    
    public String getCommandFileSwitch(String commandFile) {
        return "@" + commandFile;
    }
    public File[] getLibraryPath() {
        return CUtil.getPathFromEnvironment("LIB", ";");
    }
    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        StringBuffer buf = new StringBuffer();
        String[] patterns = new String[libnames.length];
        for (int i = 0; i < libnames.length; i++) {
            buf.setLength(0);
            buf.append(libnames[i]);
            buf.append(".lib");
            patterns[i] = buf.toString();
        }
        return patterns;
    }
    public int getMaximumCommandLength() {
// FREEHEP stay on the safe side
        return 32000; // 32767;
    }
    public String[] getOutputFileSwitch(String outputFile) {
        return new String[]{"/OUT:" + outputFile};
    }
    public boolean isCaseSensitive() {
        return false;
    }
    
    /**
     * Adds source or object files to the bidded fileset to
     * support version information.
     * 
     * @param versionInfo version information
     * @param linkType link type
     * @param isDebug true if debug build
     * @param outputFile name of generated executable
     * @param objDir directory for generated files
     * @param matcher bidded fileset
     */
	public void addVersionFiles(final VersionInfo versionInfo, 
			final LinkType linkType,
			final File outputFile,
			final boolean isDebug,
			final File objDir, 
			final TargetMatcher matcher) throws IOException {
		WindowsPlatform.addVersionFiles(versionInfo, linkType, outputFile, isDebug, objDir, matcher);
	}
}
