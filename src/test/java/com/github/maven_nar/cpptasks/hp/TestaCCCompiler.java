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
package com.github.maven_nar.cpptasks.hp;
import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.hp.aCCCompiler;

import junit.framework.TestCase;
/**
 * Test HP aCC compiler adapter
 *  
 */
// TODO Since aCCCompiler extends GccCompatibleCCompiler, this test
// should probably extend TestGccCompatibleCCompiler.
public class TestaCCCompiler extends TestCase {
    public TestaCCCompiler(String name) {
        super(name);
    }
    public void testBidC() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.c"));
    }
    public void testBidCpp() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.C"));
    }
    public void testBidCpp2() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cc"));
    }
    public void testBidCpp3() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.CC"));
    }
    public void testBidCpp4() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cxx"));
    }
    public void testBidCpp5() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.CXX"));
    }
    public void testBidCpp6() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cpp"));
    }
    public void testBidCpp7() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.CPP"));
    }
    public void testBidCpp8() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.c++"));
    }
    public void testBidCpp9() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.C++"));
    }
    public void testBidPreprocessed() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.i"));
    }
    public void testBidAssembly() {
        aCCCompiler compiler = aCCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.s"));
    }
}
