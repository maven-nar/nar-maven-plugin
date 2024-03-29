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
package com.github.maven_nar.cpptasks.gcc;

import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;

/**
 * Adapter for the GCC linker
 *
 * @author Adam Murdoch
 */
public class GccLinker extends AbstractLdLinker {
  private static final String[] discardFiles = new String[0];
  private static final String[] objFiles = new String[] {
      ".o", ".a", ".lib", ".dll", ".so", ".sl"
  };
  private static final String[] libtoolObjFiles = new String[] {
      ".fo", ".a", ".lib", ".dll", ".so", ".sl"
  };
  private static String[] linkerOptions = new String[] {
      "-bundle",
      // FREEHEP
      "-dynamic", "-arch", "-dynamiclib", "-nostartfiles", "-nostdlib", "-prebind", "-s", "-static", "-shared",
      "-symbolic", "-Xlinker", "--export-all-symbols", "-static-libgcc", "-p", "-pg", "-pthread",
      // Regex based
      "-specs=.*", "-std=.*", "--specs=.*", "--std=.*"
  };
  // FREEHEP refactored dllLinker to soLinker
  private static final GccLinker soLinker = new GccLinker("gcc", objFiles, discardFiles, "lib", ".so", false,
      new GccLinker("gcc", objFiles, discardFiles, "lib", ".so", true, null));
  private static final GccLinker instance = new GccLinker("gcc", objFiles, discardFiles, "", "", false, null);
  private static final GccLinker clangInstance = new GccLinker("clang", objFiles, discardFiles, "", "", false, null);
  private static final GccLinker machBundleLinker = new GccLinker("gcc", objFiles, discardFiles, "lib", ".bundle",
      false, null);
  private static final GccLinker machDllLinker = new GccLinker("gcc", objFiles, discardFiles, "lib", ".dylib", false,
      null);
  private static final GccLinker machJNILinker = new GccLinker("gcc", objFiles, discardFiles, "lib", ".jnilib", false,
      null);
  // FREEHEP added dllLinker for windows
  private static final GccLinker dllLinker = new GccLinker("gcc", objFiles, discardFiles, "", ".dll", false, null);

   //Support running on AIX
  private static final GccLinker aLinker = new GccLinker("gcc", objFiles, discardFiles, "lib", ".a", false,  null);
  
  public static GccLinker getCLangInstance() {
    return clangInstance;
  }

  public static GccLinker getInstance() {
    return instance;
  }

  private File[] libDirs;

  protected GccLinker(final String command, final String[] extensions, final String[] ignoredExtensions,
      final String outputPrefix, final String outputSuffix, final boolean isLibtool, final GccLinker libtoolLinker) {
    super(command, "-dumpversion", extensions, ignoredExtensions, outputPrefix, outputSuffix, isLibtool, libtoolLinker);
  }

  @Override
  protected void addImpliedArgs(final CCTask task, final boolean debug, final LinkType linkType,
      final Vector<String> args) {
    super.addImpliedArgs(task, debug, linkType, args);
    if (getIdentifier().contains("mingw")) {
      if (linkType.isSubsystemConsole()) {
        args.addElement("-mconsole");
      }
      if (linkType.isSubsystemGUI()) {
        args.addElement("-mwindows");
      }
    }
  }

  /**
   * Allows drived linker to decorate linker option. Override by GccLinker to
   * prepend a "-Wl," to pass option to through gcc to linker.
   * 
   * @param buf
   *          buffer that may be used and abused in the decoration process,
   *          must not be null.
   * @param arg
   *          linker argument
   */
  @Override
  public String decorateLinkerOption(final StringBuffer buf, final String arg) {
    String decoratedArg = arg;
    if (arg.length() > 1 && arg.charAt(0) == '-') {
      switch (arg.charAt(1)) {
      //
      // passed automatically by GCC
      //
        case 'g':
        case 'f':
        case 'F':
          /* Darwin */
        case 'm':
        case 'O':
        case 'W':
        case 'l':
        case 'L':
        case 'u':
        case 'v':
          break;
        default:
          boolean known = false;
          for (final String linkerOption : linkerOptions) {
            if (arg.matches(linkerOption)) {
              known = true;
              break;
            }
          }
          if (!known) {
            buf.setLength(0);
            buf.append("-Wl,");
            buf.append(arg);
            decoratedArg = buf.toString();
          }
          break;
      }
    }
    return decoratedArg;
  }

  /**
   * Returns library path.
   * 
   */
  @Override
  public File[] getLibraryPath() {
    if (this.libDirs == null) {
      //
      // construct gcc lib path from machine and version
      //
      final StringBuffer buf = new StringBuffer("/lib/gcc-lib/");
      buf.append(GccProcessor.getMachine());
      buf.append('/');
      buf.append(GccProcessor.getVersion());
      //
      // build default path from gcc and system /lib and /lib/w32api
      //
      final String[] impliedLibPath = new String[] {
          buf.toString(), "/lib/w32api", "/lib"
      };
      //
      // read gcc specs file for other library paths
      //
      final String[] specs = GccProcessor.getSpecs();
      final String[][] libpaths = GccProcessor.parseSpecs(specs, "*link:", new String[] {
        "%q"
      });
      String[] libpath;
      if (libpaths[0].length > 0) {
        libpath = new String[libpaths[0].length + 3];
        int i = 0;
        for (; i < libpaths[0].length; i++) {
          libpath[i] = libpaths[0][i];
        }
        libpath[i++] = buf.toString();
        libpath[i++] = "/lib/w32api";
        libpath[i++] = "/lib";
      } else {
        //
        // if a failure to find any matches then
        // use some default values for lib path entries
        libpath = new String[] {
            "/usr/local/lib/mingw", "/usr/local/lib", "/usr/lib/w32api", "/usr/lib/mingw", "/usr/lib", buf.toString(),
            "/lib/w32api", "/lib"
        };
      }
      for (int i = 0; i < libpath.length; i++) {
        if (libpath[i].contains("mingw")) {
          libpath[i] = null;
        }
      }
      //
      // if cygwin then
      // we have to prepend location of gcc32
      // and .. to start of absolute filenames to
      // have something that will exist in the
      // windows filesystem
      if (GccProcessor.isCygwin()) {
        GccProcessor.convertCygwinFilenames(libpath);
      }
      //
      // check that remaining entries are actual directories
      //
      final int count = CUtil.checkDirectoryArray(libpath);
      //
      // populate return array with remaining entries
      //
      this.libDirs = new File[count];
      int index = 0;
      for (final String element : libpath) {
        if (element != null) {
          this.libDirs[index++] = new File(element);
        }
      }
    }
    return this.libDirs;
  }

  @Override
  public Linker getLinker(final LinkType type) {
    if (type.isStaticLibrary()) {
      return GccLibrarian.getInstance();
    }
    // BEGINFREEHEP
    if (type.isJNIModule()) {
      return isDarwin() ? machJNILinker : isWindows() ? dllLinker : soLinker;
    }
    if (type.isPluginModule()) {
      return isDarwin() ? machBundleLinker : isWindows() ? dllLinker : soLinker;
    }
    if (type.isSharedLibrary()) {
      return isDarwin() ? machDllLinker : isWindows() ? dllLinker : isAIX() ?  aLinker : soLinker;
    }
    if (getCommand().startsWith("clang")) {
      return clangInstance;
    }
    // ENDFREEHEP
    return instance;
  }
}
