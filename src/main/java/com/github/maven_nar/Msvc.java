package com.github.maven_nar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.types.Environment.Variable;
import org.codehaus.plexus.util.StringUtils;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.LinkerDef;
import com.github.maven_nar.cpptasks.types.SystemIncludePath;
import com.google.common.collect.Sets;

public class Msvc {

  @Parameter
  private File home;

  private AbstractNarMojo mojo;

  private final Set<String> paths = new LinkedHashSet<>();

  /**
   * VisualStudio Linker version. Required. The values should be:
   * <ul>
   * <li>7.1 for VS 2003</li>
   * <li>8.0 for VS 2005</li>
   * <li>9.0 for VS 2008</li>
   * <li>10.0 for VS 2010</li>
   * <li>11.0 for VS 2012</li>
   * <li>12.00 for VS 2013</li>
   * <li>14.0 for VS 2015</li>
   * <li>15.0 for VS 2017</li>
   * </ul>
   */
  @Parameter(defaultValue = "")
  private String version;

  @Parameter
  private File windowsSdkHome;

  @Parameter
  private String windowsSdkVersion;

  @Parameter
  private String tempPath;

  private File windowsHome;
  private String toolPathWindowsSDK;
  private String toolPathLinker;
  private List<File> sdkIncludes = new ArrayList<>();
  private List<File> sdkLibs = new ArrayList<>();
  private Set<String> libsRequired = Sets.newHashSet("ucrt", "um", "shared", "winrt");
  @Parameter(defaultValue = "false")
  private boolean force_requested_arch;

  private boolean addIncludePath(final CCTask task, final File home, final String subDirectory)
      throws MojoExecutionException {
    if (home == null) {
      return false;
    }
    final File file = new File(home, subDirectory);
    if (file.exists())
      return addIncludePathToTask(task, file);

    return false;
  }

  private boolean addIncludePathToTask(final CCTask task, final File file)
       throws MojoExecutionException {
    try {
      final SystemIncludePath includePath = task.createSysIncludePath();
      final String fullPath = file.getCanonicalPath();
      includePath.setPath(fullPath);
      return true;
    }
    catch (final IOException e) {
      throw new MojoExecutionException("Unable to add system include: " + file.getAbsolutePath(), e);
    }
  }

  private boolean addPath(final File home, final String path) {
    if (home != null) {
      final File directory = new File(home, path);
      if (directory.exists()) {
        try {
          final String fullPath = directory.getCanonicalPath();
          paths.add(fullPath);
          return true;
        }
        catch (final IOException e) {
          throw new IllegalArgumentException("Unable to get path: " + directory, e);
        }
      }
    }
    return false;
  }

  static boolean isMSVC(final AbstractNarMojo mojo) {
    return isMSVC(mojo.getLinker().getName());
  }

  static boolean isMSVC(final String name) {
    return "msvc".equalsIgnoreCase(name);
  }

