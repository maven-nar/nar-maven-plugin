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

import java.io.File;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Allows replaying a NAR build as a script execution
 *
 * @author Brent Chiodo
 */
public class Replay {
  
  public String[] scriptTypes = new String[] {"sh", "bash", "bat"};
  
  @Parameter
  protected File outputDirectory;
  
  @Parameter 
  protected File scriptDirectory; 
  
  @Parameter
  protected List<Script> scripts;

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public File getScriptDirectory() {
    return scriptDirectory;
  }

  public void setScriptDirectory(File scriptDirectory) {
    this.scriptDirectory = scriptDirectory;
  }

  public List<Script> getScripts() {
    return scripts;
  }

  public void setScripts(List<Script> scripts) {
    this.scripts = scripts;
  }
}
