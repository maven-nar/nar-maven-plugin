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
import com.github.maven_nar.cpptasks.OutputTypeEnum;

import junit.framework.TestCase;
/**
 * @author CurtA
 */
public class TestOutputTypeEnum extends TestCase {
    /**
     * Default constructor
     * 
     * @see junit.framework.TestCase#TestCase(String)
     */
    public TestOutputTypeEnum(String name) {
        super(name);
    }
    /**
     * Test checks that output type enum contains "plugin"
     * 
     * See patch [ 676276 ] Enhanced support for Mac OS X
     */
    public void testContainsValuePlugin() {
        assertTrue(new OutputTypeEnum().containsValue("plugin"));
    }
}
