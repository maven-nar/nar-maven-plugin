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
package com.github.maven_nar.cpptasks.types;

import java.util.Objects;

/**
 * A linker command line argument.
 */
public class LinkerArgument extends CommandLineArgument {
  public LinkerArgument() {
  }

  public void execute() throws org.apache.tools.ant.BuildException {
    throw new org.apache.tools.ant.BuildException("Not an actual task, but looks like one for documentation purposes");
  }

  /**
   * Since equals method is overloaded, also overload hashCode() to be consistent
   * when comparing objects for collections.
   * @return calculated hashcode of an object.
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(this.getValue());
  }


  /**
   * Override default equals method to compare objects based on the string value,
   * consumed by Collections (Vector, etc.,) methods for its comparisons.
   * @return true if the comparing object has the same superclass & contains the same value.
   */
  @Override
  public boolean equals(Object arg)
  {
    if((arg== null) || (arg.getClass() != LinkerArgument.class)) {
      return false;
    }
    LinkerArgument cmdLineArg = (LinkerArgument) arg;
    return (cmdLineArg.compareStrings(cmdLineArg.getValue(),this.getValue())
            && cmdLineArg.getLocation() == this.getLocation()
            && cmdLineArg.compareStrings(cmdLineArg.getIfCond(),this.getIfCond())
            && cmdLineArg.compareStrings(cmdLineArg.getUnlessCond(),this.getUnlessCond()));
  }
}
