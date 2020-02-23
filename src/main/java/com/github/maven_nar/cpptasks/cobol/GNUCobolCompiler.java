/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.cobol;

import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.util.FileUtils;

import com.github.maven_nar.cpptasks.OptimizationEnum;
import com.github.maven_nar.cpptasks.compiler.CommandLineCobolCompiler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;

/**
 * Adapter for the GNUCobol cobol compiler
 *
 * @author Francis ANDRE
 */
/*
 * Usage: cobc [options] file ...
Usage: cobc [options] file ...

Options:
  -help                 Display this message
  -version, -V          Display compiler version
  -info, -i             Display compiler information (build/environment)
  -v                    Display the commands invoked by the compiler
  -x                    Build an executable program
  -m                    Build a dynamically loadable module (default)
  -std=<dialect>        Warnings/features for a specific dialect :
                          cobol2002   Cobol 2002
                          cobol85     Cobol 85
                          ibm         IBM Compatible
                          mvs         MVS Compatible
                          bs2000      BS2000 Compatible
                          mf          Micro Focus Compatible
                          default     When not specified
                        See config/default.conf and config/*.conf
  -free                 Use free source format
  -fixed                Use fixed source format (default)
  -O, -O2, -Os          Enable optimization
  -g                    Enable C compiler debug / stack check / trace
  -debug                Enable all run-time error checking
  -o <file>             Place the output into <file>
  -b                    Combine all input files into a single
                        dynamically loadable module
  -E                    Preprocess only; do not compile or link
  -C                    Translation only; convert COBOL to C
  -S                    Compile only; output assembly file
  -c                    Compile and assemble, but do not link
  -P(=<dir or file>)    Generate preprocessed program listing (.lst)
  -Xref                 Generate cross reference through 'cobxref'
                        (V. Coen's 'cobxref' must be in path)
  -I <directory>        Add <directory> to copy/include search path
  -L <directory>        Add <directory> to library search path
  -l <lib>              Link the library <lib>
  -A <options>          Add <options> to the C compile phase
  -Q <options>          Add <options> to the C link phase
  -D <define>           DEFINE <define> to the COBOL compiler
  -K <entry>            Generate CALL to <entry> as static
  -conf=<file>          User defined dialect configuration - See -std=
  -cb_conf=tag:value    Override configuration entry
  -list-reserved        Display reserved words
  -list-intrinsics      Display intrinsic functions
  -list-mnemonics       Display mnemonic names
  -list-system          Display system routines
  -save-temps(=<dir>)   Save intermediate files
                        - Default : current directory
  -ext <extension>      Add default file extension

  -W                    Enable ALL warnings
  -Wall                 Enable all warnings except as noted below
  -Wobsolete            Warn if obsolete features are used
  -Warchaic             Warn if archaic features are used
  -Wredefinition        Warn incompatible redefinition of data items
  -Wconstant            Warn inconsistent constant
  -Woverlap             Warn overlapping MOVE items
  -Wparentheses         Warn lack of parentheses around AND within OR
  -Wstrict-typing       Warn type mismatch strictly
  -Wimplicit-define     Warn implicitly defined data items
  -Wcorresponding       Warn CORRESPONDING with no matching items
  -Wexternal-value      Warn EXTERNAL item with VALUE clause
  -Wcall-params         Warn non 01/77 items for CALL params
                        - NOT set with -Wall
  -Wcolumn-overflow     Warn text after column 72, FIXED format
                        - NOT set with -Wall
  -Wterminator          Warn lack of scope terminator END-XXX
                        - NOT set with -Wall
  -Wtruncate            Warn possible field truncation
                        - NOT set with -Wall
  -Wlinkage             Warn dangling LINKAGE items
                        - NOT set with -Wall
  -Wunreachable         Warn unreachable statements
                        - NOT set with -Wall

  -fsign=<value>        Define display sign representation
                        - ASCII or EBCDIC (Default : machine native)
  -ffold-copy=<value>   Fold COPY subject to value
                        - UPPER or LOWER (Default : no transformation)
  -ffold-call=<value>   Fold PROGRAM-ID, CALL, CANCEL subject to value
                        - UPPER or LOWER (Default : no transformation)
  -fdefaultbyte=<value> Initialize fields without VALUE to decimal value
                        - 0 to 255 (Default : initialize to picture)
  -fintrinsics=<value>  Intrinsics to be used without FUNCTION keyword
                        - ALL or intrinsic function name (,name,...)
  -ftrace               Generate trace code
                        - Executed SECTION/PARAGRAPH
  -ftraceall            Generate trace code
                        - Executed SECTION/PARAGRAPH/STATEMENTS
                        - Turned on by -debug
  -fsyntax-only         Syntax error checking only; don't emit any output
  -fdebugging-line      Enable debugging lines
                        - 'D' in indicator column or floating >>D
  -fsource-location     Generate source location code
                        - Turned on by -debug/-g/-ftraceall
  -fimplicit-init       Automatic initialization of the Cobol runtime system
  -fstack-check         PERFORM stack checking
                        - Turned on by -debug or -g
  -fsyntax-extension    Allow syntax extensions
                        - eg. Switch name SW1, etc.
  -fwrite-after         Use AFTER 1 for WRITE of LINE SEQUENTIAL
                        - Default : BEFORE 1
  -fmfcomment           '*' or '/' in column 1 treated as comment
                        - FIXED format only
  -facucomment          '$' in indicator area treated as '*',
                        '|' treated as floating comment
  -fnotrunc             Allow numeric field overflow
                        - Non-ANSI behaviour
  -fodoslide            Adjust items following OCCURS DEPENDING
                        - Requires implicit/explicit relaxed syntax
  -fsingle-quote        Use a single quote (apostrophe) for QUOTE
                        - Default : double quote
  -frecursive-check     Check recursive program call
  -frelax-syntax        Relax syntax checking
                        - eg. REDEFINES position
  -foptional-file       Treat all files as OPTIONAL
                        - unless NOT OPTIONAL specified */
