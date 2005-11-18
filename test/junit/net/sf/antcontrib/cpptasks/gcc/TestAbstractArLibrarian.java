/*
 * 
 * Copyright 2003-2004 The Ant-Contrib project
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
package net.sf.antcontrib.cpptasks.gcc;
import net.sf.antcontrib.cpptasks.compiler.AbstractProcessor;
import net.sf.antcontrib.cpptasks.compiler.TestAbstractLinker;
/**
 * Tests for classes that derive from AbstractArLibrarian
 * 
 * @author CurtA
 */
public class TestAbstractArLibrarian extends TestAbstractLinker {
    /**
     * Constructor
     * 
     * @param name
     *            test name
     * @see junit.framework.TestCase#TestCase(String)
     */
    public TestAbstractArLibrarian(String name) {
        super(name);
    }
    /**
     * Creates item under test @returns item under test
     * 
     * @see net.sf.antcontrib.cpptasks.compiler.TestAbstractProcessor#create()
     */
    protected AbstractProcessor create() {
        return GccLibrarian.getInstance();
    }
    /**
     * Override of
     * 
     * @see net.sf.antcontrib.cpptasks.compiler.TestAbstractProcessor#testBid()
     */
    public void testBid() {
        AbstractProcessor compiler = create();
        int bid = compiler.bid("c:/foo\\bar\\hello.o");
        assertEquals(AbstractProcessor.DEFAULT_PROCESS_BID, bid);
    }
    public void testGetIdentfier() {
        AbstractProcessor processor = create();
        String id = processor.getIdentifier();
        assertTrue(id.indexOf("ar") >= 0);
    }
    /**
     * Tests for library patterns
     * 
     * See patch [ 676276 ] Enhanced support for Mac OS X
     */
    public void testGetLibraryPatterns() {
        String[] libnames = new String[]{"foo"};
        String[] patterns = ((AbstractArLibrarian) create())
                .getLibraryPatterns(libnames, null);
        assertEquals(0, patterns.length);
    }
    /**
     * Tests output file for ar library
     * 
     * See bug [ 687732 ] Filenames for gcc static library does start with lib
     */
    public void testOutputFileName() {
        String[] outputFiles = GccLibrarian.getInstance().getOutputFileNames("x", null);
        assertEquals("libx.a", outputFiles[0]);
    }
}
