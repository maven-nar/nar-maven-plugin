package com.github.maven_nar;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;

/**
 * Keeps info on a system library
 * 
 * @author Mark Donszelmann
 */
public class SysLib
{
    /**
     * Name of the system library
     * 
     * @parameter default-value=""
     * @required
     */
    private String name;

    /**
     * Type of linking for this system library
     * 
     * @parameter default-value="shared"
     * @required
     */
    private String type = Library.SHARED;

    public final SystemLibrarySet getSysLibSet( Project antProject )
        throws MojoFailureException
    {
        if ( name == null )
        {
            throw new MojoFailureException( "NAR: Please specify <Name> as part of <SysLib>" );
        }
        SystemLibrarySet sysLibSet = new SystemLibrarySet();
        sysLibSet.setProject( antProject );
        sysLibSet.setLibs( new CUtil.StringArrayBuilder( name ) );
        LibraryTypeEnum sysLibType = new LibraryTypeEnum();
        sysLibType.setValue( type );
        sysLibSet.setType( sysLibType );
        return sysLibSet;
    }
}
