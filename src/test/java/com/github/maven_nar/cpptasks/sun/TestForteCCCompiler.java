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
package com.github.maven_nar.cpptasks.sun;
import java.util.Vector;

import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.sun.ForteCCCompiler;

import junit.framework.TestCase;

/**
 * Test Sun Forte compiler adapter
 *  
 */
// TODO Since ForteCCCompiler extends GccCompatibleCCompiler, this test
// should probably extend TestGccCompatibleCCompiler.
public class TestForteCCCompiler extends TestCase {
    public TestForteCCCompiler(String name) {
        super(name);
    }
    public void testBidC() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.c"));
    }
    public void testBidCpp() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.C"));
    }
    public void testBidCpp2() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cc"));
    }
    public void testBidCpp3() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cxx"));
    }
    public void testBidCpp4() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cpp"));
    }
    public void testBidCpp5() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.c++"));
    }
    public void testBidPreprocessed() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.i"));
    }
    public void testBidAssembly() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.s"));
    }
    /**
     * Tests command line switches for warning = 0
     */
    public void testWarningLevel0() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 0);
        assertEquals(1, args.size());
        assertEquals("-w", args.elementAt(0));
    }
    /**
     * Tests command line switches for warning = 1
     */
    public void testWarningLevel1() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 1);
        assertEquals(0, args.size());
    }
    /**
     * Tests command line switches for warning = 2
     */
    public void testWarningLevel2() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 2);
        assertEquals(0, args.size());
    }
    /**
     * Tests command line switches for warning = 3
     */
    public void testWarningLevel3() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 3);
        assertEquals(1, args.size());
        assertEquals("+w", args.elementAt(0));
    }
    /**
     * Tests command line switches for warning = 4
     */
    public void testWarningLevel4() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 4);
        assertEquals(1, args.size());
        assertEquals("+w2", args.elementAt(0));
    }
    /**
     * Tests command line switches for warning = 5
     */
    public void testWarningLevel5() {
        ForteCCCompiler compiler = ForteCCCompiler.getInstance();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 5);
        assertEquals(2, args.size());
        assertEquals("+w2", args.elementAt(0));
        assertEquals("-xwe", args.elementAt(1));
    }
}
