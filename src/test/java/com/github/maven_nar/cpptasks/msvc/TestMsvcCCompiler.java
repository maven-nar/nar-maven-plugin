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
package com.github.maven_nar.cpptasks.msvc;
import java.util.Vector;

import com.github.maven_nar.cpptasks.msvc.MsvcCCompiler;

import junit.framework.TestCase;
/**
 * Test Microsoft C/C++ compiler adapter
 *  
 */
public class TestMsvcCCompiler extends TestCase {
    public TestMsvcCCompiler(String name) {
        super(name);
    }
    public void testDebug() {
        MsvcCCompiler compiler = MsvcCCompiler.getInstance();
        Vector args = new Vector();
        compiler.addDebugSwitch(args);
        assertEquals(4, args.size());
        assertEquals("/Zi", args.elementAt(0));
        assertEquals("/Od", args.elementAt(1));
        assertEquals("/RTC1", args.elementAt(2));
        assertEquals("/D_DEBUG", args.elementAt(3));
    }
}
