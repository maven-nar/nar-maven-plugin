package com.github.maven_nar;

/**
 * @author Mark Donszelmann
 * @version $Id$
 */
public class AOL
{

    private String architecture;

    private String os;

    private String linkerName;

    // FIXME, need more complicated parsing for numbers as part of os.
    public AOL( String aol )
    {
        final int linkerIndex = 2;
        final int osIndex = 1;
        final int architectureIndex = 0; 
        
        String[] aolString = aol.split( "-", linkerIndex+1 );
        switch ( aolString.length )
        {
            case linkerIndex+1:
                linkerName = aolString[linkerIndex];
            case osIndex+1:
                os = aolString[osIndex];
            case architectureIndex+1:
                architecture = aolString[architectureIndex];
            break;

        default:
                throw new IllegalArgumentException( "AOL '" + aol + "' cannot be parsed." );
        }
    }

    public AOL( String architecture, String os, String linkerName )
    {
        this.architecture = architecture;
        this.os = os;
        this.linkerName = linkerName;
    }

    /**
     * Returns an AOL string (arch-os-linker) to use as directory or file.
     * @return dash separated AOL
     */
    public final String toString()
    {
        String tempLinkerName = null;
        if ( linkerName == null ) {
            tempLinkerName = "";
        } else if ( linkerName.equals("g++") ) {
            tempLinkerName = "-gpp";
        } else {
            tempLinkerName = "-" + linkerName;
        }
        
        return architecture
                + ((os == null) ? "" : "-" + os
                        + tempLinkerName);
    }

    // FIXME, maybe change to something like isCompatible (AOL).
    public final boolean hasLinker( String linker )
    {
        return linkerName.equals(linker);
    }

    /**
     * Returns an AOL key (arch.os.linker) to search in the properties files. 
     * @return dot separated AOL
     */
    public final String getKey() 
    {
        String tempLinkerName = null;
        if ( linkerName == null ) {
            tempLinkerName = "";
        } else if ( linkerName.equals("g++") ) {
            tempLinkerName = ".gpp";
        } else {
            tempLinkerName = "." + linkerName;
        }
        
        return architecture
                + ((os == null) ? "" : "." + os
                        + tempLinkerName);
    }     

    final String getOS() {
        return os;
    }
}
