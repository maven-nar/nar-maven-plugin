package com.github.maven_nar.cpptasks.cobol;

import java.io.File;

import com.github.maven_nar.cpptasks.compiler.CommandLineLinker;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.types.LibraryTypeEnum;

public class GNUCobolLibrarian extends CommandLineLinker {
	private static final GNUCobolLibrarian INSTANCE = new GNUCobolLibrarian();
	
	private GNUCobolLibrarian() {
		super("cobc", "--version", new String[] {
			      ".obj"
	    }, new String[] {
	        ".map", ".pdb", ".lnk", ".dll", ".tlb", ".rc", ".h"
	    }, ".lib", false, null);
	}

	@Override
	public File[] getLibraryPath() {
		return null;
	}

	@Override
	public String[] getLibraryPatterns(String[] libnames, LibraryTypeEnum libraryType) {
		return null;
	}

	@Override
	public Linker getLinker(LinkType linkType) {
		return null;
	}

	@Override
	public boolean isCaseSensitive() {
		return false;
	}

	@Override
	protected String getCommandFileSwitch(String commandFile) {
		return null;
	}

	@Override
	protected int getMaximumCommandLength() {
		return 0;
	}

	@Override
	protected String[] getOutputFileSwitch(String outputFile) {
		return null;
	}

	public static Linker getInstance() {
		return INSTANCE;
	}

}
