package com.github.maven_nar;

import java.io.File;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public class NarFileLayout10
    implements NarFileLayout
{

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarFileLayout#getIncludeDirectory()
     */
    public String getIncludeDirectory()
    {
        return "include";
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarFileLayout#getLibDirectory(java.lang.String, java.lang.String)
     */
    public String getLibDirectory( String aol, String type )
    {
        return "lib" + File.separator + aol + File.separator + type;
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.NarFileLayout#getBinDirectory(java.lang.String)
     */
    public String getBinDirectory( String aol )
    {
        return "bin" + File.separator + aol;
    }
}
