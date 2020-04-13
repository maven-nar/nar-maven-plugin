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
import java.util.regex.Pattern;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Substitutes matched strings in a replay script
 *
 * @author Brent Chiodo
 */
public class Substitution {
  
  private static final String[] types = new String[] {"string", "relativePath", "absolutePath", "regex"};
  private Pattern pattern;
  
  protected String type;
  protected String replace;
  protected String replaceWith;
  
  public Substitution() {
    this.type = "string";
    this.replaceWith = "";
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getReplace() {
    return replace;
  }

  public void setReplace(String replace) {
    this.replace = replace;
  }

  public String getReplaceWith() {
    return replaceWith;
  }

  public void setReplaceWith(String replaceWith) {
    this.replaceWith = replaceWith;
  }
  
  public String substitute(String line) {
    if (replace == null) return line;
    
    String temp = replace;
    if (type.equals("regex")) {
      if (pattern == null) pattern = Pattern.compile(replace);
      return pattern.matcher(line).replaceAll(replaceWith);
      
    }
    else if (type.equals("relativePath")) {
      temp = new File(replace).getPath() + File.separator;
    }
    else if (type.equals("absolutePath")) {
      temp = new File(replace).getAbsolutePath() + File.separator;
    }
    return line.replace(temp, replaceWith);
  }
}
