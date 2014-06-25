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
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks;

import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Enumeration of optimization levels (experimental).
 *
 * @author Curt Arnold
 *
 */
public final class OptimizationEnum
    extends EnumeratedAttribute {
  /**
   * Constructor.
   *
   * Set by default to "speed"
   *
   * @see java.lang.Object#Object()
   */
  public OptimizationEnum() {
    setValue("speed");
  }

  /**
   * Gets list of acceptable values.
   *
   * @see org.apache.tools.ant.types.EnumeratedAttribute#getValues()
   */
  public String[] getValues() {
    return new String[] {
        "none",
        "size",
        "minimal",
        "speed",
        "full",
        "aggressive",
        "extreme",
        "unsafe"
    };
  }

  /**
   * Is size optimized.
   * @return boolean true if size is optimized.
   */
  public boolean isSize() {
    return "size".equals(getValue());
  }

  /**
   * Is speed optimized.
   * @return boolean true if speed is optimized.
   */
  public boolean isSpeed() {
    return !isSize() && !isNoOptimization();
  }

  /**
   * Is no optimization performed.
   * @return boolean true if no optimization is performed.
   */
  public boolean isNoOptimization() {
    return "none".equals(getValue());
  }

}
