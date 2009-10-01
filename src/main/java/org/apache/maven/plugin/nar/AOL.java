// Copyright FreeHEP, 2007.
package org.freehep.maven.nar;

public class AOL {

	private String architecture;
	private String os;
	private String linkerName;

	// FIXME, need more complicated parsing for numbers as part of os.
	public AOL(String aol) {
		String[] aolString = aol.split("-", 3);
		switch (aolString.length) {
		case 3:
			linkerName = aolString[2];
		case 2:
			os = aolString[1];
		case 1:
			architecture = aolString[0];
			break;

		default:
			throw new RuntimeException("AOL '"+aol+"' cannot be parsed.");
		}
	}

	public AOL(String architecture, String os, String linkerName) {
		this.architecture = architecture;
		this.os = os;
		this.linkerName = linkerName;
	}

	public String toString() {
		return architecture
				+ ((os == null) ? "" : "-" + os
						+ ((linkerName == null) ? "" : "-" + linkerName));
	}

	// FIXME, maybe change to something like isCompatible (AOL).
	public boolean hasLinker(String linker) {
		return linkerName.equals(linker);
	}

	public String getKey() {
		return architecture
				+ ((os == null) ? "" : "." + os
						+ ((linkerName == null) ? "" : "." + linkerName));
	}
}
