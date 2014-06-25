package com.github.maven_nar;

/**
 * IDL compiler tag
 * 
 * @author Greg Domjan
 */
public class IDL extends Compiler {

	public IDL()
	{
	}
	
	@Override
	protected String getLanguage() {
		return "idl";
	}
}
