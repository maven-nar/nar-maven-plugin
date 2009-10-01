// Copyright 2005-2007, FreeHEP.
package org.freehep.maven.nar;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PropertyUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @author Mark Donszelmann
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarUtil.java 0ee9148b7c6a 2007/09/20 18:42:29 duns $
 */
public class NarUtil {

	private static Properties defaults;

	public static Properties getDefaults() throws MojoFailureException {
		// read properties file with defaults
		if (defaults == null) {
			defaults = PropertyUtils.loadProperties(NarUtil.class
					.getResourceAsStream("aol.properties"));
		}
		if (defaults == null)
			throw new MojoFailureException(
					"NAR: Could not load default properties file: 'aol.properties'.");

		return defaults;
	}

	public static String getOS(String os) {
		// adjust OS if not given
		if (os == null) {
			os = System.getProperty("os.name");
			if (os.startsWith("Windows"))
				os = OS.WINDOWS;
			if (os.startsWith("windows"))
				os = OS.WINDOWS;
			if (os.equals("Mac OS X"))
				os = OS.MACOSX;
		}
		return os;
	}

	public static String getArchitecture(String architecture) {
		return architecture;
	}

	public static Linker getLinker(Linker linker) {
		if (linker == null) {
			linker = new Linker();
		}
		return linker;
	}

	public static String getLinkerName(String architecture, String os,
			Linker linker) throws MojoFailureException {
		return getLinker(linker).getName(getDefaults(),
				getArchitecture(architecture) + "." + getOS(os) + ".");
	}

	public static AOL getAOL(String architecture, String os, Linker linker,
			String aol) throws MojoFailureException {
		// adjust aol
		return aol == null ? new AOL(getArchitecture(architecture), getOS(os),
				getLinkerName(architecture, os, linker)) : new AOL(aol);
	}

	// FIXME, should go to AOL.
	public static String getAOLKey(String architecture, String os, Linker linker)
			throws MojoFailureException {
		// construct AOL key prefix
		return getArchitecture(architecture) + "." + getOS(os) + "."
				+ getLinkerName(architecture, os, linker) + ".";
	}

	public static String getAOLKey(String aol) {
		// FIXME, this may not always work correctly
		return replace("-", ".", aol);
	}

	public static File getJavaHome(File javaHome, String os) {
		// adjust JavaHome
		if (javaHome == null) {
			javaHome = new File(System.getProperty("java.home"));
			if (!getOS(os).equals("MacOSX")) {
				javaHome = new File(javaHome, "..");
			}
		}
		return javaHome;
	}

