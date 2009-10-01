// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

/**
 * C compiler tag
 *
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/C.java 631dc18040bb 2007/07/17 14:21:11 duns $
 */
public class C extends Compiler { 
    
	public C() {
	}
	
    public String getName() {
        return "c";
    }   
}
