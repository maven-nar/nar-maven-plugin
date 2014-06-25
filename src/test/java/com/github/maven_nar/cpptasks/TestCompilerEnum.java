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
package com.github.maven_nar.cpptasks;
import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CompilerEnum;
/**
 * Tests for CompilerEnum.
 */
public class TestCompilerEnum extends TestCase {
    /**
     * Create instance of TestCompilerEnum.
     * @param name test name.
     */
    public TestCompilerEnum(final String name) {
        super(name);
    }
    /**
     * Test that "gcc" is recognized as a compiler enum.
     */
    public void testCompilerEnum1() {
        CompilerEnum compilerEnum = new CompilerEnum();
        compilerEnum.setValue("gcc");
        assertTrue(compilerEnum.getIndex() >= 0);
    }
    /**
     * Test that "bogus" is not recognized as a compiler enum.
     */
    public void testCompilerEnum2() {
        CompilerEnum compilerEnum = new CompilerEnum();
        try {
            compilerEnum.setValue("bogus");
            fail();
        } catch (BuildException ex) {
        }
    }
}
