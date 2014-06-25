package com.github.maven_nar;


/**
 * Defines the layout inside the nar file.
 * 
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public interface NarFileLayout
{
    /**
     * Specifies where libraries are stored
     * 
     * @return
     */
    String getLibDirectory(String aol, String type );

    /**
     * Specifies where includes are stored
     * 
     * @return
     */
    String getIncludeDirectory();

    /**
     * Specifies where binaries are stored
     * 
     * @return
     */
    String getBinDirectory(String aol );
}
