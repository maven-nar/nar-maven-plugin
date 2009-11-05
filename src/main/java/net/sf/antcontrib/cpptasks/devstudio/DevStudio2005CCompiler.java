/*
 * 
 * Copyright 2002-2007 The Ant-Contrib project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.antcontrib.cpptasks.devstudio;
import java.util.Vector;

import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.compiler.Processor;

import org.apache.tools.ant.types.Environment;

/**
 * Adapter for the Microsoft(r) C/C++ 8 Optimizing Compiler
 * 
 * @author David Haney
 */
public final class DevStudio2005CCompiler extends DevStudioCompatibleCCompiler {
    private static final DevStudio2005CCompiler instance = new DevStudio2005CCompiler(
            "cl", false, null);
    public static DevStudio2005CCompiler getInstance() {
        return instance;
    }
    private DevStudio2005CCompiler(String command, boolean newEnvironment,
            Environment env) {
        super(command, "/bogus", newEnvironment, env);
    }
    /**
     * Override the default debug flags to use VC 8 compatible versions.
     */
    protected void addDebugSwitch(Vector args) {
        args.addElement("/Zi");
        args.addElement("/Od");
        args.addElement("/RTC1");
        args.addElement("/D_DEBUG");
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new DevStudio2005CCompiler(getCommand(), newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        return DevStudioLinker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return 32767;
    }
}
