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
package com.github.maven_nar.cpptasks.compiler;
import java.io.File;

import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.gcc.GccCCompiler;

/**
 */
public class TestCommandLineCompilerConfiguration
        extends
            TestCompilerConfiguration {
    private final CommandLineCompiler compiler;
    private final String compilerId;
    public TestCommandLineCompilerConfiguration(String name) {
        super(name);
        compiler = (GccCCompiler) GccCCompiler.getInstance();
        compilerId = compiler.getIdentifier();
    }
    protected CompilerConfiguration create() {
        return new CommandLineCompilerConfiguration(compiler, "dummy",
                new File[0], new File[0], new File[0], "",
                new String[]{"/Id:/gcc"}, new ProcessorParam[0], false,
                new String[0]);
    }
    public void testConstructorNullCompiler() {
        try {
            new CommandLineCompilerConfiguration(null, "dummy", new File[0],
                    new File[0], new File[0], "", new String[0],
                    new ProcessorParam[0], false, new String[0]);
            fail("Should throw exception for null compiler");
        } catch (NullPointerException ex) {
        }
    }
    public void testGetIdentifier() {
        CompilerConfiguration config = create();
        String id = config.getIdentifier();
        assertEquals("dummy", id);
    }
    public void testToString() {
        CompilerConfiguration config = create();
        String toString = config.toString();
        assertEquals("dummy", toString);
    }
}
