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

import com.github.maven_nar.cpptasks.arm.ADSLinker;
import com.github.maven_nar.cpptasks.borland.BorlandLinker;
import com.github.maven_nar.cpptasks.compaq.CompaqVisualFortranLinker;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.msvc.MsvcLinker;
import com.github.maven_nar.cpptasks.gcc.GccLibrarian;
import com.github.maven_nar.cpptasks.gcc.GccLinker;
import com.github.maven_nar.cpptasks.gcc.GppLinker;
import com.github.maven_nar.cpptasks.gcc.LdLinker;
import com.github.maven_nar.cpptasks.hp.aCCLinker;
import com.github.maven_nar.cpptasks.ibm.VisualAgeLinker;
import com.github.maven_nar.cpptasks.intel.IntelLinux32CLinker;
import com.github.maven_nar.cpptasks.intel.IntelLinux32Linker;
import com.github.maven_nar.cpptasks.intel.IntelLinux64CLinker;
import com.github.maven_nar.cpptasks.intel.IntelLinux64Linker;
import com.github.maven_nar.cpptasks.intel.IntelWin32Linker;
import com.github.maven_nar.cpptasks.openwatcom.OpenWatcomCLinker;
import com.github.maven_nar.cpptasks.openwatcom.OpenWatcomFortranLinker;
import com.github.maven_nar.cpptasks.os390.OS390Linker;
import com.github.maven_nar.cpptasks.os400.IccLinker;
import com.github.maven_nar.cpptasks.sun.C89Linker;
import com.github.maven_nar.cpptasks.sun.ForteCCLinker;
import com.github.maven_nar.cpptasks.ti.ClxxLinker;


/**
 * Enumeration of supported linkers
 * 
 * @author Curt Arnold
 *  
 */
public class LinkerEnum extends EnumeratedAttribute {
    private final static ProcessorEnumValue[] linkers = new ProcessorEnumValue[]{
            new ProcessorEnumValue("gcc", GccLinker.getInstance()),
            new ProcessorEnumValue("g++", GppLinker.getInstance()),
            new ProcessorEnumValue("clang", GccLinker.getCLangInstance()),
            new ProcessorEnumValue("clang++", GppLinker.getCLangInstance()),
            new ProcessorEnumValue("ld", LdLinker.getInstance()),
            new ProcessorEnumValue("ar", GccLibrarian.getInstance()),
            new ProcessorEnumValue("msvc", MsvcLinker.getInstance()),
            new ProcessorEnumValue("bcc", BorlandLinker.getInstance()),
            new ProcessorEnumValue("df", CompaqVisualFortranLinker
                    .getInstance()),
            new ProcessorEnumValue("icl", IntelWin32Linker.getInstance()),
            new ProcessorEnumValue("ecl", IntelWin32Linker.getInstance()),
// BEGINFREEHEP
            new ProcessorEnumValue("icc", IntelLinux32CLinker.getInstance()),
            new ProcessorEnumValue("ecc", IntelLinux64CLinker.getInstance()),
            new ProcessorEnumValue("icpc", IntelLinux32Linker.getInstance()),
            new ProcessorEnumValue("ecpc", IntelLinux64Linker.getInstance()),
// ENDFREEHEP
            new ProcessorEnumValue("CC", ForteCCLinker.getInstance()),
            new ProcessorEnumValue("aCC", aCCLinker.getInstance()),
            new ProcessorEnumValue("os390", OS390Linker.getInstance()),
            new ProcessorEnumValue("os390batch", OS390Linker
                    .getDataSetInstance()),
            new ProcessorEnumValue("os400", IccLinker.getInstance()),
            new ProcessorEnumValue("sunc89", C89Linker.getInstance()),
            new ProcessorEnumValue("xlC", VisualAgeLinker.getInstance()),
            new ProcessorEnumValue("cl6x", ClxxLinker.getCl6xInstance()),
            new ProcessorEnumValue("cl55", ClxxLinker.getCl55Instance()),
            new ProcessorEnumValue("armcc", ADSLinker.getInstance()),
            new ProcessorEnumValue("armcpp", ADSLinker.getInstance()),
            new ProcessorEnumValue("tcc", ADSLinker.getInstance()),
            new ProcessorEnumValue("tcpp", ADSLinker.getInstance()),
            // gcc cross compilers
            new ProcessorEnumValue(
                    "sparc-sun-solaris2-gcc",
                    com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GccLinker
                            .getInstance()),
            new ProcessorEnumValue(
                    "sparc-sun-solaris2-g++",
                    com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GppLinker
                            .getInstance()),
            new ProcessorEnumValue(
                    "sparc-sun-solaris2-ld",
                    com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.LdLinker
                            .getInstance()),
            new ProcessorEnumValue(
                    "sparc-sun-solaris2-ar",
                    com.github.maven_nar.cpptasks.gcc.cross.sparc_sun_solaris2.GccLibrarian
                            .getInstance()),
            new ProcessorEnumValue("gcc-cross",
                    com.github.maven_nar.cpptasks.gcc.cross.GccLinker
                            .getInstance()),
            new ProcessorEnumValue("g++-cross",
                    com.github.maven_nar.cpptasks.gcc.cross.GppLinker
                            .getInstance()),
            new ProcessorEnumValue("ld-cross",
                    com.github.maven_nar.cpptasks.gcc.cross.LdLinker.getInstance()),
            new ProcessorEnumValue("ar-cross",
                    com.github.maven_nar.cpptasks.gcc.cross.GccLibrarian
                            .getInstance()),
			new ProcessorEnumValue("wcl", OpenWatcomCLinker.getInstance()),
			new ProcessorEnumValue("wfl", OpenWatcomFortranLinker.getInstance()),
							};
    public Linker getLinker() {
        return (Linker) linkers[getIndex()].getProcessor();
    }
    public String[] getValues() {
        return ProcessorEnumValue.getValues(linkers);
    }
}
