package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Comparator;

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

  @Parameter
  private String tempPath;

  private File windowsHome;
  private String toolPathWindowsSDK;
  private String versionPathWindowsSDK = "";
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

  public void configureCCTask(final CCTask task) throws MojoExecutionException {
    if (OS.WINDOWS.equals(mojo.getOS())
          && "msvc".equalsIgnoreCase(mojo.getLinker().getName())) {
      addIncludePath(task, this.home, "VC/include");
      addIncludePath(task, this.home, "VC/atlmfc/include");
      if (compareVersion(this.windowsSdkVersion, "7.1A") <= 0) {
        addIncludePath(task, this.windowsSdkHome, "include");
      } else {
        addIncludePath(task, this.windowsSdkHome, "include" + versionPathWindowsSDK + "/ucrt");
        addIncludePath(task, this.windowsSdkHome, "include" + versionPathWindowsSDK + "/um");
        addIncludePath(task, this.windowsSdkHome, "include" + versionPathWindowsSDK + "/shared");
        addIncludePath(task, this.windowsSdkHome, "include" + versionPathWindowsSDK + "/winrt");
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
      envVariable.setValue(getTempPath());
      task.addEnv(envVariable);
    }
  }

  public void configureLinker(final LinkerDef linker) throws MojoExecutionException {
    final String os = mojo.getOS();
    if (os.equals(OS.WINDOWS) && "msvc".equalsIgnoreCase(mojo.getLinker().getName())) {
      final String arch = mojo.getArchitecture();

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
        linker.addLibraryDirectory(this.windowsSdkHome, "Lib/Win8/um/" + sdkArch); // 8+
      } else {
        linker.addLibraryDirectory(this.windowsSdkHome, "Lib/Winv6.3/um/" + sdkArch); // win
                                                                                      // 8
                                                                                      // sdk
                                                                                      // compatible
                                                                                      // with
                                                                                      // XP
      }
      linker.addLibraryDirectory(this.windowsSdkHome, "Lib" + versionPathWindowsSDK + "/ucrt/" + sdkArch); // 10+
      linker.addLibraryDirectory(this.windowsSdkHome, "Lib" + versionPathWindowsSDK + "/um/" + sdkArch); // 10+
    }
  }

  private String getTempPath(){
    if( null == tempPath ){
      tempPath = System.getenv("TMP");
      if( null == tempPath )
        tempPath = System.getenv("TEMP");
      if( null == tempPath )
        tempPath = "C:\\Temp";
    }
    return tempPath;
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

    // Cross tools first if necessary, platform tools second, more generic tools
    // later
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
    mojo.getLog().debug(" -- Searching for usable VisualStudio ");
    if (this.version != null && this.version.trim().length() > 1) {
      String internalVersion;
      Pattern r = Pattern.compile("(\\d+)\\.*(\\d)");
      Matcher matcher = r.matcher(this.version);
      if (matcher.find()) {
        internalVersion = matcher.group(1) + matcher.group(2);
        this.version = matcher.group(1) + "." + matcher.group(2);
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
      mojo.getLog()
          .debug(String.format(" VisualStudio %1s (%2s) found %3s ", this.version, internalVersion, this.home));
    } else {
      this.version = "";
      File home = null;
      for (final Entry<String, String> entry : System.getenv().entrySet()) {
        final String key = entry.getKey();
        final String value = entry.getValue();
        final Pattern versionPattern = Pattern.compile("VS(\\d+)(\\d)COMNTOOLS");
        final Matcher matcher = versionPattern.matcher(key);
        if (matcher.matches()) {
          final String version = matcher.group(1) + "." + matcher.group(2);
          if (version.compareTo(this.version) > 0) {
            final File commonToolsDirectory = new File(value);
            if (commonToolsDirectory.exists()) {
              this.version = version;
              this.home = commonToolsDirectory.getParentFile().getParentFile();
              mojo.getLog().debug(
                  String.format(" VisualStudio %1s (%2s) found %3s ", this.version,
                      matcher.group(1) + matcher.group(2), this.home));
            }
          }
        }
      }
      if (this.version.length() == 0) {
        final TextStream out = new StringTextStream();
        final TextStream err = new StringTextStream();
        final TextStream dbg = new StringTextStream();

        NarUtil.runCommand("link", new String[] {
          "/?"
        }, null, null, out, err, dbg, null, true);
        final Pattern p = Pattern.compile("(\\d+\\.\\d+)\\.\\d+(\\.\\d+)?");
        final Matcher m = p.matcher(out.toString());
        if (m.find()) {
          this.version = m.group(1);
          mojo.getLog().debug(
              String.format(" VisualStudio Not found but link runs and reports version %1s (%2s)", this.version,
                  m.group(0)));
        } else {
          throw new MojoExecutionException(
              "msvc.version not specified and no VS<Version>COMNTOOLS environment variable can be found");
        }
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
    mojo.getLog().debug(" -- Searching for usable WindowSDK ");
    for (final File directory : Arrays.asList(
            new File("C:/Program Files (x86)/Microsoft SDKs/Windows"),
            new File("C:/Program Files (x86)/Windows Kits"),
            new File("C:/Program Files/Microsoft SDKs/Windows"),
            new File("C:/Program Files/Windows Kits") )) {
      if (directory.exists()) {
        final File[] kitDirectories = directory.listFiles();
        if (kitDirectories != null) {
          for (final File kitDirectory : kitDirectories) {
            if (new File(kitDirectory, "Include").exists()) {
              String kitVersion = kitDirectory.getName();
              if (kitVersion.charAt(0) == 'v') {
                kitVersion = kitVersion.substring(1);
              }
              mojo.getLog()
                  .debug(String.format(" WindowSDK %1s found %2s", kitVersion, kitDirectory.getAbsolutePath()));
              if (kitVersion.matches("\\d+\\.\\d+?[A-Z]?")) { // windows 8/8.1
                                                              // SDK
                if (this.windowsSdkVersion.length() == 0) {
                  if (compareVersion(kitVersion, maxVersion) > 0) {
                    maxVersion = kitVersion;
                    home = kitDirectory;
                  }
                } else {
                  // TODO: is it acceptable to be sloppy - msvc installs 'A'
                  // version, windows platform sdk installs more complete
                  // if (this.windowsSdkVersion.startsWith(kitVersion)) {
                  if (kitVersion.equals(this.windowsSdkVersion)) {
                    maxVersion = this.windowsSdkVersion;
                    home = kitDirectory;
                  }
                }
              }
              if (kitVersion.matches("\\d+?")) { // windows 10 SDK supports
                                                 // multiple installs
                home = kitDirectory;
                final File[] kitVersionDirectories = new File(kitDirectory, "Include").listFiles();
                for (final File kitVersionDirectory : kitVersionDirectories) {
                  mojo.getLog().debug(String.format(" KitDir %1s", kitVersionDirectory.getAbsolutePath()));
                  if (new File(kitVersionDirectory, "um").exists()) {
                    String kitSpecificVersion = kitVersionDirectory.getName();
                    if (this.windowsSdkVersion.length() == 0 || kitVersion.equals(this.windowsSdkVersion)) { // unspecified
                                                                                                             // or
                                                                                                             // generic
                                                                                                             // 10
                      // find the max version available.
                      mojo.getLog().debug(String.format(" KitVer %1s, maxVer %2s", kitSpecificVersion, maxVersion));
                      if (compareVersion(kitSpecificVersion, maxVersion) > 0) {
                        maxVersion = kitSpecificVersion;
                        versionPathWindowsSDK = "/" + maxVersion;
                      }
                    } else {
                      // find the specific version
                      if (kitVersion.equals(this.windowsSdkVersion)) {
                        maxVersion = kitSpecificVersion;
                        versionPathWindowsSDK = "/" + maxVersion;
                      }
                    }
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
      throw new MojoExecutionException("msvc.windowsSdkVersion not specified and versions cannot be found");
    }
    this.windowsSdkVersion = maxVersion;
    mojo.getLog().debug(String.format(" Using WindowSDK %1s found %2s", this.windowsSdkVersion, this.windowsSdkHome));
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

  public String getToolPath() {
    return this.toolPathLinker;
  }

  public String getSDKToolPath() {
    return this.toolPathWindowsSDK;
  }

  public void setToolPath(CompilerDef compilerDef, String name) {
    if ("res".equals(name) || "mc".equals(name) || "idl".equals(name)) {
      compilerDef.setToolPath(this.toolPathWindowsSDK);
    } else {
      compilerDef.setToolPath(this.toolPathLinker);
    }
  }

  public int compareVersion(Object o1, Object o2) {
    String version1 = (String) o1;
    String version2 = (String) o2;

    VersionTokenizer tokenizer1 = new VersionTokenizer(version1);
    VersionTokenizer tokenizer2 = new VersionTokenizer(version2);

    int number1 = 0, number2 = 0;
    String suffix1 = "", suffix2 = "";

    while (tokenizer1.MoveNext()) {
      if (!tokenizer2.MoveNext()) {
        do {
          number1 = tokenizer1.getNumber();
          suffix1 = tokenizer1.getSuffix();
          if (number1 != 0 || suffix1.length() != 0) {
            // Version one is longer than number two, and non-zero
            return 1;
          }
        } while (tokenizer1.MoveNext());

        // Version one is longer than version two, but zero
        return 0;
      }

      number1 = tokenizer1.getNumber();
      suffix1 = tokenizer1.getSuffix();
      number2 = tokenizer2.getNumber();
      suffix2 = tokenizer2.getSuffix();

      if (number1 < number2) {
        // Number one is less than number two
        return -1;
      }
      if (number1 > number2) {
        // Number one is greater than number two
        return 1;
      }

      boolean empty1 = suffix1.length() == 0;
      boolean empty2 = suffix2.length() == 0;

      if (empty1 && empty2)
        continue; // No suffixes
      if (empty1)
        return 1; // First suffix is empty (1.2 > 1.2b)
      if (empty2)
        return -1; // Second suffix is empty (1.2a < 1.2)

      // Lexical comparison of suffixes
      int result = suffix1.compareTo(suffix2);
      if (result != 0)
        return result;

    }
    if (tokenizer2.MoveNext()) {
      do {
        number2 = tokenizer2.getNumber();
        suffix2 = tokenizer2.getSuffix();
        if (number2 != 0 || suffix2.length() != 0) {
          // Version one is longer than version two, and non-zero
          return -1;
        }
      } while (tokenizer2.MoveNext());

      // Version two is longer than version one, but zero
      return 0;
    }
    return 0;
  }

  // VersionTokenizer.java
  class VersionTokenizer {
    private final String _versionString;
    private final int _length;

    private int _position;
    private int _number;
    private String _suffix;
    private boolean _hasValue;

    public int getNumber() {
      return _number;
    }

    public String getSuffix() {
      return _suffix;
    }

    public boolean hasValue() {
      return _hasValue;
    }

    public VersionTokenizer(String versionString) {
      if (versionString == null)
        throw new IllegalArgumentException("versionString is null");

      _versionString = versionString;
      _length = versionString.length();
    }

    public boolean MoveNext() {
      _number = 0;
      _suffix = "";
      _hasValue = false;

      // No more characters
      if (_position >= _length)
        return false;

      _hasValue = true;

      while (_position < _length) {
        char c = _versionString.charAt(_position);
        if (c < '0' || c > '9')
          break;
        _number = _number * 10 + (c - '0');
        _position++;
      }

      int suffixStart = _position;

      while (_position < _length) {
        char c = _versionString.charAt(_position);
        if (c == '.')
          break;
        _position++;
      }

      _suffix = _versionString.substring(suffixStart, _position);

      if (_position < _length)
        _position++;

      return true;
    }
  }
}
