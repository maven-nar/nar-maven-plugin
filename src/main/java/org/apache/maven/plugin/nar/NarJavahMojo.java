// Copyright FreeHEP, 2005.
package org.freehep.maven.nar;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Compiles class files into c/c++ headers using "javah". 
 * Any class file that contains methods that were declared
 * "native" will be run through javah.
 *
 * @goal nar-javah
 * @phase compile
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarJavahMojo.java eeac31f37379 2007/07/24 04:02:00 duns $
 */
public class NarJavahMojo extends AbstractCompileMojo {
    
    public void execute() throws MojoExecutionException, MojoFailureException {
    	if (shouldSkip()) return;
    	
        getJavah().execute();
    }    
}