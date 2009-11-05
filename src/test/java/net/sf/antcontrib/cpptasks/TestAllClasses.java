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
package net.sf.antcontrib.cpptasks;
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
        addTestSuite(net.sf.antcontrib.cpptasks.TestCUtil.class);
        addTestSuite(net.sf.antcontrib.cpptasks.borland.TestBorlandCCompiler.class);
        addTestSuite(net.sf.antcontrib.cpptasks.compiler.TestAbstractCompiler.class);
        addTestSuite(net.sf.antcontrib.cpptasks.compiler.TestAbstractLinker.class);
        addTestSuite(net.sf.antcontrib.cpptasks.compiler.TestAbstractProcessor.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestCCTask.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestCompilerEnum.class);
        addTestSuite(net.sf.antcontrib.cpptasks.compiler.TestCommandLineCompilerConfiguration.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestDependencyTable.class);
        addTestSuite(net.sf.antcontrib.cpptasks.types.TestDefineArgument.class);
        addTestSuite(net.sf.antcontrib.cpptasks.devstudio.TestDevStudio2005CCompiler.class);
        addTestSuite(net.sf.antcontrib.cpptasks.devstudio.TestDevStudioCCompiler.class);
        addTestSuite(net.sf.antcontrib.cpptasks.devstudio.TestDevStudioLinker.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestLinkerDef.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestTargetInfo.class);
        addTestSuite(net.sf.antcontrib.cpptasks.types.TestLibrarySet.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestCompilerDef.class);
        addTestSuite(net.sf.antcontrib.cpptasks.parser.TestCParser.class);
        addTestSuite(net.sf.antcontrib.cpptasks.gcc.TestGccCCompiler.class);
        addTestSuite(net.sf.antcontrib.cpptasks.gcc.TestAbstractLdLinker.class);
        addTestSuite(net.sf.antcontrib.cpptasks.gcc.TestAbstractArLibrarian.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestTargetHistoryTable.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestOutputTypeEnum.class);
        addTestSuite(net.sf.antcontrib.cpptasks.compiler.TestLinkType.class);
        addTestSuite(net.sf.antcontrib.cpptasks.TestLinkerEnum.class);
        addTestSuite(net.sf.antcontrib.cpptasks.gcc.TestAbstractLdLinker.class);
        addTestSuite(net.sf.antcontrib.cpptasks.gcc.TestAbstractArLibrarian.class);
        addTestSuite(net.sf.antcontrib.cpptasks.gcc.TestGccLinker.class);
        addTestSuite(net.sf.antcontrib.cpptasks.gcc.TestGccLinker.class);
        addTestSuite(net.sf.antcontrib.cpptasks.sun.TestForteCCCompiler.class);
        addTestSuite(net.sf.antcontrib.cpptasks.hp.TestaCCCompiler.class);
        addTestSuite(net.sf.antcontrib.cpptasks.ibm.TestVisualAgeCCompiler.class);
    }
}
