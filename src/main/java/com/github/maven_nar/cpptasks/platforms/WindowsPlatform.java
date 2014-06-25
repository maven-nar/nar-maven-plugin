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
package com.github.maven_nar.cpptasks.platforms;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.TargetMatcher;
import com.github.maven_nar.cpptasks.VersionInfo;
import com.github.maven_nar.cpptasks.compiler.LinkType;



/**
 * Platform specific behavior for Microsoft Windows.
 *
 * @author Curt Arnold
 */
public final class WindowsPlatform {

  /**
   * Constructor.
   */
  private WindowsPlatform() {
  }
  /**
   * Adds source or object files to the bidded fileset to
   * support version information.
   *
   * @param versionInfo version information
   * @param linkType link type
   * @param isDebug true if debug build
   * @param outputFile name of generated executable
   * @param objDir directory for generated files
   * @param matcher bidded fileset
   * @throws IOException if unable to write version resource
   */
  public static void addVersionFiles(final VersionInfo versionInfo,
                                     final LinkType linkType,
                                     final File outputFile,
                                     final boolean isDebug,
                                     final File objDir,
                                     final TargetMatcher matcher)
      throws IOException {
    if (versionInfo == null) {
      throw new NullPointerException("versionInfo");
    }
    if (linkType == null) {
      throw new NullPointerException("linkType");
    }
    if (outputFile == null) {
      throw new NullPointerException("outputFile");
    }
    if (objDir == null) {
      throw new NullPointerException("objDir");
    }

    /**
     * Fully resolve version info
     */
    VersionInfo mergedInfo = versionInfo.merge();

    File versionResource = new File(objDir, "versioninfo.rc");

    boolean notChanged = false;
    //
    //   if the resource exists
    //
    if (versionResource.exists()) {
      ByteArrayOutputStream memStream = new ByteArrayOutputStream();
      Writer writer = new BufferedWriter(new OutputStreamWriter(memStream));
      writeResource(writer, mergedInfo, outputFile, isDebug, linkType);
      writer.close();
      ByteArrayInputStream proposedResource = new ByteArrayInputStream(
          memStream.toByteArray());

      InputStream existingResource = new FileInputStream(versionResource);
      //
      //
      //
      notChanged = hasSameContent(proposedResource, existingResource);
      existingResource.close();
    }

    //
    //   if the resource file did not exist or will be changed then
    //       write the file
    //
    if (!notChanged) {
      Writer writer = new BufferedWriter(
          new OutputStreamWriter(
          new FileOutputStream(versionResource)));
      writeResource(writer, mergedInfo, outputFile, isDebug, linkType);
      writer.close();
    }
    if (matcher != null) {
      matcher.visit(new File(versionResource.getParent()),
                    versionResource.getName());
    }

  }

  /**
   * Compare two input streams for duplicate content
   *
   * Naive implementation, but should not be performance issue.
   * @param stream1 stream
   * @param stream2 stream
   * @return true if streams are identical in content
   * @throws IOException if error reading streams
   */
  private static boolean hasSameContent(final InputStream stream1,
                                        final InputStream stream2)
      throws IOException {
    int byte1 = -1;
    int byte2 = -1;
    do {
      byte1 = stream1.read();
      byte2 = stream2.read();

    }
    while (byte1 == byte2 && byte1 != -1);
    return (byte1 == byte2);
  }


  /**
   * Parse version string into array of four short values.
   * @param version String version
   * @return short[] four element array
   */
  public static short[] parseVersion(final String version) {
    short[] values = new short[] {
        0, 0, 0, 0};
    if (version != null) {
      StringBuffer buf = new StringBuffer(version);
      int start = 0;
      for (int i = 0; i < 4; i++) {
        int end = version.indexOf('.', start);
        if (end <= 0) {
          end = version.length();
          for (int j = end; j > start; j--) {
            String part = buf.substring(start, end);
            try {
              values[i] = Short.parseShort(part);
              break;
            } catch (NumberFormatException ex) {
              values[i] = 0;
            }
          }
          break;
        } else {
          String part = buf.substring(start, end);
          try {
            values[i] = Short.parseShort(part);
            start = end + 1;
          } catch (NumberFormatException ex) {
            break;
          }
        }
      }
    }
    return values;
  }

  /**
   * Converts parsed version information into a string representation.
   *
   * @param buf StringBuffer string buffer to receive version number
   * @param version short[] four-element array
   */
  private static void encodeVersion(final StringBuffer buf,
                                    final short[] version) {
    for (int i = 0; i < 3; i++) {
      buf.append(Short.toString(version[i]));
      buf.append(',');
    }
    buf.append(Short.toString(version[3]));
  }

