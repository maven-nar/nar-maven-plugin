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
