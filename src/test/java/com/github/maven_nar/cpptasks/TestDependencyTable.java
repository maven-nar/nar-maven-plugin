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
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.github.maven_nar.cpptasks.DependencyTable;
/**
 * DependencyTable tests
 * 
 * @author curta
 */
public class TestDependencyTable extends TestXMLConsumer {
    /**
     * Constructor
     * 
     * @param testName
     *            test name
     */
    public TestDependencyTable(String testName) {
        super(testName);
    }
    /**
     * Loads a dependency file from OpenSHORE (http://www.openshore.org)
     * 
     * @throws IOException
     */
    public void testLoadOpenshore() throws IOException,
            ParserConfigurationException, SAXException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        try {
            copyResourceToTmpDir("openshore/dependencies.xml",
                    "dependencies.xml");
            DependencyTable dependencies = new DependencyTable(new File(tmpDir));
            dependencies.load();
        } finally {
            deleteTmpFile("dependencies.xml");
        }
    }
    /**
     * Loads a dependency file from Xerces-C (http://xml.apache.org)
     * 
     * @throws IOException
     */
    public void testLoadXerces() throws IOException,
            ParserConfigurationException, SAXException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        try {
            copyResourceToTmpDir("xerces-c/dependencies.xml",
                    "dependencies.xml");
            DependencyTable dependencies = new DependencyTable(new File(tmpDir));
            dependencies.load();
        } finally {
            deleteTmpFile("dependencies.xml");
        }
    }
}
