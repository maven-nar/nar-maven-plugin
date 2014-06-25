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
