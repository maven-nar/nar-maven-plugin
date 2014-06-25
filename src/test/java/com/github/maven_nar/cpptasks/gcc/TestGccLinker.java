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
package com.github.maven_nar.cpptasks.gcc;
import com.github.maven_nar.cpptasks.OutputTypeEnum;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.GccLinker;

import junit.framework.TestCase;
/**
 * @author CurtA
 */
public class TestGccLinker extends TestCase {
    private final String realOSName;
    /**
     * Constructor
     * 
     * @param name test name
     */
    public TestGccLinker(String name) {
        super(name);
        realOSName = System.getProperty("os.name");
    }
    protected void tearDown() throws java.lang.Exception {
        System.setProperty("os.name", realOSName);
    }
    public void testGetLinkerDarwinPlugin() {
        System.setProperty("os.name", "Mac OS X");
        GccLinker linker = GccLinker.getInstance();
        OutputTypeEnum outputType = new OutputTypeEnum();
        outputType.setValue("plugin");
        LinkType linkType = new LinkType();
        linkType.setOutputType(outputType);
        Linker pluginLinker = linker.getLinker(linkType);
        assertEquals("libfoo.bundle", pluginLinker.getOutputFileNames("foo", null)[0]);
    }
    public void testGetLinkerDarwinShared() {
        System.setProperty("os.name", "Mac OS X");
        GccLinker linker = GccLinker.getInstance();
        OutputTypeEnum outputType = new OutputTypeEnum();
        outputType.setValue("shared");
        LinkType linkType = new LinkType();
        linkType.setOutputType(outputType);
        Linker sharedLinker = linker.getLinker(linkType);
        assertEquals("libfoo.dylib", sharedLinker.getOutputFileNames("foo", null)[0]);
    }
    public void testGetLinkerNonDarwinPlugin() {
        System.setProperty("os.name", "Microsoft Windows");
        GccLinker linker = GccLinker.getInstance();
        OutputTypeEnum outputType = new OutputTypeEnum();
        outputType.setValue("plugin");
        LinkType linkType = new LinkType();
        linkType.setOutputType(outputType);
        Linker pluginLinker = linker.getLinker(linkType);
        assertEquals("libfoo.so", pluginLinker.getOutputFileNames("foo", null)[0]);
    }
    public void testGetLinkerNonDarwinShared() {
        System.setProperty("os.name", "Microsoft Windows");
        GccLinker linker = GccLinker.getInstance();
        OutputTypeEnum outputType = new OutputTypeEnum();
        outputType.setValue("shared");
        LinkType linkType = new LinkType();
        linkType.setOutputType(outputType);
        Linker sharedLinker = linker.getLinker(linkType);
        assertEquals("libfoo.so", sharedLinker.getOutputFileNames("foo", null)[0]);
    }
}
