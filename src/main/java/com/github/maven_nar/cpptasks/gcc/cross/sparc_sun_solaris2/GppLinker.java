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
package com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2;

import java.io.File;
import java.util.Vector;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.compiler.CaptureStreamHandler;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.gcc.AbstractLdLinker;
import com.github.maven_nar.cpptasks.types.LibrarySet;

/**
 * Adapter for the g++ variant of the GCC linker
 *
 * @author Stephen M. Webb <stephen.webb@bregmasoft.com>
 */
public class GppLinker extends AbstractLdLinker {
  protected static final String[] discardFiles = new String[0];
  protected static final String[] objFiles = new String[] {
      ".o", ".a", ".lib", ".dll", ".so", ".sl"
  };
  private final static String libPrefix = "libraries: =";
  protected static final String[] libtoolObjFiles = new String[] {
      ".fo", ".a", ".lib", ".dll", ".so", ".sl"
  };
  private static String[] linkerOptions = new String[] {
      "-bundle", "-dylib", "-dynamic", "-dynamiclib", "-nostartfiles", "-nostdlib", "-prebind", "-s", "-static",
      "-shared", "-symbolic", "-Xlinker"
  };
  private static final GppLinker dllLinker = new GppLinker(GccCCompiler.CMD_PREFIX + "gcc", objFiles, discardFiles,
      "lib", ".so", false, new GppLinker(GccCCompiler.CMD_PREFIX + "gcc", objFiles, discardFiles, "lib", ".so", true,
          null));
  private static final GppLinker instance = new GppLinker(GccCCompiler.CMD_PREFIX + "gcc", objFiles, discardFiles, "",
      "", false, null);
  private static final GppLinker machDllLinker = new GppLinker(GccCCompiler.CMD_PREFIX + "gcc", objFiles, discardFiles,
      "lib", ".dylib", false, null);
  private static final GppLinker machPluginLinker = new GppLinker(GccCCompiler.CMD_PREFIX + "gcc", objFiles,
      discardFiles, "lib", ".bundle", false, null);

  public static GppLinker getInstance() {
    return instance;
  }

  private File[] libDirs;
  private String runtimeLibrary;

  protected GppLinker(final String command, final String[] extensions, final String[] ignoredExtensions,
      final String outputPrefix, final String outputSuffix, final boolean isLibtool, final GppLinker libtoolLinker) {
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
    if (linkType.isStaticRuntime()) {
      final String[] cmdin = new String[] {
          GccCCompiler.CMD_PREFIX + "g++", "-print-file-name=libstdc++.a"
      };
      final String[] cmdout = CaptureStreamHandler.run(cmdin);
      if (cmdout.length > 0) {
        this.runtimeLibrary = cmdout[0];
      } else {
        this.runtimeLibrary = null;
      }
    } else {
      this.runtimeLibrary = "-lstdc++";
    }
  }

  @Override
  public String[] addLibrarySets(final CCTask task, final LibrarySet[] libsets, final Vector<String> preargs,
      final Vector<String> midargs, final Vector<String> endargs) {
    final String[] rs = super.addLibrarySets(task, libsets, preargs, midargs, endargs);
    if (this.runtimeLibrary != null) {
      endargs.addElement(this.runtimeLibrary);
    }
    return rs;
  }

  /**
   * Allows drived linker to decorate linker option. Override by GppLinker to
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
          break;
        default:
          boolean known = false;
          for (final String linkerOption : linkerOptions) {
            if (linkerOption.equals(arg)) {
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
      final Vector<String> dirs = new Vector<>();
      // Ask GCC where it will look for its libraries.
      final String[] args = new String[] {
          GccCCompiler.CMD_PREFIX + "g++", "-print-search-dirs"
      };
      final String[] cmdout = CaptureStreamHandler.run(args);
      for (int i = 0; i < cmdout.length; ++i) {
        final int prefixIndex = cmdout[i].indexOf(libPrefix);
        if (prefixIndex >= 0) {
          // Special case DOS-type GCCs like MinGW or Cygwin
          int s = prefixIndex + libPrefix.length();
          int t = cmdout[i].indexOf(';', s);
          while (t > 0) {
            dirs.addElement(cmdout[i].substring(s, t));
            s = t + 1;
            t = cmdout[i].indexOf(';', s);
          }
          dirs.addElement(cmdout[i].substring(s));
          ++i;
          for (; i < cmdout.length; ++i) {
            dirs.addElement(cmdout[i]);
          }
        }
      }
      // Eliminate all but actual directories.
      final String[] libpath = new String[dirs.size()];
      dirs.copyInto(libpath);
      final int count = CUtil.checkDirectoryArray(libpath);
      // Build return array.
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
    if (type.isPluginModule()) {
      if (GccProcessor.getMachine().contains("darwin")) {
        return machPluginLinker;
      } else {
        return dllLinker;
      }
    }
    if (type.isSharedLibrary()) {
      if (GccProcessor.getMachine().contains("darwin")) {
        return machDllLinker;
      } else {
        return dllLinker;
      }
    }
    return instance;
  }
}
