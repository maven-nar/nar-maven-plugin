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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Sets up a test to create
 *
 * @author Mark Donszelmann
 */
public class Test implements Executable {

  /**
   * Name of the test to create
   */
  @Parameter(required = true)
  private String name = null;

  /**
   * Type of linking to main artifact used for this test.
   * Possible choices are: "shared" or "static".
   * Defaults to library type if single library is built or "shared" otherwise.
   */
  @Parameter(defaultValue = "shared")
  private String link = null;

  /**
   * When true run this test. Defaults to true;
   */
  @Parameter(defaultValue = "true")
  private boolean run = true;

  /**
   * Type of the library to generate. 
   * Possible choices are: "shared", "static" or "executable".
   * Defaults to "executable".
   */
  @Parameter
  private String type = Library.EXECUTABLE;
  
  /**
   * Arguments to be used for running this test. Defaults to empty list. This
   * option is only used if run=true.
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
      String[] pair = dependBind.trim().split( ":", 2 );  // TODO: match how much?
      if( dependency.getArtifactId().equals(pair[0].trim()) ){
        String result = pair[1].trim();
        if( !result.isEmpty() )
          return result;
      }
    }
    return null;
  }

  public final String getLink( List<Library> libraries ) {
    if( this.link != null )
      return this.link;
    
    String libraryPreferred = null;
    if(libraries.size() == 1){
      String type = libraries.get(0).getType();
      if (Library.SHARED.equals(type)||Library.STATIC.equals(type) )
        libraryPreferred = type;
      //if(Library.JNI.equals(type)) default shared
    }
    return libraryPreferred == null ? Library.SHARED : libraryPreferred; 
  }
  
  public final String getName() throws MojoFailureException {
    if (this.name == null) {
      throw new MojoFailureException("NAR: Please specify <Name> as part of <Test>");
    }
    return this.name;
  }

  public String getType() {
    return this.type;
  }

  @Override
  public final boolean shouldRun() {
    return this.run;
  }
}
