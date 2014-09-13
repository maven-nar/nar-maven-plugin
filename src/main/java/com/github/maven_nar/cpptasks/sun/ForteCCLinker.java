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
package com.github.maven_nar.cpptasks.sun;
import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractLdLinker;

/**
 * Adapter for Sun (r) Forte(tm) C++ Linker
 * 
 * @author Curt Arnold
 */
public final class ForteCCLinker extends AbstractLdLinker {
    private static final String[] discardFiles = new String[]{".dll", ".so",
    ".sl"};
    private static final String[] objFiles = new String[]{".o", ".a", ".lib"};
    private static final ForteCCLinker arLinker = new ForteCCLinker("CC",
            objFiles, discardFiles, "lib", ".a");
    private static final ForteCCLinker dllLinker = new ForteCCLinker("CC",
            objFiles, discardFiles, "lib", ".so");
    private static final ForteCCLinker instance = new ForteCCLinker("CC",
            objFiles, discardFiles, "", "");
    public static ForteCCLinker getInstance() {
        return instance;
    }
    private File[] libDirs;
    private ForteCCLinker(String command, String[] extensions,
            String[] ignoredExtensions, String outputPrefix, String outputSuffix) {
        super(command, "-V", extensions, ignoredExtensions, outputPrefix,
                outputSuffix, false, null);
    }
    public void addImpliedArgs(boolean debug, LinkType linkType, Vector<String> args) {
        if (debug) {
            args.addElement("-g");
        }
        if (linkType.isStaticRuntime()) {
// FREEHEP changed -static
            args.addElement("-staticlib=%all");
        }
        if (linkType.isSharedLibrary()) {
            args.addElement("-G");
        }
        if (linkType.isStaticLibrary()) {
            args.addElement("-xar");
        }
    }
    public void addIncremental(boolean incremental, Vector<String> args) {
        /*
         * if (incremental) { args.addElement("-xidlon"); } else {
         * args.addElement("-xidloff"); }
         */
    }
    /**
     * Returns library path.
     *  
     */
    public File[] getLibraryPath() {
        if (libDirs == null) {
            File CCloc = CUtil.getExecutableLocation("CC");
            if (CCloc != null) {
                File compilerLib = new File(new File(CCloc, "../lib")
                        .getAbsolutePath());
                if (compilerLib.exists()) {
                    libDirs = new File[2];
                    libDirs[0] = compilerLib;
                }
            }
            if (libDirs == null) {
                libDirs = new File[1];
            }
        }
        libDirs[libDirs.length - 1] = new File("/usr/lib");
        return libDirs;
    }
    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return arLinker;
        }
        if (type.isSharedLibrary()) {
            return dllLinker;
        }
        return instance;
    }
}
