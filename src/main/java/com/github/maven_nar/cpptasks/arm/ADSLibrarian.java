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
package com.github.maven_nar.cpptasks.arm;

import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

import java.io.File;

/**
 * Adapter for ARM Librarian
 *
 * @author Curt Arnold
 */
public class ADSLibrarian extends CommandLineLinker {

    private static final ADSLibrarian instance = new ADSLibrarian();

    public static ADSLibrarian getInstance() {
      return instance;
    }

    private ADSLibrarian()
    {
        super("armar",null,
          new String[] { ".o" }, new String[0], ".lib", false, null);
    }

    protected String getCommandFileSwitch(String commandFile) {
        // TODO Auto-generated method stub
        return null;
    }

    public File[] getLibraryPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
        return new String[0];
    }

    public Linker getLinker(LinkType linkType) {
        // TODO Auto-generated method stub
        return null;
    }

    protected int getMaximumCommandLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    protected String[] getOutputFileSwitch(String outputFile) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCaseSensitive() {
        // TODO Auto-generated method stub
        return false;
    }

}
