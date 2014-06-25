package com.github.maven_nar.cpptasks.borland;
import com.github.maven_nar.cpptasks.borland.BorlandCCompiler;
import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.compiler.TestAbstractCompiler;

/**
 * Borland C++ Compiler adapter tests
 * 
 * Override create to test concrete compiler implementions
 */
public class TestBorlandCCompiler extends TestAbstractCompiler {
    public TestBorlandCCompiler(String name) {
        super(name);
    }
    protected AbstractProcessor create() {
        return BorlandCCompiler.getInstance();
    }
    protected String getObjectExtension() {
        return ".obj";
    }
    public void testGetIdentfier() {
    }
}
