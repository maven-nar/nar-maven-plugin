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
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
/**
 * Adapter for the Microsoft (r) Incremental Linker
 * 
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public final class MsvcLinker extends MsvcCompatibleLinker {
    private static final MsvcLinker dllLinker = new MsvcLinker(".dll");
    private static final MsvcLinker instance = new MsvcLinker(".exe");
    public static MsvcLinker getInstance() {
        return instance;
    }
    private MsvcLinker(String outputSuffix) {
        super("link", "/DLL", outputSuffix);
    }
    public Linker getLinker(LinkType type) {
        if (type.isSharedLibrary()) {
            return dllLinker;
        }
        if (type.isStaticLibrary()) {
            return MsvcLibrarian.getInstance();
        }
        return instance;
    }
}
