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

import org.apache.maven.plugin.MojoExecutionException;

public class Script {
  
  protected String id;
  protected String scriptType;
  protected String mode;
  protected boolean compile;
  protected boolean link;
  protected boolean testCompile;
  protected boolean testLink;
  protected boolean echoLines;
  
  protected List<String> headers;
  protected List<Substitution> substitutions;
  protected List<String> footers;
  
  public Script() {
    this.scriptType = "sh";
    this.mode = "0755";
    this.compile = true;
    this.link = true;
    this.testCompile = false;
    this.testLink = false;
    this.echoLines = true;
    this.headers = new ArrayList<>();
    this.substitutions = new ArrayList<>();
    this.footers = new ArrayList<>();
  }

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

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
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

  public boolean isEchoLines() {
    return echoLines;
  }

  public void setEchoLines(boolean echoLines) {
    this.echoLines = echoLines;
  }

  public List<String> getHeaders() {
    return headers;
  }

  public void setHeaders(List<String> headers) {
    this.headers = headers;
  }

  public List<Substitution> getSubstitutions() {
    return substitutions;
  }

  public void setSubstitutions(List<Substitution> substitutions) {
    this.substitutions = substitutions;
  }

  public List<String> getFooters() {
    return footers;
  }

  public void setFooters(List<String> footers) {
    this.footers = footers;
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