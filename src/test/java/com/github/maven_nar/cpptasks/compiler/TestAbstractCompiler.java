package com.github.maven_nar.cpptasks.compiler;
import java.io.File;


import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.AbstractCompiler;
import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.parser.CParser;
import com.github.maven_nar.cpptasks.parser.Parser;
/**
 * Test for abstract compiler class
 * 
 * Override create to test concrete compiler implementions
 */
public class TestAbstractCompiler extends TestAbstractProcessor {
    private class DummyAbstractCompiler extends AbstractCompiler {
        public DummyAbstractCompiler() {
            super(new String[]{".cpp", ".c"},
                    new String[]{".hpp", ".h", ".inl"}, ".o");
        }
        public void compile(CCTask task, File[] srcfile, File[] outputfile,
                CompilerConfiguration config) throws BuildException {
            throw new BuildException("Not implemented");
        }
        public CompilerConfiguration createConfiguration(CCTask task,
                LinkType linkType, ProcessorDef[] def1, CompilerDef def2,
				com.github.maven_nar.cpptasks.TargetDef targetPlatform,
				VersionInfo versionInfo) {
            return null;
        }
        public Parser createParser(File file) {
            return new CParser();
        }
        public String getIdentifier() {
            return "dummy";
        }
        public Linker getLinker(LinkType type) {
            return null;
        }
    }
    public TestAbstractCompiler(String name) {
        super(name);
    }
    protected AbstractProcessor create() {
        return new DummyAbstractCompiler();
    }
    protected String getObjectExtension() {
        return ".o";
    }
    public void testCanParseTlb() {
        AbstractCompiler compiler = (AbstractCompiler) create();
        assertEquals(false, compiler.canParse(new File("sample.tlb")));
    }
    public void failingtestGetOutputFileName1() {
        AbstractProcessor compiler = create();
        String[] output = compiler.getOutputFileNames("c:/foo\\bar\\hello.c", null);
        assertEquals("hello" + getObjectExtension(), output[0]);
        output = compiler.getOutputFileNames("c:/foo\\bar/hello.c", null);
        assertEquals("hello" + getObjectExtension(), output[0]);
        output = compiler.getOutputFileNames("hello.c", null);
        assertEquals("hello" + getObjectExtension(), output[0]);
        output = compiler.getOutputFileNames("c:/foo\\bar\\hello.h", null);
        assertEquals(0, output.length);
        output = compiler.getOutputFileNames("c:/foo\\bar/hello.h", null);
        assertEquals(0, output.length);
    }
}