public final class GNUCobolCompiler extends CommandLineCobolCompiler {
	private final static String[] headerExtensions = new String[] { ".cpy", ".inc" };
	private final static String[] sourceExtensions = new String[] { ".cob", ".cbl" };

	private static final GNUCobolCompiler instance = new GNUCobolCompiler("cobc", sourceExtensions, headerExtensions,
			false, null);

	/**
	 * Gets singleton instance of this class
	 */
	public static GNUCobolCompiler getInstance() {
		return instance;
	}

	private String identifier;
	private File[] includePath;

	/**
	 * Private constructor. Use getInstance() to get singleton instance of this
	 * class.
	 */
	private GNUCobolCompiler(final String command, final String[] sourceExtensions, final String[] headerExtensions,
			final boolean newEnvironment, final Environment env) {
		super(command, "-help", sourceExtensions, headerExtensions, ".c", false, null, newEnvironment, env);
	}

	@Override
	public void addImpliedArgs(final Vector<String> args, final boolean debug, final boolean multithreaded,
			final boolean exceptions, final LinkType linkType, final Boolean rtti,
			final OptimizationEnum optimization) {
//		args.addElement("-c");
		if (debug) {
			args.addElement("-g");
		}
		if (linkType.isSharedLibrary()) {
//			args.addElement("-m");
		}
	}

	@Override
	public void addWarningSwitch(final Vector<String> args, final int level) {
		switch (level) {
		case 0:
		default:
			args.addElement("-w");
			break;
		}
	}

	/**
	 * Gets identifier for the compiler.
	 * 
	 * Initial attempt at extracting version information would lock up. Using a
	 * stock response.
	 */
	@Override
	public String getIdentifier() {
		return "GNU Cobol Compiler";
	}

	@Override
	public Linker getLinker(final LinkType linkType) {
		return GNUCobolLinker.getInstance().getLinker(linkType);
	}

	@Override
	public int getMaximumCommandLength() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected void getDefineSwitch(StringBuffer buffer, String define, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected File[] getEnvironmentIncludePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getIncludeDirSwitch(String source) {
		return "-I" + source;
	}

	@Override
	protected void getUndefineSwitch(StringBuffer buffer, String define) {
		// TODO Auto-generated method stub

	}

	@Override
	  protected int getArgumentCountPerInputFile() {
	    return 3;
	  }
	@Override
	protected String getInputFileArgument(final File outputDir, final String filename, final int index) {
	    switch (index) {
	      case 0:
	        return "-o";
	      case 1:
	        final String outputFileName = getOutputFileNames(filename, null)[0];
	        return new File(outputDir, outputFileName).toString();

	      case 2:
	        return filename;

	      default:
	        return null;
	    }
	}
}
