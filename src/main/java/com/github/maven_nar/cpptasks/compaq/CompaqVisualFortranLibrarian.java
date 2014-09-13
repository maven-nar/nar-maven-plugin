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
package com.github.maven_nar.cpptasks.compaq;
import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.msvc.MsvcLibrarian;
import com.github.maven_nar.cpptasks.msvc.MsvcProcessor;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

import java.io.File;
import java.util.Vector;

/**
 * Adapter for the Compaq(r) Visual Fortran Librarian
 * 
 * @author Curt Arnold
 */
public class CompaqVisualFortranLibrarian extends CommandLineLinker {
    private static final CompaqVisualFortranLibrarian instance = new CompaqVisualFortranLibrarian();
    public static CompaqVisualFortranLibrarian getInstance() {
        return instance;
    }
    private CompaqVisualFortranLibrarian() {
        super("lib", "/bogus", new String[]{".obj"}, new String[0], ".lib",
                false, null);
    }

    protected void addImpliedArgs(CCTask task, boolean debug, LinkType linkType, Vector<String> args) {
        args.addElement("/nologo");
    }
    protected String getCommandFileSwitch(String commandFile) {
        return MsvcProcessor.getCommandFileSwitch(commandFile);
    }
    public File[] getLibraryPath() {
        return new File[0];
    }
    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        return new String[0];
    }
    public Linker getLinker(LinkType type) {
        return CompaqVisualFortranLinker.getInstance().getLinker(type);
    }
    protected int getMaximumCommandLength() {
        return MsvcLibrarian.getInstance().getMaximumCommandLength();
    }
    protected String[] getOutputFileSwitch(String outputFile) {
        return MsvcLibrarian.getInstance().getOutputFileSwitch(outputFile);
    }
    public boolean isCaseSensitive() {
        return false;
    }
}
