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

public class CommandHistory {
  private final List<String[]> compileCommands;
  private final List<String[]> linkCommands;
  private final List<String[]> testCompileCommands;
  private final List<String[]> testLinkCommands;
  
  public CommandHistory() {
    this.compileCommands = new ArrayList<String[]>();
    this.linkCommands = new ArrayList<String[]>();
    this.testCompileCommands = new ArrayList<String[]>();
    this.testLinkCommands = new ArrayList<String[]>();
  }

  public List<String[]> getCompileCommands() {
    return compileCommands;
  }

  public List<String[]> getLinkCommands() {
    return linkCommands;
  }

  public List<String[]> getTestCompileCommands() {
    return testCompileCommands;
  }

  public List<String[]> getTestLinkCommands() {
    return testLinkCommands;
  }
}
