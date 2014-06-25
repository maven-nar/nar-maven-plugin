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
