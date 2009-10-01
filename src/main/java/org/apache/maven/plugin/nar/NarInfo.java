// Copyright FreeHEP, 2006-2007.
package org.freehep.maven.nar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.logging.Log;

/**
 * 
 * @author Mark Donszelmann
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarInfo.java 0ee9148b7c6a 2007/09/20 18:42:29 duns $
 */
public class NarInfo {

	public static final String NAR_PROPERTIES = "nar.properties";
	private String groupId, artifactId, version;
	private Properties info;
	private Log log;

	public NarInfo(String groupId, String artifactId, String version, Log log) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.log = log;
		info = new Properties();

		// Fill with general properties.nar file
		File propertiesDir = new File("src/main/resources/META-INF/nar/"
				+ groupId + "/" + artifactId);
		if (!propertiesDir.exists()) {
			propertiesDir.mkdirs();
		}
		File propertiesFile = new File(propertiesDir, NarInfo.NAR_PROPERTIES);
		try {
			info.load(new FileInputStream(propertiesFile));
		} catch (IOException ioe) {
			// ignored
		}

	}

	public String toString() {
		StringBuffer s = new StringBuffer("NarInfo for ");
		s.append(groupId);
		s.append(":");
		s.append(artifactId);
		s.append("-");
		s.append(version);
		s.append(" {\n");

		for (Iterator i = info.keySet().iterator(); i.hasNext();) {
			String key = (String) i.next();
			s.append("   ");
			s.append(key);
			s.append("='");
			s.append(info.getProperty(key, "<null>"));
			s.append("'\n");
		}

		s.append("}\n");
		return s.toString();
	}

	public boolean exists(JarFile jar) {
		return getNarPropertiesEntry(jar) != null;
	}

	public void read(JarFile jar) throws IOException {
		info.load(jar.getInputStream(getNarPropertiesEntry(jar)));
	}

	private JarEntry getNarPropertiesEntry(JarFile jar) {
		return jar.getJarEntry("META-INF/nar/" + groupId + "/" + artifactId
				+ "/" + NAR_PROPERTIES);
	}

	/**
	 * No binding means default binding.
	 * 
	 * @param aol
	 * @return
	 */
	public String getBinding(AOL aol, String defaultBinding) {
		return getProperty(aol, "libs.binding", defaultBinding);
	}

	public void setBinding(AOL aol, String value) {
		setProperty(aol, "libs.binding", value);
	}

	// FIXME replace with list of AttachedNarArtifacts
	public String[] getAttachedNars(AOL aol, String type) {
		String attachedNars = getProperty(aol, "nar." + type);
		return attachedNars != null ? attachedNars.split(",") : null;
	}

	public void addNar(AOL aol, String type, String nar) {
		String nars = getProperty(aol, "nar." + type);
		nars = (nars == null) ? nar : nars + ", " + nar;
		setProperty(aol, "nar." + type, nars);
	}

	public void setNar(AOL aol, String type, String nar) {
		setProperty(aol, "nar." + type, nar);
	}

	public AOL getAOL(AOL aol) {
		return aol == null ? null : new AOL(getProperty(aol, aol.toString(), aol.toString()));
	}

	public String getOptions(AOL aol) {
		return getProperty(aol, "linker.options");
	}

	public String getLibs(AOL aol) {
		return getProperty(aol, "libs.names", artifactId + "-" + version);
	}

	public String getSysLibs(AOL aol) {
		return getProperty(aol, "syslibs.names");
	}

	public void writeToFile(File file) throws IOException {
		info.store(new FileOutputStream((file)), "NAR Properties for "
				+ groupId + "." + artifactId + "-" + version);
	}

	private void setProperty(AOL aol, String key, String value) {
		if (aol == null) {
			info.setProperty(key, value);
		} else {
			info.setProperty(aol.toString() + "." + key, value);
		}
	}

	public String getProperty(AOL aol, String key) {
		return getProperty(aol, key, (String)null);
	}
	
	public String getProperty(AOL aol, String key, String defaultValue) {
		if (key == null)
			return defaultValue;
		String value = info.getProperty(key, defaultValue);
		value = aol == null ? value : info.getProperty(aol.toString() + "."
				+ key, value);
		log.debug("getProperty(" + aol + ", " + key + ", "
				+ defaultValue + ") = " + value);
		return value;
	}

	public int getProperty(AOL aol, String key, int defaultValue) {
		return Integer.parseInt(getProperty(aol, key, Integer.toString(defaultValue)));
	}

	public boolean getProperty(AOL aol, String key, boolean defaultValue) {
		return Boolean.parseBoolean(getProperty(aol, key, String.valueOf(defaultValue)));
	}

	public File getProperty(AOL aol, String key, File defaultValue) {
		return new File(getProperty(aol, key, defaultValue.getPath()));
	}
}
