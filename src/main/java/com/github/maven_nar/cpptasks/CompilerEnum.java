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
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks;

import org.apache.tools.ant.types.EnumeratedAttribute;

import com.github.maven_nar.cpptasks.arm.ADSCCompiler;
import com.github.maven_nar.cpptasks.borland.BorlandCCompiler;
import com.github.maven_nar.cpptasks.borland.BorlandResourceCompiler;
import com.github.maven_nar.cpptasks.compaq.CompaqVisualFortranCompiler;
import com.github.maven_nar.cpptasks.compiler.Compiler;
import com.github.maven_nar.cpptasks.msvc.Msvc2005CCompiler;
import com.github.maven_nar.cpptasks.msvc.MsvcCCompiler;
import com.github.maven_nar.cpptasks.msvc.MsvcMIDLCompiler;
import com.github.maven_nar.cpptasks.msvc.MsvcResourceCompiler;
import com.github.maven_nar.cpptasks.msvc.MsvcMessageCompiler;
import com.github.maven_nar.cpptasks.gcc.GccCCompiler;
import com.github.maven_nar.cpptasks.gcc.WindresResourceCompiler;
import com.github.maven_nar.cpptasks.hp.aCCCompiler;
import com.github.maven_nar.cpptasks.ibm.VisualAgeCCompiler;
import com.github.maven_nar.cpptasks.intel.IntelLinux32CCompiler;
import com.github.maven_nar.cpptasks.intel.IntelLinux32Compiler;
import com.github.maven_nar.cpptasks.intel.IntelLinux64CCompiler;
import com.github.maven_nar.cpptasks.intel.IntelLinux64Compiler;
import com.github.maven_nar.cpptasks.intel.IntelLinuxFortranCompiler;
import com.github.maven_nar.cpptasks.intel.IntelWin32CCompiler;
import com.github.maven_nar.cpptasks.intel.IntelWin64CCompiler;
import com.github.maven_nar.cpptasks.mozilla.XpidlCompiler;
import com.github.maven_nar.cpptasks.openwatcom.OpenWatcomCCompiler;
import com.github.maven_nar.cpptasks.openwatcom.OpenWatcomFortranCompiler;
import com.github.maven_nar.cpptasks.os390.OS390CCompiler;
import com.github.maven_nar.cpptasks.os400.IccCompiler;
import com.github.maven_nar.cpptasks.sun.C89CCompiler;
import com.github.maven_nar.cpptasks.sun.ForteCCCompiler;
import com.github.maven_nar.cpptasks.sun.ForteCCompiler;
import com.github.maven_nar.cpptasks.sun.ForteF77Compiler;
import com.github.maven_nar.cpptasks.ti.ClxxCCompiler;
import com.github.maven_nar.cpptasks.trolltech.MetaObjectCompiler;
import com.github.maven_nar.cpptasks.trolltech.UserInterfaceCompiler;
/**
 * Enumeration of supported compilers
 * 
 * <table width="100%" border="1"> <thead>Supported compilers </thead>
 * <tr>
 * <td>gcc (default)</td>
 * <td>GCC C++ compiler</td>
 * </tr>
 * <tr>
 * <td>g++</td>
 * <td>GCC C++ compiler</td>
 * </tr>
 * <tr>
 * <td>c++</td>
 * <td>GCC C++ compiler</td>
 * </tr>
 * <tr>
 * <td>clang</td>
 * <td>clang / llvm C compiler</td>
 * </tr>
 * <tr>
 * <td>clang++</td>
 * <td>clang++ / llvm C++ compiler</td>
 * </tr>
 * <tr>
 * <tr>
 * <td>g77</td>
 * <td>GNU FORTRAN compiler</td>
 * </tr>
 * <tr>
 * <td>msvc</td>
 * <td>Microsoft Visual C++</td>
 * </tr>
 * <tr>
 * <td>msvc8</td>
 * <td>Microsoft Visual C++ 8</td>
 * </tr>
 * <tr>
 * <td>bcc</td>
 * <td>Borland C++ Compiler</td>
 * </tr>
 * <tr>
 * <td>msrc</td>
 * <td>Microsoft Resource Compiler</td>
 * </tr>
 * <tr>
 * <td>brc</td>
 * <td>Borland Resource Compiler</td>
 * </tr>
 * <tr>
 * <td>df</td>
 * <td>Compaq Visual Fortran Compiler</td>
 * </tr>
 * <tr>
 * <td>midl</td>
 * <td>Microsoft MIDL Compiler</td>
 * </tr>
 * <tr>
 * <td>icl</td>
 * <td>Intel C++ compiler for Windows (IA-32)</td>
 * </tr>
 * <tr>
 * <td>ecl</td>
 * <td>Intel C++ compiler for Windows (IA-64)</td>
 * </tr>
 * <tr>
 * <td>icc</td>
 * <td>Intel C++ compiler for Linux (IA-32)</td>
 * </tr>
 * <tr>
 * <td>ifort</td>
 * <td>Intel Fortran compiler for Linux (IA-32)</td>
 * </tr>
 * <tr>
 * <td>ecc</td>
 * <td>Intel C++ compiler for Linux (IA-64)</td>
 * </tr>
 * <tr>
 * <td>CC</td>
 * <td>Sun ONE C++ compiler</td>
 * </tr>
 * <tr>
 * <td>aCC</td>
 * <td>HP aC++ C++ Compiler</td>
 * </tr>
 * <tr>
 * <td>os390</td>
 * <td>OS390 C Compiler</td>
 * </tr>
 * <tr>
 * <td>os400</td>
 * <td>Icc Compiler</td>
 * </tr>
 * <tr>
 * <td>sunc89</td>
 * <td>Sun C89 C Compiler</td>
 * </tr>
 * <tr>
 * <td>xlC</td>
 * <td>VisualAge C Compiler</td>
 * </tr>
 * <tr>
 * <td>cl6x</td>
 * <td>TI TMS320C6000 Optimizing Compiler</td>
 * </tr>
 * <tr>
 * <td>cl55</td>
 * <td>TI TMS320C55x Optimizing C/C++ Compiler</td>
 * </tr>
 * <tr>
 * <td>armcpp</td>
 * <td>ARM 32-bit C++ compiler</td>
 * </tr>
 * <tr>
 * <td>armcc</td>
 * <td>ARM 32-bit C compiler</td>
 * </tr>
 * <tr>
 * <td>tcpp</td>
 * <td>ARM 16-bit C++ compiler</td>
 * </tr>
 * <tr>
 * <td>tcc</td>
 * <td>ARM 16-bit C compiler</td>
 * </tr>
 *      * <tr>
 * <td>uic</td>
 * <td>Qt user interface compiler</td>
 * </tr>
 * <tr>
 * <td>moc</td>
 * <td>Qt meta-object compiler</td>
 * </tr>
 * <tr>
 * <td>xpidl</td>
 * <td>Mozilla xpidl compiler (creates .h and .xpt files).</td>
 * </tr>
 * <tr>
 * <td>wcl</td>
 * <td>OpenWatcom C/C++ compiler (experimental)</td>
 * </tr>
 * <tr>
 * <td>wfl</td>
 * <td>OpenWatcom FORTRAN compiler (experimental)</td>
 * </tr>
 * <tr>
 * <td>windres</td>
 * <td>GNU windres resource compiler</td>
 * </tr>
 * </table>
 * 
 * @author Curt Arnold
 *  
 */
