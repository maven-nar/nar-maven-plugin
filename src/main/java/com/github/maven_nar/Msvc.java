package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.types.SystemIncludePath;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;

public class Msvc {

    @Parameter
    private File home;

    @Parameter
    private String version;

    @Parameter
    private File windowsSdkHome;

    @Parameter
    private String windowsSdkVersion;

    public String getWindowsSdkVersion() throws MojoExecutionException {
        init();
        return windowsSdkVersion;
    }

    public String getVersion() throws MojoExecutionException {
        init();
        return version;
    }

    private void init() throws MojoExecutionException {
        if (NarUtil.getOS(null).equals(OS.WINDOWS)) {
            initVisualStudio();
            initWindowsSdk();
        } else {
            version = "";
            windowsSdkVersion = "";
        }
    }

    private void initVisualStudio() throws MojoExecutionException {
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
                    "visualStudio.version must be the internal version in the form 10.0 or 120");
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
                throw new MojoExecutionException(
                    "visualStudio.version not specified and no VS<Version>COMNTOOLS environment variable can be found");
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
                            if (kitVersion.matches("\\d+.\\d+[A-Z]")) {
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
                "visualStudio.windowsSdkVersion not specified and versions can be found");
        } else {
            windowsSdkVersion = maxVersion;
        }
    }

    private int compareVersion(String version1, String version2) {
        return version1.replace(".", "").compareTo(version2.replace(".", ""));
    }

    public void configureCCTask(NarCompileMojo mojo, CCTask task)
        throws MojoExecutionException {
        String os = mojo.getOS();
        if (os.equals(OS.WINDOWS)) {
            init();
            for (File file : Arrays.asList(new File(home, "VC/bin"), new File(
                home, "Common7/IDE"))) {
                task.addPath("PATH", file);
            }
            addIncludePath(task, home, "VC/include");
            if (compareVersion(windowsSdkVersion, "7.1A") <= 0) {
                addIncludePath(task, windowsSdkHome, "include");
            } else {
                addIncludePath(task, windowsSdkHome, "include/shared");
            }
        }
    }

    public void configureLinker(AbstractNarMojo mojo, LinkerDef linker) throws MojoExecutionException {
        String os = mojo.getOS();
        if (os.equals(OS.WINDOWS)) {
            init();
            String arch = mojo.getArchitecture();
            if ("x86".equals(arch)) {
                linker.addLibraryDirectory(home, "VC/lib");
                linker.addLibraryDirectory(windowsSdkHome, "lib");
            } else {
                linker.addLibraryDirectory(home, "VC/lib/" + arch);
                if (compareVersion(windowsSdkVersion, "7.1A") < 0) {
                    throw new MojoExecutionException("Architecture "
                        + mojo.getArchitecture()
                        + " not supported for Microsoft Windows SDK "
                        + windowsSdkVersion);
                } else {
                    String sdkArch = arch;
                    if ("amd64".equals(arch)) {
                        sdkArch = "x64";
                    }
                    if (compareVersion(windowsSdkVersion, "7.1A") == 0) {
                        linker.addLibraryDirectory(windowsSdkHome, "lib/"
                            + sdkArch);
                    } else if (compareVersion(windowsSdkVersion, "8.0") == 0) {
                        linker.addLibraryDirectory(windowsSdkHome,
                            "Lib/Win8/um" + sdkArch);
                    } else {
                        linker.addLibraryDirectory(windowsSdkHome,
                            "Lib/Winv6.3/um" + sdkArch);
                    }
                }
            }
        }
    }

    private boolean addIncludePath(CCTask task, File home, String subDirectory)
        throws MojoExecutionException {
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
            throw new MojoExecutionException("Unable to add system include: "
                + file.getAbsolutePath(), e);
        }
    }

    @Override
    public String toString() {
        try {
            init();
        } catch (MojoExecutionException e) {
        }
        return home + "\n" + windowsSdkHome;
    }
}
