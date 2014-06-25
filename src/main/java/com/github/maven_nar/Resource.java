package com.github.maven_nar;

/**
 * Resource compiler tag
 * 
 * @author Greg Domjan
 */
public class Resource
    extends Compiler
{
    public Resource()
    {
    }

    public final String getLanguage()
    {
        return "res";
    }
}
