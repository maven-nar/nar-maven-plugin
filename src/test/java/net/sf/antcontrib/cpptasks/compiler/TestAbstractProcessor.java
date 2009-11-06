/*
 * 
 * Copyright 2002-2004 The Ant-Contrib project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.antcontrib.cpptasks.compiler;
import junit.framework.TestCase;
import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.ProcessorDef;
import net.sf.antcontrib.cpptasks.VersionInfo;
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
				net.sf.antcontrib.cpptasks.TargetDef targetPlatform,
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
    public void testBid() {
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
