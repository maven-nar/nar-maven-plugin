package com.github.maven_nar.cpptasks.compiler;
import java.io.File;

import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompiler;
import com.github.maven_nar.cpptasks.compiler.CommandLineCompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.gcc.GccCCompiler;

/**
 */
public class TestCommandLineCompilerConfiguration
        extends
            TestCompilerConfiguration {
    private final CommandLineCompiler compiler;
    private final String compilerId;
    public TestCommandLineCompilerConfiguration(String name) {
        super(name);
        compiler = (GccCCompiler) GccCCompiler.getInstance();
        compilerId = compiler.getIdentifier();
    }
    protected CompilerConfiguration create() {
        return new CommandLineCompilerConfiguration(compiler, "dummy",
                new File[0], new File[0], new File[0], "",
                new String[]{"/Id:/gcc"}, new ProcessorParam[0], false,
                new String[0]);
    }
    public void testConstructorNullCompiler() {
        try {
            new CommandLineCompilerConfiguration(null, "dummy", new File[0],
                    new File[0], new File[0], "", new String[0],
                    new ProcessorParam[0], false, new String[0]);
            fail("Should throw exception for null compiler");
        } catch (NullPointerException ex) {
        }
    }
    public void testGetIdentifier() {
        CompilerConfiguration config = create();
        String id = config.getIdentifier();
        assertEquals("dummy", id);
    }
    public void testToString() {
        CompilerConfiguration config = create();
        String toString = config.toString();
        assertEquals("dummy", toString);
    }
}
