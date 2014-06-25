package com.github.maven_nar.cpptasks.devstudio;
import java.util.Vector;

import com.github.maven_nar.cpptasks.devstudio.DevStudioCCompiler;

import junit.framework.TestCase;
/**
 * Test Microsoft C/C++ compiler adapter
 *  
 */
public class TestDevStudioCCompiler extends TestCase {
    public TestDevStudioCCompiler(String name) {
        super(name);
    }
    public void testDebug() {
        DevStudioCCompiler compiler = DevStudioCCompiler.getInstance();
        Vector args = new Vector();
        compiler.addDebugSwitch(args);
        assertEquals(4, args.size());
        assertEquals("/Zi", args.elementAt(0));
        assertEquals("/Od", args.elementAt(1));
        assertEquals("/RTC1", args.elementAt(2));
        assertEquals("/D_DEBUG", args.elementAt(3));
    }
}
