package com.github.maven_nar.cpptasks;

import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Distributer prococol names (experimental).
 *
 * @author Curt Arnold
 *
 */
public final class DistributerProtocolEnum
    extends EnumeratedAttribute {
  /**
   * Constructor.
   *
   * Set by default to "distcc"
   *
   * @see java.lang.Object#Object()
   */
  public DistributerProtocolEnum() {
    setValue("distcc");
  }

  /**
   * Gets list of acceptable values.
   *
   * @see org.apache.tools.ant.types.EnumeratedAttribute#getValues()
   */
  public String[] getValues() {
    return new String[] {
        "distcc",
        "ssh"};
  }
}
