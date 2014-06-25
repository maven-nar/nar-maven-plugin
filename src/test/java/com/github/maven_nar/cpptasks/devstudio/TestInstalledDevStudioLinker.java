package com.github.maven_nar.cpptasks.devstudio;

import java.io.File;

import com.github.maven_nar.cpptasks.devstudio.DevStudioLinker;


/**
 * Test for Microsoft Developer Studio linker
 *
 * Override create to test concrete compiler implementions
 */
public class TestInstalledDevStudioLinker extends TestDevStudioLinker
{
     public TestInstalledDevStudioLinker(String name) {
        super(name);
     }

    public void failingtestGetLibraryPath() {
        File[] libpath = DevStudioLinker.getInstance().getLibraryPath();
        //
        //  unless you tweak the library path
        //       it should have more thean three entries
        assertTrue(libpath.length >= 2);
        //
        //   check if these files can be found         
        //
        String[] libnames = new String[] { "kernel32.lib", 
            "advapi32.lib", "msvcrt.lib", "mfc42.lib", "mfc70.lib" };
        boolean[] libfound = new boolean[libnames.length];
        for (int i = 0; i < libpath.length; i++) {
           for (int j = 0; j < libnames.length; j++) {
               File libfile = new File(libpath[i], libnames[j]);
               if (libfile.exists()) {
                  libfound[j] = true;
               }
           }
        }
        assertTrue("kernel32 not found", libfound[0]);
        assertTrue("advapi32 not found", libfound[1]);
        assertTrue("msvcrt not found", libfound[2]);
        if(!(libfound[3] || libfound[4])) {
           fail("mfc42.lib or mfc70.lib not found");
        }
    }
}
