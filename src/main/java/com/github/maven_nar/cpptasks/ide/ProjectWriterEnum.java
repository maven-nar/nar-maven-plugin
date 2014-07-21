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
package com.github.maven_nar.cpptasks.ide;

import org.apache.tools.ant.types.EnumeratedAttribute;

import com.github.maven_nar.cpptasks.apple.XcodeProjectWriter;
import com.github.maven_nar.cpptasks.borland.CBuilderXProjectWriter;
import com.github.maven_nar.cpptasks.msvc.MsvcProjectWriter;
import com.github.maven_nar.cpptasks.msvc.VisualStudioNETProjectWriter;

/**
 * Enumeration of supported project file generators.
 *
 * <table width="100%" border="1"> <thead>Supported project generators </thead>
 * <tr>
 * <td>cbuilderx</td>
 * <td>Borland C++BuilderX</td>
 * </tr>
 * <tr>
 * <td>msvc5</td>
 * <td>Microsoft Visual C++ 97</td>
 * </tr>
 * <tr>
 * <td>msvc6</td>
 * <td>Microsoft Visual C++ 6</td>
 * </tr>
 * <tr>
 * <td>msvc7</td>
 * <td>Microsoft Visual C++.NET</td>
 * </tr>
 * <tr>
 * <td>msvc71</td>
 * <td>Microsoft Visual C++.NET 2003</td>
 * </tr>
 * <tr>
 * <td>msvc8</td>
 * <td>Microsoft Visual C++ 2005</td>
 * </tr>
 * <tr>
 * <td>msvc9</td>
 * <td>Microsoft Visual C++ 2008</td>
 * </tr>
 * <tr>
 * <td>xcode</td>
 * <td>Apple Xcode</td>
 * </tr>
 * </table>
 *
 * @author Curt Arnold
 *
 */
public final class ProjectWriterEnum
    extends EnumeratedAttribute {
  /**
   * Enumeration values.
   */
  private static String[] values = new String[] {
      "cbuilderx", "msvc5",
      "msvc6", "msvc7", "msvc71", "msvc8", "msvc9", "xcode"};

  /**
   * Project writers associated with enumeration values.
   */
  private static ProjectWriter[] writers = new ProjectWriter[] {
      new CBuilderXProjectWriter(), new MsvcProjectWriter("5.00"),
      new MsvcProjectWriter("6.00"),
      new VisualStudioNETProjectWriter("7.00", "TRUE", "FALSE"),
      new VisualStudioNETProjectWriter("7.10", "TRUE", "FALSE"),
      new VisualStudioNETProjectWriter("8.00", "true", "false"),
      new VisualStudioNETProjectWriter("9.00", "true", "false"),
      new XcodeProjectWriter()};

  /**
   * Gets ProjectWriter associated with enumeration value.
   *
   * @return project writer
   */
  public ProjectWriter getProjectWriter() {
    return writers[this.getIndex()];
  }

  /**
   * Gets acceptible values for enumeration.
   *
   * @return acceptible values
   */
  public String[] getValues() {
    return (String[]) values.clone();
  }
}

