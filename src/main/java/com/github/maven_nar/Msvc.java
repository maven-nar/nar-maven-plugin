package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.types.Environment.Variable;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.types.SystemIncludePath;

public class Msvc {
    @Parameter
    private File home;

    private AbstractNarMojo mojo;

    private Set<String> paths = new LinkedHashSet<String>();

    @Parameter
    private String version;

    @Parameter
    private File windowsSdkHome;

    @Parameter
    private String windowsSdkVersion;

    private boolean addIncludePath(CCTask task, File home, String subDirectory)
        throws MojoExecutionException {
        if (home == null) {
            return false;
        } else {
            File file = new File(home, subDirectory);
            try {
                if (file.exists()) {
                    SystemIncludePath includePath = task.createSysIncludePath();
                    String fullPath = file.getCanonicalPath();
                    includePath.setPath(fullPath);
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                throw new MojoExecutionException(
                    "Unable to add system include: " + file.getAbsolutePath(),
                    e);
            }
        }
    }

    private void addPath(File home, String path) throws MojoExecutionException {
        if (home != null) {
            File directory = new File(home, path);
            if (directory.exists()) {
                try {
                    paths.add(directory.getCanonicalPath());
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to get path: "
                        + directory, e);
                }
            }
        }
    }

    private int compareVersion(String version1, String version2) {
        return version1.replace(".", "").compareTo(version2.replace(".", ""));
    }

    public void configureCCTask(NarCompileMojo mojo, CCTask task)
        throws MojoExecutionException {
        String os = mojo.getOS();
        if (os.equals(OS.WINDOWS)) {
            addIncludePath(task, home, "VC/include");
            if (compareVersion(windowsSdkVersion, "7.1A") <= 0) {
                addIncludePath(task, windowsSdkHome, "include");
            } else {
                addIncludePath(task, windowsSdkHome, "include/shared");
                addIncludePath(task, windowsSdkHome, "include/um");
            }
            task.addEnv(getPathVariable());
        }
    }

    public void configureLinker(AbstractNarMojo mojo, LinkerDef linker)
        throws MojoExecutionException {
        String os = mojo.getOS();
        if (os.equals(OS.WINDOWS)) {
            String arch = mojo.getArchitecture();
            if (compareVersion(windowsSdkVersion, "7.1A") < 0) {
                if ("x86".equals(arch)) {
                    linker.addLibraryDirectory(home, "VC/lib");
                    linker.addLibraryDirectory(windowsSdkHome, "lib");
                } else {
                    throw new MojoExecutionException("Architure " + arch
                        + " not supported for Windows SDK " + windowsSdkVersion);
                }
            } else {
                if ("x86".equals(arch)) {
                    linker.addLibraryDirectory(home, "VC/lib");
                } else {
                    linker.addLibraryDirectory(home, "VC/lib/" + arch);
                }
                String sdkArch = arch;
                if ("amd64".equals(arch)) {
                    sdkArch = "x64";
                }
                if (compareVersion(windowsSdkVersion, "7.1A") == 0) {
                    if ("x86".equals(arch)) {
                        linker.addLibraryDirectory(windowsSdkHome, "lib");
                    } else {
                        linker.addLibraryDirectory(windowsSdkHome, "lib/"
                            + sdkArch);
                    }
                } else if (compareVersion(windowsSdkVersion, "8.0") == 0) {
                    linker.addLibraryDirectory(windowsSdkHome, "Lib/Win8/um/"
                        + sdkArch);
                } else {
                    linker.addLibraryDirectory(windowsSdkHome,
                        "Lib/Winv6.3/um/" + sdkArch);
                }
            }
        }
    }

    public Variable getPathVariable() {
        if (paths.isEmpty()) {
            return null;
        } else {
            Variable pathVariable = new Variable();
            pathVariable.setKey("PATH");
            StringBuilder string = new StringBuilder();
            boolean first = true;
            for (String path : paths) {
                if (first) {
                    first = false;
                } else {
                    string.append(";");
                }
                string.append(path);
            }
            pathVariable.setValue(string.toString());
            return pathVariable;
        }
    }

    public String getVersion() throws MojoExecutionException {
        return version;
    }

    public String getWindowsSdkVersion() throws MojoExecutionException {
        return windowsSdkVersion;
    }

    private void init() throws MojoFailureException, MojoExecutionException {
        String mojoOs = mojo.getOS();
        if (NarUtil.getOS(null).equals(OS.WINDOWS) && OS.WINDOWS.equals(mojoOs)) {
            initVisualStudio();
            initWindowsSdk();
            initPath();
        } else {
            version = "";
            windowsSdkVersion = "";
        }
    }

