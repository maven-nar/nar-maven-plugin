// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.types.LibraryTypeEnum;
import net.sf.antcontrib.cpptasks.types.SystemLibrarySet;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;

/**
 * Keeps info on a system library
 *
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/SysLib.java eda4d0bbde3d 2007/07/03 16:52:10 duns $
 */
public class SysLib {

    /**
     * Name of the system library
     *
     * @parameter expression=""
     * @required
     */
    private String name;

    /**
     * Type of linking for this system library
     *
     * @parameter expression="" default-value="shared"
     * @required
     */
    private String type = Library.SHARED;

    public SystemLibrarySet getSysLibSet(Project antProject) throws MojoFailureException {
        if (name == null) {
            throw new MojoFailureException("NAR: Please specify <Name> as part of <SysLib>");
        }
        SystemLibrarySet sysLibSet = new SystemLibrarySet();
        sysLibSet.setProject(antProject);
        sysLibSet.setLibs(new CUtil.StringArrayBuilder(name));
        LibraryTypeEnum sysLibType = new LibraryTypeEnum();
        sysLibType.setValue(type);
        sysLibSet.setType(sysLibType);
        return sysLibSet;
    }    
}
