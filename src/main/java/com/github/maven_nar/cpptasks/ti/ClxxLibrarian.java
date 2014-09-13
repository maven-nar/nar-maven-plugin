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
    protected void addBase(long base, Vector<String> args) {
        // TODO Auto-generated method stub
    }
    protected void addFixed(Boolean fixed, Vector<String> args) {
        // TODO Auto-generated method stub
    }
    protected void addImpliedArgs(boolean debug, LinkType linkType, Vector<String> args) {
        // TODO Auto-generated method stub
    }
    protected void addIncremental(boolean incremental, Vector<String> args) {
        // TODO Auto-generated method stub
    }
    protected void addMap(boolean map, Vector<String> args) {
        // TODO Auto-generated method stub
    }
    protected void addStack(int stack, Vector<String> args) {
        // TODO Auto-generated method stub
    }
    protected void addEntry(String entry, Vector<String> args) {
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
