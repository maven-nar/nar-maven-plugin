/*
 * 
 * Copyright 2003-2004 The Ant-Contrib project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.antcontrib.cpptasks.gcc;
import java.io.File;
import java.util.Vector;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.compiler.CaptureStreamHandler;
import net.sf.antcontrib.cpptasks.compiler.LinkType;
import net.sf.antcontrib.cpptasks.compiler.Linker;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
/**
 * Adapter for the g++ variant of the GCC linker
 * 
 * @author Stephen M. Webb <stephen.webb@bregmasoft.com>
 */
public class GppLinker extends AbstractLdLinker {
    protected static final String[] discardFiles = new String[0];
    protected static final String[] objFiles = new String[]{".o", ".a", ".lib",
            ".dll", ".so", ".sl"};
    // FREEHEP refactored dllLinker into soLinker
    private static final GppLinker soLinker = new GppLinker("gcc", objFiles,
            discardFiles, "lib", ".so", false, new GppLinker("gcc", objFiles,
                    discardFiles, "lib", ".so", true, null));
    private final static String libPrefix = "libraries: =";
    protected static final String[] libtoolObjFiles = new String[]{".fo", ".a",
            ".lib", ".dll", ".so", ".sl"};
    private static String[] linkerOptions = new String[]{"-bundle", "-dylib",
            "-dynamic", "-dynamiclib", "-nostartfiles", "-nostdlib",
            "-prebind", "-s", "-static", "-shared", "-symbolic", "-Xlinker",
    // FREEHEP
    "-static-libgcc", "-shared-libgcc" };
    private static final GppLinker instance = new GppLinker("gcc", objFiles,
            discardFiles, "", "", false, null);
    private static final GppLinker machDllLinker = new GppLinker("gcc",
            objFiles, discardFiles, "lib", ".dylib", false, null);
    private static final GppLinker machPluginLinker = new GppLinker("gcc",
            objFiles, discardFiles, "lib", ".bundle", false, null);
    // FREEHEP
    private static final GppLinker machJNILinker = new GppLinker("gcc",
            objFiles, discardFiles, "lib", ".jnilib", false, null);
    // FREEHEP added dllLinker for windows
    private static final GppLinker dllLinker = new GppLinker("gcc", objFiles,
            discardFiles, "", ".dll", false, null);
    public static GppLinker getInstance() {
        return instance;
    }
    private File[] libDirs;
    private String runtimeLibrary;
    // FREEEHEP
    private String gccLibrary, gfortranLibrary, gfortranMainLibrary;

