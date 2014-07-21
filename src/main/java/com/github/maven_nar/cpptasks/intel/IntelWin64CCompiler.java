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
package com.github.maven_nar.cpptasks.intel;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.msvc.MsvcCompatibleCCompiler;
/**
 * Adapter for the Intel C++ compiler for Itanium(TM) Applications
 * 
 * @author Curt Arnold
 */
public final class IntelWin64CCompiler extends MsvcCompatibleCCompiler {
    private static final IntelWin64CCompiler instance = new IntelWin64CCompiler(
            false, null);
    public static IntelWin64CCompiler getInstance() {
        return instance;
    }
    private IntelWin64CCompiler(boolean newEnvironment, Environment env) {
        super("ecl", "-help", newEnvironment, env);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new IntelWin64CCompiler(newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        //
        //   currently the Intel Win32 and Win64 linkers
        //      are command line equivalent
        return IntelWin32Linker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return 32767;
    }
}
