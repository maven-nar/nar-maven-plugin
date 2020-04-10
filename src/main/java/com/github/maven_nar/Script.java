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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

public class Script {
  @Parameter(required = true)
  protected String id;
  
  @Parameter(defaultValue = "sh", required = true)
  protected String scriptType;
  
  @Parameter(defaultValue = "true", required = true)
  protected boolean compile;
  
  @Parameter(defaultValue = "true", required = true)
  protected boolean link;
  
  @Parameter(defaultValue = "false", required = true)
  protected boolean testCompile;
  
  @Parameter(defaultValue = "false", required = true)
  protected boolean testLink;
  
  @Parameter
  protected List<Substitution> substitutions;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getScriptType() {
    return scriptType;
  }

  public void setScriptType(String scriptType) {
    this.scriptType = scriptType;
  }

  public boolean isCompile() {
    return compile;
  }

  public void setCompile(boolean compile) {
    this.compile = compile;
  }

  public boolean isLink() {
    return link;
  }

  public void setLink(boolean link) {
    this.link = link;
  }

  public boolean isTestCompile() {
    return testCompile;
  }

  public void setTestCompile(boolean testCompile) {
    this.testCompile = testCompile;
  }

  public boolean isTestLink() {
    return testLink;
  }

  public void setTestLink(boolean testLink) {
    this.testLink = testLink;
  }

  public List<Substitution> getSubstitutions() {
    return substitutions;
  }

  public void setSubstitutions(List<Substitution> substitutions) {
    this.substitutions = substitutions;
  }

  public String getExtension() throws MojoExecutionException {
    switch (scriptType) {
      case "sh": return "sh";
      case "bash": return "sh";
      case "bat": return "bat";
      default: throw new MojoExecutionException("Unkown script type " + scriptType);
    }
  }
}