    protected GppLinker(String command, String[] extensions,
            String[] ignoredExtensions, String outputPrefix,
            String outputSuffix, boolean isLibtool, GppLinker libtoolLinker) {
        super(command, "-dumpversion", extensions, ignoredExtensions,
                outputPrefix, outputSuffix, isLibtool, libtoolLinker);
    }
    protected void addImpliedArgs(boolean debug, LinkType linkType, Vector args) {
        super.addImpliedArgs(debug, linkType, args);
        if (getIdentifier().indexOf("mingw") >= 0) {
            if (linkType.isSubsystemConsole()) {
                args.addElement("-mconsole");
            }
            if (linkType.isSubsystemGUI()) {
                args.addElement("-mwindows");
            }
        }
        // BEGINFREEHEP link or not with libstdc++
        // for MacOS X see:
        // http://developer.apple.com/documentation/DeveloperTools/Conceptual/CppRuntimeEnv/Articles/LibCPPDeployment.html
        gfortranLibrary = null;
        if (linkType.linkFortran()) {
            if (linkType.isStaticRuntime()) {
                String[] cmdin = new String[] { "gfortran",
                        "-print-file-name=libgfortran.a" };
                String[] cmdout = CaptureStreamHandler.run(cmdin);
                if ((cmdout.length > 0) && (cmdout[0].indexOf('/') >= 0)) {
                    gfortranLibrary = cmdout[0];
                }
            } else {
                gfortranLibrary = "-lgfortran";
            }
        }
        
        gfortranMainLibrary = null;
        if (linkType.linkFortran()) {
        	if (linkType.isExecutable() && linkType.linkFortranMain() && !isDarwin()) {
                if (linkType.isStaticRuntime()) {
                    String[] cmdin = new String[] { "gfortran",
                            "-print-file-name=libgfortranbegin.a" };
                    String[] cmdout = CaptureStreamHandler.run(cmdin);
                    if ((cmdout.length > 0) && (cmdout[0].indexOf('/') >= 0)) {
                    	gfortranMainLibrary = cmdout[0];
                    }
                } else {
                	gfortranMainLibrary = "-lgfortranbegin";
                }        		
        	}
        }

        runtimeLibrary = null;
        if (linkType.linkCPP()) {
            if (linkType.isStaticRuntime()) {
                if (isDarwin()) {
                    runtimeLibrary = "-lstdc++-static";
                } else {
                    String[] cmdin = new String[] { "g++",
                            "-print-file-name=libstdc++.a" };
                    String[] cmdout = CaptureStreamHandler.run(cmdin);
                    if ((cmdout.length > 0) && (cmdout[0].indexOf('/') >= 0)) {
                        runtimeLibrary = cmdout[0];
                    }
                }
            } else {
                runtimeLibrary = "-lstdc++";
            }
        }

        gccLibrary = null;
        if (linkType.isStaticRuntime()) {
            gccLibrary = "-static-libgcc";
        } else {
            if (linkType.linkCPP()) {
                // NOTE: added -fexceptions here for MacOS X
                gccLibrary = "-fexceptions";
            } else {
                gccLibrary = "-shared-libgcc";
            }
        }
        // ENDFREEHEP
    }
    public String[] addLibrarySets(CCTask task, LibrarySet[] libsets,
            Vector preargs, Vector midargs, Vector endargs) {
        String[] rs = super.addLibrarySets(task, libsets, preargs, midargs,
                endargs);
        // BEGINFREEHEP
        if (gfortranLibrary != null) {
            endargs.addElement(gfortranLibrary);
        }
        if (gfortranMainLibrary != null) {
            endargs.addElement(gfortranMainLibrary);
        }
        if (gccLibrary != null) {
            endargs.addElement(gccLibrary);
        }
        // ENDFREEHEP
        if (runtimeLibrary != null) {
            endargs.addElement(runtimeLibrary);
        }
        return rs;
    }
    /**
     * Allows drived linker to decorate linker option. Override by GppLinker to
     * prepend a "-Wl," to pass option to through gcc to linker.
     * 
     * @param buf
     *            buffer that may be used and abused in the decoration process,
     *            must not be null.
     * @param arg
     *            linker argument
     */
    public String decorateLinkerOption(StringBuffer buf, String arg) {
        String decoratedArg = arg;
        if (arg.length() > 1 && arg.charAt(0) == '-') {
            switch (arg.charAt(1)) {
                //
                //   passed automatically by GCC
                //
                case 'g' :
                case 'f' :
                case 'F' :
                /* Darwin */
                case 'm' :
                case 'O' :
                case 'W' :
                case 'l' :
                case 'L' :
                case 'u' :
                    break;
                default :
                    boolean known = false;
                    for (int i = 0; i < linkerOptions.length; i++) {
                        if (linkerOptions[i].equals(arg)) {
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
    public File[] getLibraryPath() {
        if (libDirs == null) {
            Vector dirs = new Vector();
            // Ask GCC where it will look for its libraries.
            String[] args = new String[]{"g++", "-print-search-dirs"};
            String[] cmdout = CaptureStreamHandler.run(args);
            for (int i = 0; i < cmdout.length; ++i) {
                int prefixIndex = cmdout[i].indexOf(libPrefix);
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
            String[] libpath = new String[dirs.size()];
            dirs.copyInto(libpath);
            int count = CUtil.checkDirectoryArray(libpath);
            // Build return array.
            libDirs = new File[count];
            int index = 0;
            for (int i = 0; i < libpath.length; ++i) {
                if (libpath[i] != null) {
                    libDirs[index++] = new File(libpath[i]);
                }
            }
        }
        return libDirs;
    }
    public Linker getLinker(LinkType type) {
        if (type.isStaticLibrary()) {
            return GccLibrarian.getInstance();
        }
        // BEGINFREEHEP
        if (type.isJNIModule()) {
            return isDarwin() ? machJNILinker : isWindows() ? dllLinker
                    : soLinker;
        }
        if (type.isPluginModule()) {
            return isDarwin() ? machPluginLinker : isWindows() ? dllLinker
                    : soLinker;
        }
        if (type.isSharedLibrary()) {
            return isDarwin() ? machDllLinker : isWindows() ? dllLinker
                    : soLinker;
        }
        // ENDFREEHEP
        return instance;
    }
}
