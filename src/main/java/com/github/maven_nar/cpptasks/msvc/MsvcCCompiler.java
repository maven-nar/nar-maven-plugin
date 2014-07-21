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

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
/**
 * Adapter for the Microsoft(r) C/C++ Optimizing Compiler
 * 
 * @author Adam Murdoch
 */
public final class MsvcCCompiler extends MsvcCompatibleCCompiler {
    private static final MsvcCCompiler instance = new MsvcCCompiler(
            "cl", false, null);
    public static MsvcCCompiler getInstance() {
        return instance;
    }
    private MsvcCCompiler(String command, boolean newEnvironment,
            Environment env) {
        super(command, "/bogus", newEnvironment, env);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new MsvcCCompiler(getCommand(), newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        return MsvcLinker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
// FREEHEP stay on safe side
        return 32000; // 32767;
    }
}
