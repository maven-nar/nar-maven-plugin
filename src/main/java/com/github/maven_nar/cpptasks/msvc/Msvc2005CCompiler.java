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
import java.util.Vector;


import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;

/**
 * Adapter for the Microsoft(r) C/C++ 8 Optimizing Compiler
 * 
 * @author David Haney
 */
public final class Msvc2005CCompiler extends MsvcCompatibleCCompiler {
    private static final Msvc2005CCompiler instance = new Msvc2005CCompiler(
            "cl", false, null);
    public static Msvc2005CCompiler getInstance() {
        return instance;
    }
    private Msvc2005CCompiler(String command, boolean newEnvironment,
            Environment env) {
        super(command, "/bogus", newEnvironment, env);
    }
    /**
     * Override the default debug flags to use VC 8 compatible versions.
     */
    protected void addDebugSwitch(Vector<String> args) {
        args.addElement("/Zi");
        args.addElement("/Od");
        args.addElement("/RTC1");
        args.addElement("/D_DEBUG");
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new Msvc2005CCompiler(getCommand(), newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        return MsvcLinker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return 32767;
    }
}
