package com.github.maven_nar.cpptasks.devstudio;
import org.apache.tools.ant.taskdefs.condition.Os;

import com.github.maven_nar.cpptasks.compiler.AbstractProcessor;
import com.github.maven_nar.cpptasks.compiler.TestAbstractLinker;
import com.github.maven_nar.cpptasks.devstudio.DevStudioLinker;
/**
 * Test for Microsoft Developer Studio linker
 * 
 * Override create to test concrete compiler implementions
 */
public class TestDevStudioLinker extends TestAbstractLinker {
    public TestDevStudioLinker(String name) {
        super(name);
    }
    protected AbstractProcessor create() {
        return DevStudioLinker.getInstance();
    }
    public void testGetIdentfier() {
        if (!Os.isFamily("windows")) {
            return;
        }
        AbstractProcessor compiler = create();
        String id = compiler.getIdentifier();
        boolean hasMSLinker = ((id.indexOf("Microsoft") >= 0) && (id
                .indexOf("Linker") >= 0))
                || id.indexOf("link") >= 0;
        assertTrue(hasMSLinker);
    }
}
