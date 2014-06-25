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
package com.github.maven_nar.cpptasks.compiler;
import com.github.maven_nar.cpptasks.OutputTypeEnum;
import com.github.maven_nar.cpptasks.compiler.LinkType;

import junit.framework.TestCase;
/**
 * Tests for LinkType
 * 
 * @author CurtA
 */
public class TestLinkType extends TestCase {
    /**
     * Constructor
     * 
     * @param name
     *            test case name
     */
    public TestLinkType(String name) {
        super(name);
    }
    /**
     * Tests if isPluginModule returns true when set to plugin output type
     * 
     * See patch [ 676276 ] Enhanced support for Mac OS X
     */
    public void testIsPluginFalse() {
        LinkType type = new LinkType();
        OutputTypeEnum pluginType = new OutputTypeEnum();
        pluginType.setValue("executable");
        type.setOutputType(pluginType);
        assertTrue(!type.isPluginModule());
    }
    /**
     * Tests if isPluginModule returns true when set to plugin output type
     * 
     * See patch [ 676276 ] Enhanced support for Mac OS X
     */
    public void testIsPluginTrue() {
        LinkType type = new LinkType();
        OutputTypeEnum pluginType = new OutputTypeEnum();
        pluginType.setValue("plugin");
        type.setOutputType(pluginType);
        assertTrue(type.isPluginModule());
    }
}
