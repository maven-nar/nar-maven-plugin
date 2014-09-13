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
import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

import java.io.File;
import java.util.Vector;

/**
 * Abstract base adapter for librarians with command line options compatible
 * with the Microsoft(r) Library Manager
 * 
 * @author Curt Arnold
 */
public abstract class MsvcCompatibleLibrarian extends CommandLineLinker {
    public MsvcCompatibleLibrarian(String command, String identifierArg) {
        super(command, identifierArg, new String[]{".obj"},
			new String[]{".map", ".pdb", ".lnk", ".dll", ".tlb", ".rc", ".h"},
                ".lib", false, null);
    }

    protected void addImpliedArgs(CCTask task, boolean debug, LinkType linkType, Vector<String> args) {
        args.addElement("/nologo");
    }
    
    protected String getCommandFileSwitch(String cmdFile) {
        return "@" + cmdFile;
    }
    public File[] getLibraryPath() {
        return new File[0];
    }
    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        return new String[0];
    }
    public int getMaximumCommandLength() {
// FREEHEP stay on the safe side
        return 32000; // 32767;
    }
    public String[] getOutputFileSwitch(String outFile) {
        StringBuffer buf = new StringBuffer("/OUT:");
        if (outFile.indexOf(' ') >= 0) {
            buf.append('"');
            buf.append(outFile);
            buf.append('"');
        } else {
            buf.append(outFile);
        }
        return new String[]{buf.toString()};
    }
    public boolean isCaseSensitive() {
        return false;
    }
}
