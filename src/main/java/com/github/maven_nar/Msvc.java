package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private final Set<String> paths = new LinkedHashSet<String>();

  @Parameter
  private String version;

  @Parameter
  private File windowsSdkHome;

  @Parameter
  private String windowsSdkVersion;

  private boolean addIncludePath(final CCTask task, final File home, final String subDirectory)
      throws MojoExecutionException {
    if (home == null) {
      return false;
    }
    final File file = new File(home, subDirectory);
    try {
      if (file.exists()) {
        final SystemIncludePath includePath = task.createSysIncludePath();
        final String fullPath = file.getCanonicalPath();
        includePath.setPath(fullPath);
        return true;
      }
      return false;
    } catch (final IOException e) {
      throw new MojoExecutionException("Unable to add system include: " + file.getAbsolutePath(), e);
    }
  }

  private boolean addPath(final File home, final String path) {
    if (home != null) {
      final File directory = new File(home, path);
      if (directory.exists()) {
        try {
          final String fullPath = directory.getCanonicalPath();
          this.paths.add(fullPath);
          return true;
        } catch (final IOException e) {
          throw new IllegalArgumentException("Unable to get path: " + directory, e);
        }
      }
    }
    return false;
  }

  private int compareVersion(final String version1, final String version2) {
    return version1.replace(".", "").compareTo(version2.replace(".", ""));
  }

  public void configureCCTask(final NarCompileMojo mojo, final CCTask task) throws MojoExecutionException {
    final String os = mojo.getOS();
    if (os.equals(OS.WINDOWS)) {
      addIncludePath(task, this.home, "VC/include");
      if (compareVersion(this.windowsSdkVersion, "7.1A") <= 0) {
        addIncludePath(task, this.windowsSdkHome, "include");
      } else {
        addIncludePath(task, this.windowsSdkHome, "include/shared");
        addIncludePath(task, this.windowsSdkHome, "include/um");
      }
      task.addEnv(getPathVariable());
    }
  }

  public void configureLinker(final AbstractNarMojo mojo, final LinkerDef linker) throws MojoExecutionException {
    final String os = mojo.getOS();
    if (os.equals(OS.WINDOWS)) {
      final String arch = mojo.getArchitecture();
      if (compareVersion(this.windowsSdkVersion, "7.1A") < 0) {
        if ("x86".equals(arch)) {
          linker.addLibraryDirectory(this.home, "VC/lib");
          linker.addLibraryDirectory(this.windowsSdkHome, "lib");
        } else {
          throw new MojoExecutionException("Architure " + arch + " not supported for Windows SDK "
              + this.windowsSdkVersion);
        }
      } else {
        if ("x86".equals(arch)) {
          linker.addLibraryDirectory(this.home, "VC/lib");
        } else {
          linker.addLibraryDirectory(this.home, "VC/lib/" + arch);
        }
        String sdkArch = arch;
        if ("amd64".equals(arch)) {
          sdkArch = "x64";
        }
        if (compareVersion(this.windowsSdkVersion, "7.1A") == 0) {
          if ("x86".equals(arch)) {
            linker.addLibraryDirectory(this.windowsSdkHome, "lib");
          } else {
            linker.addLibraryDirectory(this.windowsSdkHome, "lib/" + sdkArch);
          }
        } else if (compareVersion(this.windowsSdkVersion, "8.0") == 0) {
          linker.addLibraryDirectory(this.windowsSdkHome, "Lib/Win8/um/" + sdkArch);
        } else {
          linker.addLibraryDirectory(this.windowsSdkHome, "Lib/Winv6.3/um/" + sdkArch);
        }
      }
    }
  }

  public Variable getPathVariable() {
    if (this.paths.isEmpty()) {
      return null;
    }
    final Variable pathVariable = new Variable();
    pathVariable.setKey("PATH");
    final StringBuilder string = new StringBuilder();
    boolean first = true;
    for (final String path : this.paths) {
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

  public String getVersion() {
    return this.version;
  }

  public String getWindowsSdkVersion() {
    return this.windowsSdkVersion;
  }

  private void init() throws MojoFailureException, MojoExecutionException {
    final String mojoOs = this.mojo.getOS();
    if (NarUtil.isWindows() && OS.WINDOWS.equals(mojoOs)) {
      initVisualStudio();
      initWindowsSdk();
      initPath();
    } else {
      this.version = "";
      this.windowsSdkVersion = "";
    }
  }

  private void initPath() throws MojoExecutionException {
    final String mojoArchitecture = this.mojo.getArchitecture();
    final String osArchitecture = NarUtil.getArchitecture(null);

    addPath(this.home, "Common7/Tools");
    addPath(this.home, "Common7/IDE");

    if (osArchitecture.equals(mojoArchitecture)) {
      if ("x86".equals(osArchitecture)) {
        addPath(this.home, "VC/bin");
      } else {
        if (!addPath(this.home, "VC/bin/" + osArchitecture)) {
          throw new MojoExecutionException("Unable to find compiler for architecture " + mojoArchitecture + ".\n"
              + new File(this.home, "VC/bin/" + osArchitecture));
        }
      }
      addPath(this.home, "VC/bin/x86_" + osArchitecture);
    } else {
      if (!addPath(this.home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture)) {
        throw new MojoExecutionException("Unable to find compiler for architecture " + mojoArchitecture + ".\n"
            + new File(this.home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture));
      }
    }

    if (compareVersion(this.windowsSdkVersion, "7.1A") <= 0) {
      addPath(this.windowsSdkHome, "bin");
    } else {
      String sdkArch = mojoArchitecture;
      if ("amd64".equals(mojoArchitecture)) {
        sdkArch = "x64";
      }
      addPath(this.windowsSdkHome, "bin/" + sdkArch);
    }
  }

  private void initVisualStudio() throws MojoFailureException, MojoExecutionException {
    if (this.version != null && this.version.trim().length() > 1) {
      String internalVersion;
      if (this.version.matches("\\d\\d.\\d")) {
        internalVersion = this.version.substring(0, 2) + this.version.substring(3, 4);
      } else if (this.version.matches("\\d\\d\\d")) {
        internalVersion = this.version;
        this.version = internalVersion.substring(0, 2) + "." + internalVersion.substring(2, 3);
      } else {
        throw new MojoExecutionException("msvc.version must be the internal version in the form 10.0 or 120");
      }
      if (this.home == null) {
        final String commontToolsVar = System.getenv("VS" + internalVersion + "COMNTOOLS");
        if (commontToolsVar != null && commontToolsVar.trim().length() > 0) {
          final File commonToolsDirectory = new File(commontToolsVar);
          if (commonToolsDirectory.exists()) {
            this.home = commonToolsDirectory.getParentFile().getParentFile();
          }
        }
      }
    } else {
      String maxVersion = "";
      File home = null;
      for (final Entry<String, String> entry : System.getenv().entrySet()) {
        final String key = entry.getKey();
        final String value = entry.getValue();
        final Pattern versionPattern = Pattern.compile("VS(\\d\\d\\d)COMNTOOLS");
        final Matcher matcher = versionPattern.matcher(key);
        if (matcher.matches()) {
          final String version = matcher.group(1);
          if (version.compareTo(maxVersion) > 0) {
            final File commonToolsDirectory = new File(value);
            if (commonToolsDirectory.exists()) {
              maxVersion = version;
              home = commonToolsDirectory.getParentFile().getParentFile();
            }
          }
        }
      }
      if (this.home == null) {
        this.home = home;
      }
      if (maxVersion.length() == 0) {
        final TextStream out = new StringTextStream();
        final TextStream err = new StringTextStream();
        final TextStream dbg = new StringTextStream();

        NarUtil.runCommand("link", new String[] {
            "/?"
        }, null, null, out, err, dbg, null, true);
        final Pattern p = Pattern.compile("\\d+\\.\\d+\\.\\d+(\\.\\d+)?");
        final Matcher m = p.matcher(out.toString());
        if (m.find()) {
          this.version = m.group(0);
        } else {
          throw new MojoExecutionException(
              "msvc.version not specified and no VS<Version>COMNTOOLS environment variable can be found");
        }
      } else {
        this.version = maxVersion.substring(0, 2) + "." + maxVersion.substring(2, 3);
      }
    }
  }

  private void initWindowsSdk() throws MojoExecutionException {
    final boolean hasVersion = this.windowsSdkVersion != null && this.windowsSdkVersion.trim().length() >= 0;
    if (!hasVersion) {
      this.windowsSdkVersion = "";
    }
    String maxVersion = "";
    File home = null;
    for (final File directory : Arrays.asList(new File("C:/Program Files (x86)/Microsoft SDKs/Windows"), new File(
        "C:/Program Files (x86)/Windows Kits"))) {
      if (directory.exists()) {
        final File[] kitDirectories = directory.listFiles();
        if (kitDirectories != null) {
          for (final File kitDirectory : kitDirectories) {
            if (new File(kitDirectory, "Include").exists()) {
              String kitVersion = kitDirectory.getName();
              if (kitVersion.charAt(0) == 'v') {
                kitVersion = kitVersion.substring(1);
              }
              if (kitVersion.matches("\\d+.\\d+[A-Z]?")) {
                if (this.windowsSdkVersion.length() == 0) {
                  if (compareVersion(kitVersion, maxVersion) > 0) {
                    maxVersion = kitVersion;
                    home = kitDirectory;
                  }
                } else {
                  if (kitVersion.equals(this.windowsSdkVersion)) {
                    maxVersion = this.windowsSdkVersion;
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
      throw new MojoExecutionException("msvc.windowsSdkVersion not specified and versions can be found");
    }
    this.windowsSdkVersion = maxVersion;
  }

  public void setMojo(final AbstractNarMojo mojo) throws MojoFailureException, MojoExecutionException {
    this.mojo = mojo;
    init();
  }

  @Override
  public String toString() {
    return this.home + "\n" + this.windowsSdkHome;
  }
}
