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
  private final String name = null;

  /**
   * Type of linking used for this test Possible choices are: "shared" or
   * "static". Defaults to "shared".
   */
  @Parameter(defaultValue = "shared")
  private final String link = Library.SHARED;

  /**
   * When true run this test. Defaults to true;
   */
  @Parameter(defaultValue = "true")
  private final boolean run = true;

  /**
   * Arguments to be used for running this test. Defaults to empty list. This
   * option is only used if run=true.
   */
  @Parameter
  private final List/* <String> */args = new ArrayList();

  @Override
  public final List/* <String> */getArgs() {
    return this.args;
  }

  public final String getLink() {
    return this.link;
  }

  public final String getName() throws MojoFailureException {
    if (this.name == null) {
      throw new MojoFailureException("NAR: Please specify <Name> as part of <Test>");
    }
    return this.name;
  }

  @Override
  public final boolean shouldRun() {
    return this.run;
  }
}
