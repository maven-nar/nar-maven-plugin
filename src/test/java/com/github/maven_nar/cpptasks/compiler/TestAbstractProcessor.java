package com.github.maven_nar.cpptasks.compiler;
import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.ProcessorDef;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;

import junit.framework.TestCase;
/**
 * Test for abstract compiler class
 * 
 * Override create to test concrete compiler implementions
 */
public class TestAbstractProcessor extends TestCase {
    private class DummyAbstractProcessor extends AbstractProcessor {
        public DummyAbstractProcessor() {
            super(new String[]{".cpp", ".c"},
                    new String[]{".hpp", ".h", ".inl"});
        }
        public ProcessorConfiguration createConfiguration(CCTask task,
                LinkType linkType, ProcessorDef[] defaultProvider,
                ProcessorDef specificProvider,
				com.github.maven_nar.cpptasks.TargetDef targetPlatform,
				VersionInfo versionInfo) {
            return null;
        }
        public String getIdentifier() {
            return "dummy";
        }
        public Linker getLinker(LinkType type) {
            return null;
        }
        public String[] getOutputFileNames(String sourceFile, VersionInfo versionInfo) {
            return new String[0];
        }
        public String[][] getRuntimeLibraries(boolean debug,
                boolean multithreaded, boolean staticLink) {
            return new String[2][0];
        }
    }
    public TestAbstractProcessor(String name) {
        super(name);
    }
    protected AbstractProcessor create() {
        return new DummyAbstractProcessor();
    }
    public void failingtestBid() {
        AbstractProcessor compiler = create();
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
    public void testGetIdentfier() {
        AbstractProcessor compiler = create();
        String id = compiler.getIdentifier();
        assertEquals("dummy", id);
    }
}