	public static void makeExecutable(File file, final Log log)
			throws MojoExecutionException, MojoFailureException {
		if (!file.exists())
			return;

		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				makeExecutable(files[i], log);
			}
		}
		if (file.isFile() && file.canRead() && file.canWrite()
				&& !file.isHidden()) {
			// chmod +x file
			int result = runCommand("chmod", new String[] { "+x",
					file.getPath() }, null, log);
			if (result != 0) {
				throw new MojoExecutionException("Failed to execute 'chmod +x "
						+ file.getPath() + "'" + " return code: \'" + result
						+ "\'.");
			}
		}
	}

	public static void runRanlib(File file, final Log log)
			throws MojoExecutionException, MojoFailureException {
		if (!file.exists()) {
			return;
		}

		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				runRanlib(files[i], log);
			}
		}
		if (file.isFile() && file.canRead() && file.canWrite()
				&& !file.isHidden() && file.getName().endsWith(".a")) {
			// ranlib file
			int result = runCommand("ranlib", new String[] { file.getPath() },
					null, log);
			if (result != 0) {
				throw new MojoExecutionException("Failed to execute 'ranlib "
						+ file.getPath() + "'" + " return code: \'" + result
						+ "\'.");
			}
		}
	}

	/**
	 * Returns the Bcel Class corresponding to the given class filename
	 * 
	 * @param filename
	 *            the absolute file name of the class
	 * @return the Bcel Class.
	 * @throws IOException,
	 *             ClassFormatException
	 */
	public static final JavaClass getBcelClass(String filename)
			throws IOException, ClassFormatException {
		ClassParser parser = new ClassParser(filename);
		return parser.parse();
	}

	/**
	 * Returns the header file name (javah) corresponding to the given class
	 * file name
	 * 
	 * @param filename
	 *            the absolute file name of the class
	 * @return the header file name.
	 */
	public static final String getHeaderName(String base, String filename) {
		base = base.replaceAll("\\\\", "/");
		filename = filename.replaceAll("\\\\", "/");
		if (!filename.startsWith(base)) {
			throw new IllegalArgumentException("Error " + filename
					+ " does not start with " + base);
		}
		String header = filename.substring(base.length() + 1);
		header = header.replaceAll("/", "_");
		header = header.replaceAll("\\.class", ".h");
		return header;
	}

	/**
	 * Replaces target with replacement in string. For jdk 1.4 compatiblity.
	 * 
	 * @param target
	 * @param replacement
	 * @param string
	 * @return
	 */
	public static String replace(CharSequence target, CharSequence replacement,
			String string) {
		return Pattern.compile(quote(target.toString())/*
														 * , Pattern.LITERAL jdk
														 * 1.4
														 */).matcher(string).replaceAll(
		/* Matcher. jdk 1.4 */quoteReplacement(replacement.toString()));
	}

	/* for jdk 1.4 */
	private static String quote(String s) {
		int slashEIndex = s.indexOf("\\E");
		if (slashEIndex == -1)
			return "\\Q" + s + "\\E";

		StringBuffer sb = new StringBuffer(s.length() * 2);
		sb.append("\\Q");
		slashEIndex = 0;
		int current = 0;
		while ((slashEIndex = s.indexOf("\\E", current)) != -1) {
			sb.append(s.substring(current, slashEIndex));
			current = slashEIndex + 2;
			sb.append("\\E\\\\E\\Q");
		}
		sb.append(s.substring(current, s.length()));
		sb.append("\\E");
		return sb.toString();
	}

	/* for jdk 1.4 */
	private static String quoteReplacement(String s) {
		if ((s.indexOf('\\') == -1) && (s.indexOf('$') == -1))
			return s;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\') {
				sb.append('\\');
				sb.append('\\');
			} else if (c == '$') {
				sb.append('\\');
				sb.append('$');
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static final String DEFAULT_EXCLUDES = "**/*~,**/#*#,**/.#*,**/%*%,**/._*,"
			+ "**/CVS,**/CVS/**,**/.cvsignore,"
			+ "**/SCCS,**/SCCS/**,**/vssver.scc,"
			+ "**/.svn,**/.svn/**,**/.DS_Store";

	public static int copyDirectoryStructure(File sourceDirectory,
			File destinationDirectory, String includes, String excludes)
			throws IOException {
		if (!sourceDirectory.exists()) {
			throw new IOException("Source directory doesn't exists ("
					+ sourceDirectory.getAbsolutePath() + ").");
		}

		List files = FileUtils.getFiles(sourceDirectory, includes, excludes);
		String sourcePath = sourceDirectory.getAbsolutePath();

		int copied = 0;
		for (Iterator i = files.iterator(); i.hasNext();) {
			File file = (File) i.next();
			String dest = file.getAbsolutePath();
			dest = dest.substring(sourcePath.length() + 1);
			File destination = new File(destinationDirectory, dest);
			if (file.isFile()) {
				destination = destination.getParentFile();
				FileUtils.copyFileToDirectory(file, destination);
				copied++;
			} else if (file.isDirectory()) {
				if (!destination.exists() && !destination.mkdirs()) {
					throw new IOException(
							"Could not create destination directory '"
									+ destination.getAbsolutePath() + "'.");
				}
				copied += copyDirectoryStructure(file, destination, includes,
						excludes);
			} else {
				throw new IOException("Unknown file type: "
						+ file.getAbsolutePath());
			}
		}
		return copied;
	}

	public static String getEnv(String envKey, String alternateSystemProperty,
			String defaultValue) {
		String envValue = null;
		try {
			envValue = System.getenv(envKey);
			if (envValue == null && alternateSystemProperty != null) {
				envValue = System.getProperty(alternateSystemProperty);
			}
		} catch (Error e) {
			// JDK 1.4?
			if (alternateSystemProperty != null) {
				envValue = System.getProperty(alternateSystemProperty);
			}
		}

		if (envValue == null) {
			envValue = defaultValue;
		}

		return envValue;
	}

	public static String addLibraryPathToEnv(String path, Map environment,
			String os) {
		String pathName = null;
		char separator = ' ';
		if (os.equals(OS.WINDOWS)) {
			pathName = "PATH";
			separator = ';';
		} else if (os.equals(OS.MACOSX)) {
			pathName = "DYLD_LIBRARY_PATH";
			separator = ':';
		} else {
			pathName = "LD_LIBRARY_PATH";
			separator = ':';
		}

		String value = environment != null ? (String) environment.get(pathName)
				: null;
		if (value == null) {
			value = NarUtil.getEnv(pathName, pathName, null);
		}

		path = path.replace(File.pathSeparatorChar, separator);
		if (value != null) {
			value += separator + path;
		} else {
			value = path;
		}
		if (environment != null) {
			environment.put(pathName, value);
		}
		return pathName + "=" + value;
	}

	public static int runCommand(String cmd, String[] args, String[] env,
			Log log) throws MojoExecutionException, MojoFailureException {
		log.debug("RunCommand: " + cmd);
		Commandline cmdLine = new Commandline();
		cmdLine.setExecutable(cmd);
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				log.debug("  '" + args[i] + "'");
			}
			cmdLine.addArguments(args);
		}

		if (env != null) {
			log.debug("with Env:");
			for (int i = 0; i < env.length; i++) {
				String[] nameValue = env[i].split("=", 2);
				if (nameValue.length < 2)
					throw new MojoFailureException("   Misformed env: '"
							+ env[i] + "'");
				log.debug("   '" + env[i] + "'");
				cmdLine.addEnvironment(nameValue[0], nameValue[1]);
			}
		}

		try {
			Process process = cmdLine.execute();
			StreamGobbler errorGobbler = new StreamGobbler(process
					.getErrorStream(), true, log);
			StreamGobbler outputGobbler = new StreamGobbler(process
					.getInputStream(), false, log);

			errorGobbler.start();
			outputGobbler.start();
			process.waitFor();
			return process.exitValue();
		} catch (Throwable e) {
			throw new MojoExecutionException("Could not launch " + cmdLine, e);
		}
	}

	static class StreamGobbler extends Thread {
		InputStream is;
		boolean error;
		Log log;

		StreamGobbler(InputStream is, boolean error, Log log) {
			this.is = is;
			this.error = error;
			this.log = log;
		}

		public void run() {
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (error) {
						log.error(line);
					} else {
						log.debug(line);
					}
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