  public void configureCCTask(final CCTask task) throws MojoExecutionException {
    if (OS.WINDOWS.equals(mojo.getOS()) && isMSVC(mojo)) {
      addIncludePath(task, home, "VC/include");
      addIncludePath(task, home, "VC/atlmfc/include");
      if (compareVersion(windowsSdkVersion, "7.1A") <= 0) {
        if (version.equals("8.0")) {
          // For VS 2005 the version of SDK is 2.0, but it needs more paths
          for (File sdkInclude : sdkIncludes)      {
            addIncludePathToTask(task, sdkInclude);
            mojo.getLog().debug(" configureCCTask add to Path-- " + sdkInclude.getAbsolutePath());
          }
        }
        else {
          addIncludePath(task, windowsSdkHome, "include");
        }
      }
      else {
        for (File sdkInclude : sdkIncludes) {
            addIncludePathToTask(task, sdkInclude);
        }
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
      envVariable.setValue(windowsHome.getAbsolutePath());
      task.addEnv(envVariable);
      // cl needs TMP otherwise D8050 is raised c1xx.dll
      envVariable = new Variable();
      envVariable.setKey("TMP");
      envVariable.setValue(getTempPath());
      task.addEnv(envVariable);
      final String envInclude = System.getenv("INCLUDE");
      if (envInclude != null) {
        for (final String path : envInclude.split(";")) {
          addIncludePathToTask(task, new File(path));
        }
      }
    }
  }

  public void configureLinker(final LinkerDef linker) throws MojoExecutionException {
    final String os = mojo.getOS();

    if (os.equals(OS.WINDOWS) && isMSVC(mojo)) {
      final String arch = mojo.getArchitecture();

      // Visual Studio
      if ("x86".equals(arch)) {
        linker.addLibraryDirectory(home, "VC/lib");
        linker.addLibraryDirectory(home, "VC/atlmfc/lib");
      }
      else {
        linker.addLibraryDirectory(home, "VC/lib/" + arch);
        linker.addLibraryDirectory(home, "VC/atlmfc/lib/" + arch);
      }

      // Windows SDK
      String sdkArch = arch;
      if ("amd64".equals(arch)) {
        sdkArch = "x64";
      }
      // 6 lib ?+ lib/x86 or lib/x64
      if (compareVersion(windowsSdkVersion, "8.0") < 0) {
        if ("x86".equals(arch)) {
          linker.addLibraryDirectory(windowsSdkHome, "lib");
        }
        else {
          linker.addLibraryDirectory(windowsSdkHome, "lib/" + sdkArch);
        }
      }
      else {
        for (File sdkLib : sdkLibs) {
          linker.addLibraryDirectory(sdkLib, sdkArch);
        }
      }

      final String envLib = System.getenv("LIB");
      if (envLib != null) {
        for (final String path : envLib.split(";")) {
          linker.addLibraryDirectory(new File(path));
        }
      }
    }
  }

  private String getTempPath(){
    if (null == tempPath) {
      tempPath = System.getenv("TMP");
      if (tempPath == null) tempPath = System.getenv("TEMP");
      if (tempPath == null) tempPath = "C:\\Temp";
    }
    return tempPath;
  }

  public Variable getPathVariable() {
    if (paths.isEmpty()) return null;
    final Variable pathVariable = new Variable();
    pathVariable.setKey("PATH");
    pathVariable.setValue(StringUtils.join(paths.iterator(), File.pathSeparator));
    return pathVariable;
  }

  public String getVersion() {
    return version;
  }

  public String getWindowsSdkVersion() {
    return windowsSdkVersion;
  }

  private void init() throws MojoFailureException, MojoExecutionException {
    final String mojoOs = mojo.getOS();
    if (NarUtil.isWindows() && OS.WINDOWS.equals(mojoOs) && isMSVC(mojo)) {
      windowsHome = new File(System.getenv("SystemRoot"));

      initVisualStudio();
      if (version.equals("8.0")) {
        // VS 2005 works with build in Windows SDK
        initWindowsSdk8();
        initPath8();
      }
      else {
        initWindowsSdk();
        initPath();
      }
    }
    else {
      version = "";
      windowsSdkVersion = "";
      windowsHome = null;
    }
  }

  private void initPath() throws MojoExecutionException {
    final String mojoArchitecture = mojo.getArchitecture();
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
    if (force_requested_arch)
    {
      if ("amd64".equals(mojoArchitecture) && !matchMojo) {
        addPath(home, "VC/bin/amd64");
        toolPathLinker = new File(home, "VC/bin/amd64").getAbsolutePath();
      }
      else {
        addPath(home, "VC/bin");
        toolPathLinker = new File(home, "VC/bin").getAbsolutePath();
      }
    }
    else if (!osArchitecture.equals(mojoArchitecture) && !matchMojo) {
      if (!addPath(home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture)) {
        throw new MojoExecutionException("Unable to find compiler for architecture " + mojoArchitecture + ".\n"
            + new File(home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture));
      }
      toolPathLinker = new File(home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture).getAbsolutePath();
    }
    if (null == toolPathLinker) {
      if ("amd64".equals(mojoArchitecture)) {
        toolPathLinker = new File(home, "VC/bin/amd64").getAbsolutePath();
        if (!new File(toolPathLinker).exists()) {
          final String envVCToolsInstallDir = System.getenv("VCToolsInstallDir");
          if (envVCToolsInstallDir != null) {
            toolPathLinker = new File(envVCToolsInstallDir, "bin/HostX64/x64").getAbsolutePath();
          }
        }
      }
      else {
        toolPathLinker = new File(home, "VC/bin").getAbsolutePath();
      }
    }
    if ("amd64".equals(osArchitecture) && !matchMojo) {
      addPath(home, "VC/bin/amd64");
    }
    else {
      addPath(home, "VC/bin");
    }
    addPath(home, "VC/VCPackages");
    addPath(home, "Common7/Tools");
    addPath(home, "Common7/IDE");

    // 64 bit tools if present are preferred
    if (compareVersion(windowsSdkVersion, "7.1A") <= 0) {
      if ("amd64".equals(osArchitecture) && !matchMojo) {
        addPath(windowsSdkHome, "bin/x64");
      }
      addPath(windowsSdkHome, "bin");
    }
    else {
      if ("amd64".equals(osArchitecture) && !matchMojo) {
        addPath(windowsSdkHome, "bin/x64");
      }
      addPath(windowsSdkHome, "bin/x86");
    }
    if ("amd64".equals(mojoArchitecture)) {
      toolPathWindowsSDK = new File(windowsSdkHome, "bin/x64").getAbsolutePath();
    }
    else if (compareVersion(windowsSdkVersion, "7.1A") <= 0) {
      toolPathWindowsSDK = new File(windowsSdkHome, "bin").getAbsolutePath();
    }
    else {
      toolPathWindowsSDK = new File(windowsSdkHome, "bin/x86").getAbsolutePath();
    }

    // clearing the path, add back the windows system folders
    addPath(windowsHome, "System32");
    addPath(windowsHome, "");
    addPath(windowsHome, "System32/wbem");
  }

    private void initPath8() throws MojoExecutionException {
      final String mojoArchitecture = mojo.getArchitecture();
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

      if (force_requested_arch) {
        if ("amd64".equals(mojoArchitecture) && !matchMojo) {
          addPath(home, "VC/bin/amd64");
          toolPathLinker = new File(home, "VC/bin/amd64").getAbsolutePath();
        }
        else {
          addPath(home, "VC/bin");
          toolPathLinker = new File(home, "VC/bin").getAbsolutePath();
        }
      }
      else if (!osArchitecture.equals(mojoArchitecture) && !matchMojo) {
        if (!addPath(home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture)) {
          throw new MojoExecutionException("Unable to find compiler for architecture " + mojoArchitecture + ".\n" +
            new File(home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture));
        }
        toolPathLinker = new File(home, "VC/bin/" + osArchitecture + "_" + mojoArchitecture).getAbsolutePath();
      }
      if (null == toolPathLinker) {
        if ("amd64".equals(mojoArchitecture))
          toolPathLinker = new File(home, "VC/bin/amd64").getAbsolutePath();
        else
          toolPathLinker = new File(home, "VC/bin").getAbsolutePath();
      }
      if ("amd64".equals(osArchitecture) && !matchMojo) {
        addPath(home, "VC/bin/amd64");
      }
      else {
        addPath(home, "VC/bin");
      }
      // clearing the path, add back the windows system folders
      addPath(windowsHome, "System32");
      addPath(windowsHome, "");
      addPath(windowsHome, "System32/wbem");
    }

  private void initVisualStudio() throws MojoFailureException, MojoExecutionException {
    mojo.getLog().debug(" -- Searching for usable VisualStudio ");

    mojo.getLog().debug("Linker version is  " + version);

    if (version != null && version.trim().length() > 1) {
      String internalVersion;
      Pattern r = Pattern.compile("(\\d+)\\.*(\\d)");
      Matcher matcher = r.matcher(version);
      if (matcher.find()) {
        internalVersion = matcher.group(1) + matcher.group(2);
        version = matcher.group(1) + "." + matcher.group(2);
      }
      else {
        throw new MojoExecutionException("msvc.version must be the internal version in the form 10.0 or 120");
      }
      if (home == null) {
        final String commontToolsVar = System.getenv("VS" + internalVersion + "COMNTOOLS");
        if (commontToolsVar != null && commontToolsVar.trim().length() > 0) {
          final File commonToolsDirectory = new File(commontToolsVar);
          if (commonToolsDirectory.exists()) {
            home = commonToolsDirectory.getParentFile().getParentFile();
          }
        }
        // TODO: else Registry might be more reliable but adds dependency to be
        // able to acccess - HKLM\SOFTWARE\Microsoft\Visual Studio\Major.Minor:InstallDir
      }
      mojo.getLog()
          .debug(String.format(" VisualStudio %1s (%2s) found %3s ", version, internalVersion, home));
    }
    else {
      version = "";
      for (final Entry<String, String> entry : System.getenv().entrySet()) {
        final String key = entry.getKey();
        final String value = entry.getValue();
        final Pattern versionPattern = Pattern.compile("VS(\\d+)(\\d)COMNTOOLS");
        final Matcher matcher = versionPattern.matcher(key);
        if (matcher.matches()) {
          final String version = matcher.group(1) + "." + matcher.group(2);
          if (versionStringComparator.compare(version, version) > 0) {
            final File commonToolsDirectory = new File(value);
            if (commonToolsDirectory.exists()) {
              this.version = version;
              home = commonToolsDirectory.getParentFile().getParentFile();
              mojo.getLog().debug(
                String.format(" VisualStudio %1s (%2s) found %3s ", version,
                  matcher.group(1) + matcher.group(2), home));
            }
          }
        }
      }
      if (version.length() == 0) {
        final TextStream out = new StringTextStream();
        final TextStream err = new StringTextStream();
        final TextStream dbg = new StringTextStream();

        NarUtil.runCommand("link", new String[] {
          "/?"
        }, null, null, out, err, dbg, null, true);
        final Pattern p = Pattern.compile("(\\d+\\.\\d+)\\.\\d+(\\.\\d+)?");
        final Matcher m = p.matcher(out.toString());
        if (m.find()) {
          version = m.group(1);
          mojo.getLog().debug(
            String.format(" VisualStudio Not found but link runs and reports version %1s (%2s)", version,
              m.group(0)));
        }
        else {
          throw new MojoExecutionException(
              "msvc.version not specified and no VS<Version>COMNTOOLS environment variable can be found");
        }
      }
    }
  }

  private final Comparator<String> versionStringComparator = new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
      DefaultArtifactVersion version1 = new DefaultArtifactVersion(o1);
      DefaultArtifactVersion version2 = new DefaultArtifactVersion(o2);
      return version1.compareTo(version2);
    }
  };

  private final Comparator<File> versionComparator = new Comparator<File>() {
    @Override
    public int compare(File o1, File o2) {
      // will be sorted smallest first, so we need to invert the order of
      // the objects
      String firstDir = o2.getName(), secondDir = o1.getName();
      if (firstDir.charAt(0) == 'v') { // remove 'v' and 'A' at the end
        firstDir = firstDir.substring(1, firstDir.length() - 1);
        secondDir = secondDir.substring(1, secondDir.length() - 1);
      }
      // impossible that two dirs are the same
      String[] firstVersionString = firstDir.split("\\."), secondVersionString = secondDir.split("\\.");

      int maxIdx = Math.min(firstVersionString.length, secondVersionString.length);
      int deltaVer;
      try {
        for (int i = 0; i < maxIdx; i++)
          if ((deltaVer = Integer.parseInt(firstVersionString[i]) - Integer.parseInt(secondVersionString[i])) != 0)
            return deltaVer;

      }
      catch (NumberFormatException e) {
        return firstDir.compareTo(secondDir);
      }
      if (firstVersionString.length > maxIdx) // 10.0.150 > 10.0
        return 1;
      else if (secondVersionString.length > maxIdx) // 10.0 < 10.0.150
        return -1;
      return 0; // impossible that they are the same
    }
  };
  private boolean foundSDK = false;

  private void initWindowsSdk() throws MojoExecutionException {
    if (windowsSdkVersion != null && windowsSdkVersion.trim().equals(""))
      windowsSdkVersion = null;

    mojo.getLog().debug(" -- Searching for usable WindowSDK ");
    // newer first: 10 -> 8.1 -> 8.0 -> 7.1 and look for libs specified


    for (final File directory : Arrays.asList(
            new File("C:/Program Files (x86)/Windows Kits"),
            new File("C:/Program Files (x86)/Microsoft SDKs/Windows"),
            new File("C:/Program Files/Windows Kits"),
            new File("C:/Program Files/Microsoft SDKs/Windows") )) {
      if (directory.exists()) {
        final File[] kitDirectories = directory.listFiles();
        Arrays.sort(kitDirectories, versionComparator);
        if (kitDirectories != null) {
          for (final File kitDirectory : kitDirectories) {

            if (new File(kitDirectory, "Include").exists()) {
              // legacy SDK
              String kitVersion = kitDirectory.getName();
              if (kitVersion.charAt(0) == 'v') {
                kitVersion = kitVersion.substring(1);
              }
              if (windowsSdkVersion != null && compareVersion(kitVersion, windowsSdkVersion) != 0)
                continue; // skip versions not identical to exact version
              mojo.getLog()
                  .debug(String.format(" WindowSDK %1s found %2s", kitVersion, kitDirectory.getAbsolutePath()));
              if (kitVersion.matches("\\d+\\.\\d+?[A-Z]?")) {
                // windows <= 8.1
                legacySDK(kitDirectory);
              }
              else if (kitVersion.matches("\\d+?")) {
                // windows 10 SDK supports
                addNewSDKLibraries(kitDirectory);
              }
            }
          }
          if (libsRequired.size() == 0) // need it here to break out of the outer loop
              break;
        }
      }
    }
    if (!foundSDK)
    { // Search for SDK with lower versions
      for (final File directory : Arrays.asList(
              new File("C:/Program Files (x86)/Windows Kits"),
              new File("C:/Program Files (x86)/Microsoft SDKs/Windows"),
              new File("C:/Program Files/Windows Kits"),
              new File("C:/Program Files/Microsoft SDKs/Windows") )) {
        if (directory.exists()) {
          final File[] kitDirectories = directory.listFiles();
          Arrays.sort(kitDirectories, versionComparator);
          if (kitDirectories != null) {
            for (final File kitDirectory : kitDirectories) {

              if (new File(kitDirectory, "Include").exists()) {
                // legacy SDK
                String kitVersion = kitDirectory.getName();
                if (kitVersion.charAt(0) == 'v') {
                  kitVersion = kitVersion.substring(1);
                }
                if (windowsSdkVersion != null && compareVersion(kitVersion, windowsSdkVersion) > 0) {
                  continue; // skip versions higher than the previous version
                }
                mojo.getLog().debug(String.format(" WindowSDK %1s found %2s", kitVersion, kitDirectory.getAbsolutePath()));
                if (kitVersion.matches("\\d+\\.\\d+?[A-Z]?")) {
                  // windows <= 8.1
                  legacySDK(kitDirectory);
                }
                else if (kitVersion.matches("\\d+?")) {
                  // windows 10 SDK supports
                  addNewSDKLibraries(kitDirectory);
                }
              }
            }
            if (libsRequired.size() == 0) // need it here to break out of the outer loop
              break;
          }
        }
      }
    }

    if (!foundSDK)
      throw new MojoExecutionException("msvc.windowsSdkVersion not specified and versions cannot be found");
    mojo.getLog().debug(String.format(" Using WindowSDK %1s found %2s", windowsSdkVersion, windowsSdkHome));
  }

  private void addNewSDKLibraries(final File kitDirectory) {
    // multiple installs
    List<File> kitVersionDirectories = Arrays.asList(new File(kitDirectory, "Include").listFiles());
    Collections.sort(kitVersionDirectories,  versionComparator);
    ListIterator<File> kitVersionDirectoriesIt = kitVersionDirectories.listIterator();
    File kitVersionDirectory = null;
    while (kitVersionDirectoriesIt.hasNext() && (kitVersionDirectory = kitVersionDirectoriesIt.next()) != null) {
      if (new File(kitVersionDirectory, "ucrt").exists()) {
        break;
      }
    }

    if (kitVersionDirectory != null) {
      String version = kitVersionDirectory.getName();
      mojo.getLog().debug(String.format(" Latest Win %1s KitDir at %2s", kitVersionDirectory.getName(), kitVersionDirectory.getAbsolutePath()));
      // add the libraries found:
      File includeDir = new File(kitDirectory, "Include/" + version);
      File libDir = new File(kitDirectory, "Lib/" + version);
      addSDKLibs(includeDir, libDir);
      setKit(kitDirectory);
    }
  }

  private void setKit(File home) {
    if (!foundSDK) {
      if (windowsSdkVersion == null) windowsSdkVersion = home.getName();
      if (windowsSdkHome == null) windowsSdkHome = home;
      foundSDK = true;
    }
  }

  private void legacySDK(final File kitDirectory) {
    File includeDir = new File(kitDirectory, "Include");
    File libDir = new File(kitDirectory, "Lib");
    if (includeDir.exists() && libDir.exists()){
      File usableLibDir = null;
      for (final File libSubDir : libDir.listFiles()) {
        final File um = new File(libSubDir,"um");
        if (um.exists()) usableLibDir = libSubDir;
      }
      if (usableLibDir == null)
        usableLibDir = libDir.listFiles()[0];
      
      addSDKLibs(includeDir, usableLibDir);
      setKit(kitDirectory);
    }
  }

  private void addSDKLibs(File includeDir, File libdir) {
    final File[] libs = includeDir.listFiles();
    for (final File libIncludeDir : libs) {
      // <libName> <include path> <lib path>
      if (libsRequired.remove(libIncludeDir.getName())) {
        mojo.getLog().debug(String.format(" Using directory %1s for library %2s", libIncludeDir.getAbsolutePath(), libIncludeDir.getName()));
        sdkIncludes.add(libIncludeDir);
        sdkLibs.add(new File(libdir, libIncludeDir.getName()));
      }
    }
  }

  private void initWindowsSdk8() throws MojoExecutionException {
    final String osArchitecture = NarUtil.getArchitecture(null);
    //VS 2005 - The SDK files are included in the VS installation-
    File VCINSTALLDIR = new File (home,"VC");

    //File VSLibDir = new File(VCINSTALLDIR.getAbsolutePath()+File.separator+ "lib" , osArchitecture);

    File PlatformSDKIncludeDir = new File(VCINSTALLDIR.getAbsolutePath()+ File.separator+ "PlatformSDK", "include");
    File SDKIncludeDir = new File(VCINSTALLDIR.getAbsolutePath()+ File.separator+ "SDK"+ File.separator+ "v2.0", "include");

    sdkIncludes.add(PlatformSDKIncludeDir);
    sdkIncludes.add(SDKIncludeDir);
    windowsSdkHome = home;
  }

  public void setMojo(final AbstractNarMojo mojo) throws MojoFailureException, MojoExecutionException {
    if (this.mojo != mojo) {
      this.mojo = mojo;
      init();
    }

  }

  @Override
  public String toString() {
    return "VS Home-"+ home + "\nSDKHome-" + windowsSdkHome;
  }

  public String getToolPath() {
    return toolPathLinker;
  }

  public String getSDKToolPath() {
    return toolPathWindowsSDK;
  }

  public void setToolPath(CompilerDef compilerDef, String name) {
    if ("res".equals(name) || "mc".equals(name) || "idl".equals(name)) {
      compilerDef.setToolPath(toolPathWindowsSDK);
    }
    else {
      compilerDef.setToolPath(toolPathLinker);
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
