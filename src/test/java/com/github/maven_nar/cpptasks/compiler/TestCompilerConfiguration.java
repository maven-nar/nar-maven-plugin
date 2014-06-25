package com.github.maven_nar.cpptasks.compiler;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;

import junit.framework.TestCase;
/**
 */
public abstract class TestCompilerConfiguration extends TestCase {
    public TestCompilerConfiguration(String name) {
        super(name);
    }
    protected abstract CompilerConfiguration create();
    public String getObjectFileExtension() {
        return ".o";
    }
    public void testBid() {
        CompilerConfiguration compiler = create();
        int bid = compiler.bid("c:/foo\\bar\\hello.c");
        assertEquals(100, bid);
        bid = compiler.bid("c:/foo\\bar/hello.c");
        assertEquals(100, bid);
        bid = compiler.bid("c:/foo\\bar\\hello.h");
        assertEquals(1, bid);
        bid = compiler.bid("c:/foo\\bar/hello.h");
        assertEquals(1, bid);
        bid = compiler.bid("c:/foo\\bar/hello.pas");
        assertEquals(0, bid);
        bid = compiler.bid("c:/foo\\bar/hello.java");
        assertEquals(0, bid);
    }
    public void testGetOutputFileName1() {
        CompilerConfiguration compiler = create();
        String input = "c:/foo\\bar\\hello.c";
        //
        //  may cause IllegalStateException since
        //     setPlatformInfo has not been called
        try {
            String[] output = compiler.getOutputFileNames(input, null);
        } catch (java.lang.IllegalStateException ex) {
        }
    }
    public void testGetOutputFileName2() {
        CompilerConfiguration compiler = create();
        String[] output = compiler.getOutputFileNames("c:/foo\\bar\\hello.c", null);
        assertEquals("hello" + getObjectFileExtension(), output[0]);
        output = compiler.getOutputFileNames("c:/foo\\bar/hello.c", null);
        assertEquals("hello" + getObjectFileExtension(), output[0]);
        output = compiler.getOutputFileNames("hello.c", null);
        assertEquals("hello" + getObjectFileExtension(), output[0]);
        output = compiler.getOutputFileNames("c:/foo\\bar\\hello.h", null);
        assertEquals(0, output.length);
        output = compiler.getOutputFileNames("c:/foo\\bar/hello.h", null);
        assertEquals(0, output.length);
    }
}
