package com.github.maven_nar.cpptasks.ibm;
import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.ibm.VisualAgeCCompiler;

import junit.framework.TestCase;
/**
 * Test IBM Visual Age compiler adapter
 *  
 */
// TODO Since VisualAgeCCompiler extends GccCompatibleCCompiler, this test
// should probably extend TestGccCompatibleCCompiler.
public class TestVisualAgeCCompiler extends TestCase {
    public TestVisualAgeCCompiler(String name) {
        super(name);
    }
    public void testBidC() {
        VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.c"));
    }
    public void testBidCpp() {
        VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.C"));
    }
    public void testBidCpp2() {
        VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cc"));
    }
    public void testBidCpp3() {
        VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cxx"));
    }
    public void testBidCpp4() {
        VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.cpp"));
    }
    public void testBidPreprocessed() {
        VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.i"));
    }
    public void testBidAssembly() {
        VisualAgeCCompiler compiler = VisualAgeCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.s"));
    }
}
