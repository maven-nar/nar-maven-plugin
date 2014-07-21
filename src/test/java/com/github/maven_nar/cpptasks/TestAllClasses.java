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
import junit.framework.TestSuite;
/**
 * Test for abstract compiler class
 * 
 * Override create to test concrete compiler implementions
 */
public class TestAllClasses extends TestSuite {
    public static TestSuite suite() {
        return new TestAllClasses("TestAllClasses");
    }
    public TestAllClasses(String name) {
        super(name);
        addTestSuite(com.github.maven_nar.cpptasks.TestCUtil.class);
        addTestSuite(com.github.maven_nar.cpptasks.borland.TestBorlandCCompiler.class);
        addTestSuite(com.github.maven_nar.cpptasks.compiler.TestAbstractCompiler.class);
        addTestSuite(com.github.maven_nar.cpptasks.compiler.TestAbstractLinker.class);
        addTestSuite(com.github.maven_nar.cpptasks.compiler.TestAbstractProcessor.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestCCTask.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestCompilerEnum.class);
        addTestSuite(com.github.maven_nar.cpptasks.compiler.TestCommandLineCompilerConfiguration.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestDependencyTable.class);
        addTestSuite(com.github.maven_nar.cpptasks.types.TestDefineArgument.class);
        addTestSuite(com.github.maven_nar.cpptasks.msvc.TestMsvc2005CCompiler.class);
        addTestSuite(com.github.maven_nar.cpptasks.msvc.TestMsvcCCompiler.class);
        addTestSuite(com.github.maven_nar.cpptasks.msvc.TestMsvcLinker.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestLinkerDef.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestTargetInfo.class);
        addTestSuite(com.github.maven_nar.cpptasks.types.TestLibrarySet.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestCompilerDef.class);
        addTestSuite(com.github.maven_nar.cpptasks.parser.TestCParser.class);
        addTestSuite(com.github.maven_nar.cpptasks.gcc.TestGccCCompiler.class);
        addTestSuite(com.github.maven_nar.cpptasks.gcc.TestAbstractLdLinker.class);
        addTestSuite(com.github.maven_nar.cpptasks.gcc.TestAbstractArLibrarian.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestTargetHistoryTable.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestOutputTypeEnum.class);
        addTestSuite(com.github.maven_nar.cpptasks.compiler.TestLinkType.class);
        addTestSuite(com.github.maven_nar.cpptasks.TestLinkerEnum.class);
        addTestSuite(com.github.maven_nar.cpptasks.gcc.TestAbstractLdLinker.class);
        addTestSuite(com.github.maven_nar.cpptasks.gcc.TestAbstractArLibrarian.class);
        addTestSuite(com.github.maven_nar.cpptasks.gcc.TestGccLinker.class);
        addTestSuite(com.github.maven_nar.cpptasks.gcc.TestGccLinker.class);
        addTestSuite(com.github.maven_nar.cpptasks.sun.TestForteCCCompiler.class);
        addTestSuite(com.github.maven_nar.cpptasks.hp.TestaCCCompiler.class);
        addTestSuite(com.github.maven_nar.cpptasks.ibm.TestVisualAgeCCompiler.class);
    }
}