    private void initPath() throws MojoExecutionException {
        String mojoArchitecture = mojo.getArchitecture();
        String osArchitecture = NarUtil.getArchitecture(null);

        addPath(home, "Common7/Tools");
        addPath(home, "Common7/IDE");

        if (compareVersion(windowsSdkVersion, "7.1A") <= 0) {
            addPath(home, "VC/bin");
            addPath(windowsSdkHome, "bin");
        } else {
            if (!osArchitecture.equals(mojoArchitecture)) {
                addPath(home, "VC/bin/" + osArchitecture + "_"
                    + mojoArchitecture);
            }
            if ("x86".equals(osArchitecture)) {
                addPath(home, "VC/bin");
            } else {
                addPath(home, "VC/bin/" + osArchitecture);
            }
            String sdkArch = mojoArchitecture;
            if ("amd64".equals(mojoArchitecture)) {
                sdkArch = "x64";
            }
            addPath(windowsSdkHome, "bin/" + sdkArch);
        }
    }

    private void initVisualStudio() throws MojoFailureException,
        MojoExecutionException {
        if (version != null && version.trim().length() > 1) {
            String internalVersion;
            if (version.matches("\\d\\d.\\d")) {
                internalVersion = version.substring(0, 2)
                    + version.substring(3, 4);
            } else if (version.matches("\\d\\d\\d")) {
                internalVersion = version;
                version = internalVersion.substring(0, 2) + "."
                    + internalVersion.substring(2, 3);
            } else {
                throw new MojoExecutionException(
                    "msvc.version must be the internal version in the form 10.0 or 120");
            }
            if (this.home == null) {
                String commontToolsVar = System.getenv("VS" + internalVersion
                    + "COMNTOOLS");
                if (commontToolsVar != null
                    && commontToolsVar.trim().length() > 0) {
                    File commonToolsDirectory = new File(commontToolsVar);
                    if (commonToolsDirectory.exists()) {
                        home = commonToolsDirectory.getParentFile()
                            .getParentFile();
                    }
                }
            }
        } else {
            String maxVersion = "";
            File home = null;
            for (Entry<String, String> entry : System.getenv().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Pattern versionPattern = Pattern.compile("VS(\\d\\d\\d)COMNTOOLS");
                Matcher matcher = versionPattern.matcher(key);
                if (matcher.matches()) {
                    String version = matcher.group(1);
                    if (version.compareTo(maxVersion) > 0) {
                        File commonToolsDirectory = new File(value);
                        if (commonToolsDirectory.exists()) {
                            maxVersion = version;
                            home = commonToolsDirectory.getParentFile()
                                .getParentFile();
                        }
                    }
                }
            }
            if (this.home == null) {
                this.home = home;
            }
            if (maxVersion.length() == 0) {
                TextStream out = new StringTextStream();
                TextStream err = new StringTextStream();
                TextStream dbg = new StringTextStream();

                NarUtil.runCommand("link", new String[] {
                    "/?"
                }, null, null, out, err, dbg, null, true);
                Pattern p = Pattern.compile("\\d+\\.\\d+\\.\\d+(\\.\\d+)?");
                Matcher m = p.matcher(out.toString());
                if (m.find()) {
                    version = m.group(0);
                } else {
                    throw new MojoExecutionException(
                        "msvc.version not specified and no VS<Version>COMNTOOLS environment variable can be found");
                }
            } else {
                version = maxVersion.substring(0, 2) + "."
                    + maxVersion.substring(2, 3);
            }
        }
    }

    private void initWindowsSdk() throws MojoExecutionException {
        boolean hasVersion = windowsSdkVersion != null
            && windowsSdkVersion.trim().length() >= 0;
        if (!hasVersion) {
            windowsSdkVersion = "";
        }
        String maxVersion = "";
        File home = null;
        for (File directory : Arrays.asList(new File(
            "C:/Program Files (x86)/Microsoft SDKs/Windows"), new File(
            "C:/Program Files (x86)/Windows Kits"))) {
            if (directory.exists()) {
                File[] kitDirectories = directory.listFiles();
                if (kitDirectories != null) {
                    for (File kitDirectory : kitDirectories) {
                        if (new File(kitDirectory, "Include").exists()) {
                            String kitVersion = kitDirectory.getName();
                            if (kitVersion.charAt(0) == 'v') {
                                kitVersion = kitVersion.substring(1);
                            }
                            if (kitVersion.matches("\\d+.\\d+[A-Z]?")) {
                                if (windowsSdkVersion.length() == 0) {
                                    if (compareVersion(kitVersion, maxVersion) > 0) {
                                        maxVersion = kitVersion;
                                        home = kitDirectory;
                                    }
                                } else {
                                    if (kitVersion.equals(windowsSdkVersion)) {
                                        maxVersion = windowsSdkVersion;
                                        home = kitDirectory;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (this.windowsSdkHome == null) {
            this.windowsSdkHome = home;
        }
        if (maxVersion.length() == 0) {
            throw new MojoExecutionException(
                "msvc.windowsSdkVersion not specified and versions can be found");
        } else {
            windowsSdkVersion = maxVersion;
        }
    }

    public void setMojo(AbstractNarMojo mojo) throws MojoFailureException,
        MojoExecutionException {
        this.mojo = mojo;
        init();
    }

    @Override
    public String toString() {
        return home + "\n" + windowsSdkHome;
    }
}
