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
// BEGINFREEHEP
package com.github.maven_nar.cpptasks.intel;

import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.gcc.GccCompatibleCCompiler;

public final class IntelLinuxFortranCompiler extends GccCompatibleCCompiler {
    private static final IntelLinuxFortranCompiler instance = new IntelLinuxFortranCompiler(
            false, new IntelLinuxFortranCompiler(true, null, false, null), false,
            null);
    public static IntelLinuxFortranCompiler getInstance() {
        return instance;
    }
    private IntelLinuxFortranCompiler(boolean isLibtool,
            IntelLinuxFortranCompiler libtoolCompiler, boolean newEnvironment,
            Environment env) {
        super("ifort", "-V", isLibtool, libtoolCompiler, newEnvironment, env);
    }
    public Processor changeEnvironment(boolean newEnvironment, Environment env) {
        if (newEnvironment || env != null) {
            return new IntelLinuxFortranCompiler(getLibtool(),
                    (IntelLinuxFortranCompiler) getLibtoolCompiler(),
                    newEnvironment, env);
        }
        return this;
    }
    public Linker getLinker(LinkType type) {
        return IntelLinux32Linker.getInstance().getLinker(type);
    }
    public int getMaximumCommandLength() {
        return Integer.MAX_VALUE;
    }
}
// ENDFREEHEP