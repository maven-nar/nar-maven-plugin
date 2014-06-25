package com.github.maven_nar.cpptasks;
import com.github.maven_nar.cpptasks.LinkerEnum;

import junit.framework.TestCase;
/**
 * 
 * Tests for LinkerEnum
 * 
 * @author CurtA
 */
public class TestLinkerEnum extends TestCase {
    /**
     * @param name test case name
     */
    public TestLinkerEnum(String name) {
        super(name);
    }
    /**
     * Test checks that enumeration contains value g++
     * 
     * See patch [ 676276 ] Enhanced support for Mac OS X
     */
    public void testContainsValueGpp() {
        assertTrue(new LinkerEnum().containsValue("g++"));
    }
}
