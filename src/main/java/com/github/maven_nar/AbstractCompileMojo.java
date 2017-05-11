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

import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.Project;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractCompileMojo extends AbstractDependencyMojo {

  /**
   * C++ Compiler
   */
  @Parameter
  private Cpp cpp;

  /**
   * C Compiler
   */
  @Parameter
  private C c;

  /**
   * Fortran Compiler
   */
  @Parameter
  private Fortran fortran;

  /**
   * Resource Compiler
   */
  @Parameter
  private Resource resource;

  /**
   * IDL Compiler
   */
  @Parameter
  private IDL idl;

  /**
   * Message Compiler
   */
  @Parameter
  private Message message;

  /**
   * By default NAR compile will attempt to compile using all known compilers
   * against files in the directories specified by convention.
   * This allows configuration to a reduced set, you will have to specify each
   * compiler to use in the configuration.
   */
  @Parameter(defaultValue = "false")
  protected boolean onlySpecifiedCompilers;

  /**
   * Do we log commands that is executed to produce the end-result?
   * Conception was to allow eclipse to sniff out include-paths from compile.
   */
  @Parameter
  protected int commandLogLevel = Project.MSG_VERBOSE;

  /**
   * Maximum number of Cores/CPU's to use. 0 means unlimited.
   */
  @Parameter
  private int maxCores = 0;

  /**
   * Fail on compilation/linking error.
   */
  @Parameter(defaultValue = "true", required = true)
  private boolean failOnError;

  /**
   * Sets the type of runtime library, possible values "dynamic", "static".
   */
  @Parameter(defaultValue = "dynamic", required = true)
  private String runtime;

  /**
   * Set use of libtool. If set to true, the "libtool " will be prepended to the
   * command line for compatible
   * processors.
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean libtool;

  /**
   * List of tests to create
   */
  @Parameter
  private List tests;

  /**
   * Java info for includes and linking
   */
  @Parameter
  private Java java;

  /**
   * To support scanning the code with HPE Fortify.
   * <p>
   * The attribute functions as a flag that indicates Fortify is required,
   * and the value is an ID, prepended to the command line as
   * {@code sourceanalyzer –b <fortifyID>}.
   * </p>
   */
  @Parameter(defaultValue = "")
  private String fortifyID;

  
  /**
   * Flag to cpptasks to indicate whether linker options should be decorated or
   * not
   */
  @Parameter
  protected boolean decorateLinkerOptions;

  private List/* <String> */dependencyLibOrder;

  private Project antProject;

  protected final boolean failOnError(final AOL aol) throws MojoExecutionException {
    return getNarInfo().getProperty(aol, "failOnError", this.failOnError);
  }

  protected final Project getAntProject() {
    if (this.antProject == null) {
      // configure ant project
      this.antProject = new Project();
      this.antProject.setName("NARProject");
      this.antProject.addBuildListener(new NarLogger(getLog()));
    }
    return this.antProject;
  }

  protected final C getC() {
    if (this.c == null && !this.onlySpecifiedCompilers) {
      setC(new C());
    }
    return this.c;
  }

  protected final Cpp getCpp() {
    if (this.cpp == null && !this.onlySpecifiedCompilers) {
      setCpp(new Cpp());
    }
    return this.cpp;
  }

  protected final List/* <String> */getDependencyLibOrder() {
    return this.dependencyLibOrder;
  }

  protected final Fortran getFortran() {
    if (this.fortran == null && !this.onlySpecifiedCompilers) {
      setFortran(new Fortran());
    }
    return this.fortran;
  }

  protected final IDL getIdl() {
    if (this.idl == null && !this.onlySpecifiedCompilers) {
      setIdl(new IDL());
    }
    return this.idl;
  }

  protected final Java getJava() {
    if (this.java == null) {
      this.java = new Java();
    }
    this.java.setAbstractCompileMojo(this);
    return this.java;
  }

  protected final int getMaxCores(final AOL aol) throws MojoExecutionException {
    return getNarInfo().getProperty(aol, "maxCores", this.maxCores);
  }

  protected final Message getMessage() {
    if (this.message == null && !this.onlySpecifiedCompilers) {
      setMessage(new Message());
    }
    return this.message;
  }

  protected final String getOutput(final AOL aol, final String type) throws MojoExecutionException {
    return getNarInfo().getOutput(aol, getOutput(!Library.EXECUTABLE.equals(type)));
  }

  protected final Resource getResource() {
    if (this.resource == null && !this.onlySpecifiedCompilers) {
      setResource(new Resource());
    }
    return this.resource;
  }

  protected final String getRuntime(final AOL aol) throws MojoExecutionException {
    return getNarInfo().getProperty(aol, "runtime", this.runtime);
  }

  protected final List getTests() {
    if (this.tests == null) {
      this.tests = Collections.emptyList();
    }
    return this.tests;
  }

  public void setC(final C c) {
    this.c = c;
    c.setAbstractCompileMojo(this);
  }

  public void setCpp(final Cpp cpp) {
    this.cpp = cpp;
    cpp.setAbstractCompileMojo(this);
  }
  
  protected final String getfortifyID()
  {
    return this.fortifyID;
  }

  public final void setDependencyLibOrder(final List/* <String> */order) {
    this.dependencyLibOrder = order;
  }

  public void setFortran(final Fortran fortran) {
    this.fortran = fortran;
    fortran.setAbstractCompileMojo(this);
  }

  public void setIdl(final IDL idl) {
    this.idl = idl;
    idl.setAbstractCompileMojo(this);
  }

  public void setMessage(final Message message) {
    this.message = message;
    message.setAbstractCompileMojo(this);
  }

  public void setResource(final Resource resource) {
    this.resource = resource;
    resource.setAbstractCompileMojo(this);
  }

  protected final boolean useLibtool(final AOL aol) throws MojoExecutionException {
    return getNarInfo().getProperty(aol, "libtool", this.libtool);
  }

}
