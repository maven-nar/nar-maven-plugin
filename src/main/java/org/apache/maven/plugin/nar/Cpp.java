// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

/**
 * Cpp compiler tag
 *
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/Cpp.java 631dc18040bb 2007/07/17 14:21:11 duns $
 */
public class Cpp extends Compiler {
	public Cpp() {
	}
	
    public String getName() {
        return "cpp";
    }   
}
