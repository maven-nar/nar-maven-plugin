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
import java.io.File;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.LinkerDef;
import net.sf.antcontrib.cpptasks.ProcessorDef;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;
import net.sf.antcontrib.cpptasks.TargetDef;
import net.sf.antcontrib.cpptasks.VersionInfo;

/**
 * Test for abstract compiler class
 * 
 * Override create to test concrete compiler implementions
 */
public class TestAbstractLinker extends TestAbstractProcessor {
    private class DummyAbstractLinker extends AbstractLinker {
        public DummyAbstractLinker() {
            super(new String[]{".obj", ".lib"}, new String[]{".map", ".exp"});
        }
        public LinkerConfiguration createConfiguration(final CCTask task,
                final LinkType linkType, 
				final ProcessorDef[] def1, 
				final LinkerDef def2,
				final TargetDef targetPlatform,
				final VersionInfo versionInfo) {
            return null;
        }
        public String getIdentifier() {
            return "dummy";
        }
        public File[] getLibraryPath() {
            return new File[0];
        }
        public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libType) {
            return libnames;
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
        public boolean isCaseSensitive() {
            return true;
        }
    }
    public TestAbstractLinker(String name) {
        super(name);
    }
    protected AbstractProcessor create() {
        return new DummyAbstractLinker();
    }
    public void testBid() {
        AbstractProcessor compiler = create();
        int bid = compiler.bid("c:/foo\\bar\\hello.obj");
        assertEquals(100, bid);
        bid = compiler.bid("c:/foo\\bar/hello.lib");
        assertEquals(100, bid);
        bid = compiler.bid("c:/foo\\bar\\hello.map");
        assertEquals(0, bid);
        bid = compiler.bid("c:/foo\\bar/hello.map");
        assertEquals(0, bid);
        bid = compiler.bid("c:/foo\\bar/hello.c");
        assertEquals(1, bid);
        bid = compiler.bid("c:/foo\\bar/hello.cpp");
        assertEquals(1, bid);
    }
}
