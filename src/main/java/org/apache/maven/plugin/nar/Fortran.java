// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

/**
 * Fortran compiler tag
 *
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/Fortran.java 0c1f0fc112ac 2007/09/12 18:18:23 duns $
 */
public class Fortran extends Compiler {
  
	public Fortran() {
	}
	
    public String getName() {
        return "fortran";
    }         
}
