package com.github.maven_nar;

/**
 * Stream to write to a string.
 * 
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 * @version $Id$
 */
public class StringTextStream
    implements TextStream
{
    private StringBuffer sb;

    private String lineSeparator;

    public StringTextStream()
    {
        sb = new StringBuffer();
        lineSeparator = System.getProperty( "line.separator", "\n" );
    }

    /*
     * (non-Javadoc)
     * @see com.github.maven_nar.TextStream#println(java.lang.String)
     */
    public final void println( String text )
    {
        sb.append( text );
        sb.append( lineSeparator );
    }

    public final String toString()
    {
        return sb.toString();
    }
}
