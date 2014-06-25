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
import java.util.Vector;

import com.github.maven_nar.cpptasks.gcc.GccCompatibleCCompiler;

import junit.framework.TestCase;
/**
 * Tests for gcc compatible compilers
 * 
 * @author CurtA
 */
public abstract class TestGccCompatibleCCompiler extends TestCase {
    /**
     * Constructor
     * 
     * @param name
     *            test case name
     */
    public TestGccCompatibleCCompiler(String name) {
        super(name);
    }
    /**
     * Compiler creation method
     * 
     * Must be overriden by extending classes
     * 
     * @return GccCompatibleCCompiler
     */
    protected abstract GccCompatibleCCompiler create();
    /**
     * Tests command lines switches for warning = 0
     */
    public void testWarningLevel0() {
        GccCompatibleCCompiler compiler = create();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 0);
        assertEquals(1, args.size());
        assertEquals("-w", args.elementAt(0));
    }
    /**
     * Tests command lines switches for warning = 1
     */
    public void testWarningLevel1() {
        GccCompatibleCCompiler compiler = create();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 1);
        assertEquals(0, args.size());
    }
    /**
     * Tests command lines switches for warning = 2
     */
    public void testWarningLevel2() {
        GccCompatibleCCompiler compiler = create();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 2);
        assertEquals(0, args.size());
    }
    /**
     * Tests command lines switches for warning = 3
     */
    public void testWarningLevel3() {
        GccCompatibleCCompiler compiler = create();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 3);
        assertEquals(1, args.size());
        assertEquals("-Wall", args.elementAt(0));
    }
    /**
     * Tests command lines switches for warning = 4
     */
    public void testWarningLevel4() {
        GccCompatibleCCompiler compiler = create();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 4);
        assertEquals(2, args.size());
        assertEquals("-W", args.elementAt(0));
        assertEquals("-Wall", args.elementAt(1));
    }
    /**
     * Tests command lines switches for warning = 5
     */
    public void testWarningLevel5() {
        GccCompatibleCCompiler compiler = create();
        Vector args = new Vector();
        compiler.addWarningSwitch(args, 5);
        assertEquals(3, args.size());
        assertEquals("-Werror", args.elementAt(0));
        assertEquals("-W", args.elementAt(1));
        assertEquals("-Wall", args.elementAt(2));
    }
}