public class CompilerEnum extends EnumeratedAttribute {
    private final static ProcessorEnumValue[] compilers = new ProcessorEnumValue[]{
            new ProcessorEnumValue("gcc", GccCCompiler.getInstance()),
            new ProcessorEnumValue("g++", GccCCompiler.getGppInstance()),
            new ProcessorEnumValue("clang", GccCCompiler.getCLangInstance()),
            new ProcessorEnumValue("clang++", GccCCompiler.getCLangppInstance()),
            new ProcessorEnumValue("c++", GccCCompiler.getCppInstance()),
            new ProcessorEnumValue("g77", GccCCompiler.getG77Instance()),
// FREEHEP
            new ProcessorEnumValue("gfortran", GccCCompiler.getGFortranInstance()),
            new ProcessorEnumValue("msvc", MsvcCCompiler.getInstance()),
            new ProcessorEnumValue("msvc8", Msvc2005CCompiler.getInstance()),
            new ProcessorEnumValue("bcc", BorlandCCompiler.getInstance()),
            new ProcessorEnumValue("msrc", MsvcResourceCompiler
                    .getInstance()),
            new ProcessorEnumValue("msmc", MsvcMessageCompiler.getInstance()),
            new ProcessorEnumValue("brc", BorlandResourceCompiler.getInstance()),
            new ProcessorEnumValue("df", CompaqVisualFortranCompiler
                    .getInstance()),
            new ProcessorEnumValue("midl", MsvcMIDLCompiler.getInstance()),
            new ProcessorEnumValue("icl", IntelWin32CCompiler.getInstance()),
            new ProcessorEnumValue("ecl", IntelWin64CCompiler.getInstance()),
// BEGINFREEHEP
            new ProcessorEnumValue("icc", IntelLinux32CCompiler.getInstance()),
            new ProcessorEnumValue("ecc", IntelLinux64CCompiler.getInstance()),
            new ProcessorEnumValue("icpc", IntelLinux32Compiler.getInstance()),
            new ProcessorEnumValue("ecpc", IntelLinux64Compiler.getInstance()),
            new ProcessorEnumValue("ifort", IntelLinuxFortranCompiler.getInstance()),
// ENDFREEHEP
            new ProcessorEnumValue("CC", ForteCCCompiler.getInstance()),
// BEGINFREEHEP
            new ProcessorEnumValue("suncc", ForteCCompiler.getInstance()),
            new ProcessorEnumValue("sunf77", ForteF77Compiler.getInstance()),
// ENDFREEHEP
            new ProcessorEnumValue("aCC", aCCCompiler.getInstance()),
            new ProcessorEnumValue("os390", OS390CCompiler.getInstance()),
            new ProcessorEnumValue("os400", IccCompiler.getInstance()),
            new ProcessorEnumValue("sunc89", C89CCompiler.getInstance()),
            new ProcessorEnumValue("xlC", VisualAgeCCompiler.getInstance()),
            new ProcessorEnumValue("cl6x", ClxxCCompiler.getCl6xInstance()),
            new ProcessorEnumValue("cl55", ClxxCCompiler.getCl55Instance()),
            new ProcessorEnumValue("armcc", ADSCCompiler.getArmCC()),
            new ProcessorEnumValue("armcpp", ADSCCompiler.getArmCpp()),
            new ProcessorEnumValue("tcc", ADSCCompiler.getThumbCC()),
            new ProcessorEnumValue("tcpp", ADSCCompiler.getThumbCpp()),
            // GCC Cross Compilers
            new ProcessorEnumValue(
                    "sparc-sun-solaris2-gcc",
                    com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GccCCompiler
                            .getInstance()),
            new ProcessorEnumValue(
                    "sparc-sun-solaris2-g++",
                    com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GccCCompiler
                            .getGppInstance()),
            new ProcessorEnumValue(
                    "sparc-sun-solaris2-c++",
                    com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GccCCompiler
                            .getCppInstance()),
            new ProcessorEnumValue(
                    "sparc-sun-solaris2-g77",
                    com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GccCCompiler
                            .getG77Instance()),
            // GCC Cross Compilers
            new ProcessorEnumValue("gcc-cross",
                    com.github.maven_nar.cpptasks.gcc.cross.GccCCompiler
                            .getInstance()),
            new ProcessorEnumValue("g++-cross",
                    com.github.maven_nar.cpptasks.gcc.cross.GccCCompiler
                            .getGppInstance()),
            new ProcessorEnumValue("c++-cross",
                    com.github.maven_nar.cpptasks.gcc.cross.GccCCompiler
                            .getCppInstance()),
            new ProcessorEnumValue("g77-cross",
                    com.github.maven_nar.cpptasks.gcc.cross.GccCCompiler
                            .getG77Instance()),
			new ProcessorEnumValue("uic", UserInterfaceCompiler.getInstance()),
			new ProcessorEnumValue("moc", MetaObjectCompiler.getInstance()),
			new ProcessorEnumValue("xpidl", XpidlCompiler.getInstance()),
			new ProcessorEnumValue("wcl", OpenWatcomCCompiler.getInstance()),
			new ProcessorEnumValue("wfl", OpenWatcomFortranCompiler.getInstance()),
            new ProcessorEnumValue("windres", WindresResourceCompiler.getInstance())
                            };
    public Compiler getCompiler() {
        return (Compiler) compilers[getIndex()].getProcessor();
    }
    public String[] getValues() {
        return ProcessorEnumValue.getValues(compilers);
    }
}