  /**
   * Writes windows resource.
   * @param writer writer, may not be nul
   * @param versionInfo version information
   * @param outputFile executable file
   * @param isDebug true if debug
   * @param linkType link type
   * @throws IOException if error writing resource file
   */
  public static void writeResource(final Writer writer,
                                   final VersionInfo versionInfo,
                                   final File outputFile,
                                   final boolean isDebug,
                                   final LinkType linkType) throws IOException {

    //writer.write("#include \"windows.h\"\n");

    writer.write("VS_VERSION_INFO VERSIONINFO\n");
    StringBuffer buf = new StringBuffer("FILEVERSION ");
    encodeVersion(buf, parseVersion(versionInfo.getFileversion()));
    buf.append("\nPRODUCTVERSION ");
    encodeVersion(buf, parseVersion(versionInfo.getProductversion()));
    buf.append("\n");
    writer.write(buf.toString());
    buf.setLength(0);
    buf.append("FILEFLAGSMASK 0x1L /* VS_FF_DEBUG */");
    Boolean patched = versionInfo.getPatched();
    Boolean prerelease = versionInfo.getPrerelease();
    if (patched != null) {
      buf.append(" | 0x4L /* VS_FF_PATCHED */");
    }
    if (prerelease != null) {
      buf.append(" | 0x2L /* VS_FF_PRERELEASE */");
    }
    if (versionInfo.getPrivatebuild() != null) {
      buf.append(" | 0x8L /* VS_FF_PRIVATEBUILD */");
    }
    if (versionInfo.getSpecialbuild() != null) {
      buf.append(" | 0x20L /* VS_FF_SPECIALBUILD */");
    }
    buf.append('\n');
    writer.write(buf.toString());
    buf.setLength(0);
    buf.append("FILEFLAGS ");

    if (isDebug) {
      buf.append("0x1L /* VS_FF_DEBUG */ | ");
    }
    if (Boolean.TRUE.equals(patched)) {
      buf.append("0x4L /* VS_FF_PATCHED */ | ");
    }
    if (Boolean.TRUE.equals(prerelease)) {
      buf.append("0x2L /* VS_FF_PRERELEASE */ | ");
    }
    if (Boolean.TRUE.equals(versionInfo.getPrivatebuild())) {
      buf.append("0x8L /* VS_FF_PRIVATEBUILD */ | ");
    }
    if (Boolean.TRUE.equals(versionInfo.getSpecialbuild())) {
      buf.append("0x20L /* VS_FF_SPECIALBUILD */ | ");
    }
    if (buf.length() > 10) {
      buf.setLength(buf.length() - 3);
      buf.append('\n');
    } else {
      buf.append("0\n");
    }
    writer.write(buf.toString());
    buf.setLength(0);

    writer.write("FILEOS 0x40004 /* VOS_NT_WINDOWS32 */\nFILETYPE ");
    if (linkType.isExecutable()) {
      writer.write("0x1L /* VFT_APP */\n");
    } else {
      if (linkType.isSharedLibrary()) {
        writer.write("0x2L /* VFT_DLL */\n");
      } else if (linkType.isStaticLibrary()) {
        writer.write("0x7L /* VFT_STATIC_LIB */\n");
      } else {
        writer.write("0x0L /* VFT_UNKNOWN */\n");
      }
    }
    writer.write("FILESUBTYPE 0x0L\n");
    writer.write("BEGIN\n");
    writer.write("BLOCK \"StringFileInfo\"\n");
    writer.write("   BEGIN\n#ifdef UNICODE\nBLOCK \"040904B0\"\n");
    writer.write("#else\nBLOCK \"040904E4\"\n#endif\n");
    writer.write("BEGIN\n");
    if (versionInfo.getFilecomments() != null) {
      writer.write("VALUE \"Comments\", \"");
      writer.write(versionInfo.getFilecomments());
      writer.write("\\0\"\n");
    }
    if (versionInfo.getCompanyname() != null) {
      writer.write("VALUE \"CompanyName\", \"");
      writer.write(versionInfo.getCompanyname());
      writer.write("\\0\"\n");
    }
    if (versionInfo.getFiledescription() != null) {
      writer.write("VALUE \"FileDescription\", \"");
      writer.write(versionInfo.getFiledescription());
      writer.write("\\0\"\n");
    }
    if (versionInfo.getFileversion() != null) {
      writer.write("VALUE \"FileVersion\", \"");
      writer.write(versionInfo.getFileversion());
      writer.write("\\0\"\n");
    }
    String baseName = CUtil.getBasename(outputFile);
    String internalName = versionInfo.getInternalname();
    if (internalName == null) {
      internalName = baseName;
    }
    writer.write("VALUE \"InternalName\", \"");
    writer.write(internalName);
    writer.write("\\0\"\n");
    if (versionInfo.getLegalcopyright() != null) {
      writer.write("VALUE \"LegalCopyright\", \"");
      writer.write(versionInfo.getLegalcopyright());
      writer.write("\\0\"\n");
    }
    if (versionInfo.getLegaltrademarks() != null) {
      writer.write("VALUE \"LegalTrademarks\", \"");
      writer.write(versionInfo.getLegaltrademarks());
      writer.write("\\0\"\n");
    }
    writer.write("VALUE \"OriginalFilename\", \"");
    writer.write(baseName);
    writer.write("\\0\"\n");
    if (versionInfo.getPrivatebuild() != null) {
      writer.write("VALUE \"PrivateBuild\", \"");
      writer.write(versionInfo.getPrivatebuild());
      writer.write("\\0\"\n");
    }
    if (versionInfo.getProductname() != null) {
      writer.write("VALUE \"ProductName\", \"");
      writer.write(versionInfo.getProductname());
      writer.write("\\0\"\n");
    }
    if (versionInfo.getProductversion() != null) {
      writer.write("VALUE \"ProductVersion\", \"");
      writer.write(versionInfo.getProductversion());
      writer.write("\\0\"\n");
    }
    if (versionInfo.getSpecialbuild() != null) {
      writer.write("VALUE \"SpecialBuild\", \"");
      writer.write(versionInfo.getSpecialbuild());
      writer.write("\\0\"\n");
    }
    writer.write("END\n");
    writer.write("END\n");

    writer.write("BLOCK \"VarFileInfo\"\n");
    writer.write("BEGIN\n#ifdef UNICODE\n");
    writer.write("VALUE \"Translation\", 0x409, 1200\n");
    writer.write("#else\n");
    writer.write("VALUE \"Translation\", 0x409, 1252\n");
    writer.write("#endif\n");
    writer.write("END\n");
    writer.write("END\n");
  }

}
