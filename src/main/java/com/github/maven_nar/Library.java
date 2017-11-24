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
package com.github.maven_nar;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Sets up a library to create
 *
 * @author Mark Donszelmann
 */
public class Library implements Executable {

  public static final String STATIC = "static";

  public static final String SHARED = "shared";

  public static final String EXECUTABLE = "executable";

  public static final String JNI = "jni";

  public static final String PLUGIN = "plugin";

  public static final String NONE = "none"; // no library produced

  /**
   * Type of the library to generate. Possible choices are: "plugin", "shared",
   * "static", "jni" or "executable".
   * Defaults to "shared".
   */
  @Parameter
  private String type = SHARED;

  /**
   * Type of subsystem to generate: "gui", "console", "other". Defaults to
   * "console".
   */
  @Parameter
  private String subSystem = "console";

  /**
   * Link with stdcpp if necessary Defaults to true.
   */
  @Parameter(defaultValue = "true")
  private boolean linkCPP = true;

  /**
   * Link with fortran runtime if necessary Defaults to false.
   */
  @Parameter
  private boolean linkFortran = false;

  /**
   * Link with fortran startup, so that the gcc linker can find the "main" of
   * fortran. Defaults to false.
   */
  @Parameter
  private boolean linkFortranMain = false;

  /**
   * If specified will create the NarSystem class with methods to load a JNI
   * library.
   */
  @Parameter
  private String narSystemPackage = null;

  /**
   * Name of the NarSystem class
   */
  @Parameter(defaultValue = "NarSystem", required = true)
  private String narSystemName = "NarSystem";

  /**
   * The target directory into which to generate the output.
   */
  @Parameter(defaultValue = "${project.build.dir}/nar/nar-generated", required = true)
  private String narSystemDirectory = "nar-generated";

  /**
   * When true and if type is "executable" run this executable. Defaults to
   * false;
   */
  @Parameter
  private boolean run = false;

  /**
   * Arguments to be used for running this executable. Defaults to empty list.
   * This option is only used if run=true
   * and type=executable.
   */
  @Parameter
  private List/* <String> */args = new ArrayList();

  /**
   * List of artifact:binding  for type of dependency to link against when there is a choice.
   */
  @Parameter
  private List<String> dependencyBindings = new ArrayList<>();
  
  @Override
  public final List/* <String> */getArgs() {
    return this.args;
  }

  public String getBinding(NarArtifact dependency) {
    for (String dependBind : dependencyBindings ) {
      String[] pair = dependBind.trim().split( ":", 2 );
      if( dependency.getArtifactId().equals(pair[0].trim()) ){
        String result = pair[1].trim();
        if( !result.isEmpty() )
          return result;
      }
    }
    return null;
  }

  public final String getNarSystemDirectory() {
    return this.narSystemDirectory;
  }

  public final String getNarSystemName() {
    return this.narSystemName;
  }

  public final String getNarSystemPackage() {
    return this.narSystemPackage;
  }

  public String getSubSystem() {
    return this.subSystem;
  }

  public final String getType() {
    return this.type;
  }

  public final boolean linkCPP() {
    return this.linkCPP;
  }

  public final boolean linkFortran() {
    return this.linkFortran;
  }

  public final boolean linkFortranMain() {
    return this.linkFortranMain;
  }

  @Override
  public final boolean shouldRun() {
    return this.run;
  }

  // FIXME incomplete
  @Override
  public final String toString() {
    final String sb = "Library: " + "type: " +
        getType();
    return sb;
  }
}
