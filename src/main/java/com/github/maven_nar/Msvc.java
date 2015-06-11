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
import org.codehaus.plexus.util.StringUtils;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CompilerDef;
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

  private File windowsHome;
  private String toolPathWindowsSDK;
  private String toolPathLinker;

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

  public void configureCCTask(final AbstractNarMojo mojo, final CCTask task) throws MojoExecutionException {
    final String os = mojo.getOS();
    if (os.equals(OS.WINDOWS)) {
      addIncludePath(task, this.home, "VC/include");
      addIncludePath(task, this.home, "VC/atlmfc/include");
      if (compareVersion(this.windowsSdkVersion, "7.1A") <= 0) {
        addIncludePath(task, this.windowsSdkHome, "include");
      } else {
        addIncludePath(task, this.windowsSdkHome, "include/shared");
        addIncludePath(task, this.windowsSdkHome, "include/um");
      }
      task.addEnv(getPathVariable());
      // TODO: supporting running with clean environment - addEnv sets
      // newEnvironemnt by default
      // task.setNewenvironment(false);
      Variable envVariable = new Variable();
      // cl needs SystemRoot env var set, otherwise D8037 is raised (bogus
      // message)
      // - https://msdn.microsoft.com/en-us/library/bb385201.aspx
      // -
      // http://stackoverflow.com/questions/10560779/cl-exe-when-launched-via-createprocess-does-not-seem-to-have-write-permissions
      envVariable.setKey("SystemRoot");
      envVariable.setValue(this.windowsHome.getAbsolutePath());
      task.addEnv(envVariable);
      // cl needs TMP otherwise D8050 is raised c1xx.dll
      envVariable = new Variable();
      envVariable.setKey("TMP");
      envVariable.setValue("C:\\Temp");
      task.addEnv(envVariable);
    }
  }

  public void configureLinker(final AbstractNarMojo mojo, final LinkerDef linker) throws MojoExecutionException {
    final String os = mojo.getOS();
    if (os.equals(OS.WINDOWS)) {
      final String arch = mojo.getArchitecture();

      linker.setToolPath(this.toolPathLinker);

      // Visual Studio
      if ("x86".equals(arch)) {
        linker.addLibraryDirectory(this.home, "VC/lib");
        linker.addLibraryDirectory(this.home, "VC/atlmfc/lib");
      } else {
        linker.addLibraryDirectory(this.home, "VC/lib/" + arch);
        linker.addLibraryDirectory(this.home, "VC/atlmfc/lib/" + arch);
      }
      // Windows SDK
      String sdkArch = arch;
      if ("amd64".equals(arch)) {
        sdkArch = "x64";
      }
      // 6 lib ?+ lib/x86 or lib/x64
      if (compareVersion(this.windowsSdkVersion, "8.0") < 0) { 
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

  public Variable getPathVariable() {
    if (this.paths.isEmpty()) {
      return null;
    }
    final Variable pathVariable = new Variable();
    pathVariable.setKey("PATH");
    pathVariable.setValue(StringUtils.join(this.paths.iterator(), File.pathSeparator));
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
      windowsHome = new File(System.getenv("SystemRoot"));
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
    // 32 bit build on 64 bit OS can be built with 32 bit tool, or 64 bit tool
    // in amd64_x86 - currently defaulting to prefer 64 bit tools - match os
    final boolean matchMojo = false;
    // TODO: toolset architecture
    // match os - os x86 mojo(x86 / x86_amd64); os x64 mojo(amd64_x86 / amd64);
    // 32bit - force 32 on 64bit mojo(x86 / x86_amd64)
    // match mojo - os x86 is as above; os x64 mojo (x86 / amd64)

    // Cross tools first if necessary, platform tools second, more generic tools later
    if (!osArchitecture.equals(mojoArchitecture) && !matchMojo) {
      if (!addPath(this.home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture)) {
        throw new MojoExecutionException("Unable to find compiler for architecture " + mojoArchitecture + ".\n"
            + new File(this.home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture));
      }
      toolPathLinker = new File(this.home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture).getAbsolutePath();
    }
    if (null == toolPathLinker) {
      if ("amd64".equals(mojoArchitecture))
        toolPathLinker = new File(this.home, "VC/bin/amd64").getAbsolutePath();
      else
        toolPathLinker = new File(this.home, "VC/bin").getAbsolutePath();
    }
    if ("amd64".equals(osArchitecture) && !matchMojo) {
      addPath(this.home, "VC/bin/amd64");
    } else {
      addPath(this.home, "VC/bin");
    }
    addPath(this.home, "VC/VCPackages");
    addPath(this.home, "Common7/Tools");
    addPath(this.home, "Common7/IDE");

    // 64 bit tools if present are preferred
    if (compareVersion(this.windowsSdkVersion, "7.1A") <= 0) {
      if ("amd64".equals(osArchitecture) && !matchMojo) {
        addPath(this.windowsSdkHome, "bin/x64");
      }
      addPath(this.windowsSdkHome, "bin");
    } else {
      String sdkArch = mojoArchitecture;
      if ("amd64".equals(osArchitecture) && !matchMojo) {
        addPath(this.windowsSdkHome, "bin/x64");
      }
      addPath(this.windowsSdkHome, "bin/x86");
    }
    if ("amd64".equals(mojoArchitecture)) {
      toolPathWindowsSDK = new File(this.windowsSdkHome, "bin/x64").getAbsolutePath();
    } else if (compareVersion(this.windowsSdkVersion, "7.1A") <= 0) {
      toolPathWindowsSDK = new File(this.windowsSdkHome, "bin").getAbsolutePath();
    } else {
      toolPathWindowsSDK = new File(this.windowsSdkHome, "bin/x86").getAbsolutePath();
    }

    // clearing the path, add back the windows system folders
    addPath(this.windowsHome, "System32");
    addPath(this.windowsHome, "");
    addPath(this.windowsHome, "System32/wbem");
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
        // TODO: else Registry might be more reliable but adds dependency to be
        // able to acccess - HKLM\SOFTWARE\Microsoft\Visual
        // Studio\Major.Minor:InstallDir
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
    if (mojo != this.mojo) {
      this.mojo = mojo;
      init();
    }
  }

  @Override
  public String toString() {
    return this.home + "\n" + this.windowsSdkHome;
  }

  public void setToolPath(CompilerDef compilerDef, String name) {
    if ("res".equals(name) || "mc".equals(name) || "idl".equals(name)) {
      compilerDef.setToolPath(this.toolPathWindowsSDK);
    } else {
      compilerDef.setToolPath(this.toolPathLinker);
    }
  }
}
