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
import java.io.FileOutputStream;
import java.io.IOException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.ProcessorParam;
import com.github.maven_nar.cpptasks.TargetHistoryTable;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;

/**
 * Tests for TargetHistoryTable
 * 
 * @author CurtA
 */
public class TestTargetHistoryTable extends TestXMLConsumer {
    public static class MockProcessorConfiguration
            implements
                ProcessorConfiguration {
        public MockProcessorConfiguration() {
        }
        public int bid(String fileName) {
            return 100;
        }
        public String getIdentifier() {
            return "Mock Configuration";
        }
        public String[] getOutputFileNames(String baseName, VersionInfo versionInfo) {
            return new String[] { baseName };
        }
        public ProcessorParam[] getParams() {
            return new ProcessorParam[0];
        }
        public boolean getRebuild() {
            return false;
        }
    }
    /**
     * Constructor
     * 
     * @param name
     *            test case name
     * @see junit.framework.TestCase#TestCase(String)
     */
    public TestTargetHistoryTable(String name) {
        super(name);
    }
    /**
     * Tests loading a stock history file
     * 
     * @throws IOException
     */
    public void testLoadOpenshore() throws IOException {
        try {
            copyResourceToTmpDir("openshore/history.xml", "history.xml");
            CCTask task = new CCTask();
            String tmpDir = System.getProperty("java.io.tmpdir");
            TargetHistoryTable history = new TargetHistoryTable(task, new File(
                    tmpDir));
        } finally {
            deleteTmpFile("history.xml");
        }
    }
    /**
     * Tests loading a stock history file
     * 
     * @throws IOException
     */
    public void testLoadXerces() throws IOException {
        try {
            copyResourceToTmpDir("xerces-c/history.xml", "history.xml");
            CCTask task = new CCTask();
            String tmpDir = System.getProperty("java.io.tmpdir");
            TargetHistoryTable history = new TargetHistoryTable(task, new File(
                    tmpDir));
        } finally {
            deleteTmpFile("history.xml");
        }
    }
    /**
     * Tests for bug fixed by patch [ 650397 ] Fix: Needless rebuilds on Unix
     * 
     * @throws IOException
     */
    public void testUpdateTimeResolution() throws IOException {
        File compiledFile = null;
        
        try {
            //
            //  delete any history file that might exist
            //   in the test output directory
            String tempDir = System.getProperty("java.io.tmpdir");
            File historyFile = new File(tempDir, "history.xml");
            historyFile.deleteOnExit();
            if (historyFile.exists()) {
                historyFile.delete();
            }
            TargetHistoryTable table = new TargetHistoryTable(null, new File(
                    tempDir));
            //
            //  create a dummy compiled unit
            //
            compiledFile = new File(tempDir, "dummy.o");
            FileOutputStream compiledStream = new FileOutputStream(compiledFile);
            compiledStream.close();
            //
            //   lastModified times can be slightly less than
            //      task start time due to file system resolution.
            //      Mimic this by slightly incrementing the last modification time.
            //      
            long startTime = compiledFile.lastModified() + 1;
            //
            //   update the table
            //
            table.update(new MockProcessorConfiguration(),
                    new String[]{"dummy.o"}, null);
            //
            //   commit. If "compiled" file was judged to be
            //   valid we should have a history file.
            //
            table.commit();
            historyFile = table.getHistoryFile();
            assertTrue("History file was not created", historyFile.exists());
            assertTrue("History file was empty", historyFile.length() > 10);
        } finally {
            if (compiledFile != null && compiledFile.exists()) {
                compiledFile.delete();
            }
            
        }
    }
}
