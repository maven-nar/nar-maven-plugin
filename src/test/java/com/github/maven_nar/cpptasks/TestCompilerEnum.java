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
