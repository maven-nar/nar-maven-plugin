package com.github.maven_nar.cpptasks.gcc;
import java.io.File;

import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.gcc.GccCCompiler;
import com.github.maven_nar.cpptasks.gcc.GccCompatibleCCompiler;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.FortranParser;
import com.github.maven_nar.cpptasks.parser.Parser;

/**
 * Test gcc compiler adapter
 *  
 */
public class TestGccCCompiler extends TestGccCompatibleCCompiler {
    public TestGccCCompiler(String name) {
        super(name);
    }
    protected GccCompatibleCCompiler create() {
        return GccCCompiler.getInstance();
    }
    public void testBidObjectiveAssembly() {
        GccCCompiler compiler = GccCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.s"));
    }
    public void testBidObjectiveC() {
        GccCCompiler compiler = GccCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.m"));
    }
    public void testBidObjectiveCpp() {
        GccCCompiler compiler = GccCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.mm"));
    }
    public void testBidPreprocessedCpp() {
        GccCCompiler compiler = GccCCompiler.getInstance();
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, compiler
                .bid("foo.ii"));
    }
    public void testCreateCParser1() {
        Parser parser = GccCCompiler.getInstance().createParser(
                new File("foo.c"));
        assertTrue(parser instanceof CParser);
    }
    public void testCreateCParser2() {
        Parser parser = GccCCompiler.getInstance().createParser(
                new File("foo."));
        assertTrue(parser instanceof CParser);
    }
    public void testCreateCParser3() {
        Parser parser = GccCCompiler.getInstance()
                .createParser(new File("foo"));
        assertTrue(parser instanceof CParser);
    }
    public void testCreateFortranParser1() {
        Parser parser = GccCCompiler.getInstance().createParser(
                new File("foo.f"));
        assertTrue(parser instanceof FortranParser);
    }
    public void testCreateFortranParser2() {
        Parser parser = GccCCompiler.getInstance().createParser(
                new File("foo.FoR"));
        assertTrue(parser instanceof FortranParser);
    }
    public void testCreateFortranParser3() {
        Parser parser = GccCCompiler.getInstance().createParser(
                new File("foo.f90"));
        assertTrue(parser instanceof FortranParser);
    }
    
}
