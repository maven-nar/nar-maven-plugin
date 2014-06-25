/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks;
import java.io.File;
import junit.framework.TestCase;
import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CompilerParam;
import com.github.maven_nar.cpptasks.DependencyInfo;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.TargetInfo;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.ProgressMonitor;


/**
 * A description of a file built or to be built
 */
public class TestTargetInfo extends TestCase {
    private class DummyConfiguration implements CompilerConfiguration {
        public int bid(String filename) {
            return 1;
        }
        public void close() {
        }
        public void compile(CCTask task, File workingDir, String[] source,
                boolean relentless, ProgressMonitor monitor)
                throws BuildException {
            throw new BuildException("Not implemented");
        }
        public CompilerConfiguration[] createPrecompileConfigurations(
                File file, String[] exceptFiles) {
            return null;
        }
        public String getIdentifier() {
            return "dummy";
        }
        public String[] getIncludeDirectories() {
            return new String[0];
        }
        public String getIncludePathIdentifier() {
            return "dummyIncludePath";
        }
        public String[] getOutputFileNames(String inputFile, VersionInfo versionInfo) {
            return new String[0];
        }
        public CompilerParam getParam(String name) {
            return null;
        }
        public ProcessorParam[] getParams() {
            return new ProcessorParam[0];
        }
        public boolean getRebuild() {
            return false;
        }
        public boolean isPrecompileGeneration() {
            return true;
        }
        public DependencyInfo parseIncludes(CCTask task, File baseDir, File file) {
            return null;
        }
    }
    public TestTargetInfo(String name) {
        super(name);
    }
    public void testConstructorNullConfig() {
        try {
            new TargetInfo(null, new File[]{new File("")}, null, new File(""),
                    false);
            fail("Didn't throw exception");
        } catch (NullPointerException ex) {
        }
    }
    public void testConstructorNullOutput() {
        CompilerConfiguration config = new DummyConfiguration();
        try {
            new TargetInfo(config, new File[]{new File("")}, null, null, false);
            fail("Didn't throw exception");
        } catch (NullPointerException ex) {
        }
    }
    public void testConstructorNullSource() {
        CompilerConfiguration config = new DummyConfiguration();
        try {
            new TargetInfo(config, null, null, new File(""), false);
            fail("Didn't throw exception");
        } catch (NullPointerException ex) {
        }
    }
    public void testGetRebuild() {
        CompilerConfiguration config = new DummyConfiguration();
        TargetInfo targetInfo = new TargetInfo(config, new File[]{new File(
                "FoO.BaR")}, null, new File("foo.o"), false);
        assertEquals(false, targetInfo.getRebuild());
        targetInfo = new TargetInfo(config, new File[]{new File("FoO.BaR")},
                null, new File("foo.o"), true);
        assertEquals(true, targetInfo.getRebuild());
    }
    public void testGetSource() {
        CompilerConfiguration config = new DummyConfiguration();
        TargetInfo targetInfo = new TargetInfo(config, new File[]{new File(
                "FoO.BaR")}, null, new File("foo.o"), false);
        String source = targetInfo.getSources()[0].getName();
        assertEquals(source, "FoO.BaR");
    }
    public void testHasSameSource() {
        CompilerConfiguration config = new DummyConfiguration();
        TargetInfo targetInfo = new TargetInfo(config, new File[]{new File(
                "foo.bar")}, null, new File("foo.o"), false);
        boolean hasSame = targetInfo.getSources()[0]
                .equals(new File("foo.bar"));
        assertTrue(hasSame);
        hasSame = targetInfo.getSources()[0].equals(new File("boo.far"));
        assertEquals(hasSame, false);
    }
    public void testMustRebuild() {
        CompilerConfiguration config = new DummyConfiguration();
        TargetInfo targetInfo = new TargetInfo(config, new File[]{new File(
                "FoO.BaR")}, null, new File("foo.o"), false);
        assertEquals(false, targetInfo.getRebuild());
        targetInfo.mustRebuild();
        assertEquals(true, targetInfo.getRebuild());
    }
}
