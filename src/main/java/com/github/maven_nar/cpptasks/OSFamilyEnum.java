package com.github.maven_nar.cpptasks;

import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Enumeration of cpu types.
 *
 * @author Curt Arnold
 *
 */
public final class OSFamilyEnum
    extends EnumeratedAttribute {
  /**
   * Constructor.
   *
   * Set by default to "pentium3"
   *
   * @see java.lang.Object#Object()
   */
  public OSFamilyEnum() {
    setValue("windows");
  }

  /**
   * Gets list of acceptable values.
   *
   * @see org.apache.tools.ant.types.EnumeratedAttribute#getValues()
   */
  public String[] getValues() {
    return new String[] {
        "windows",
        "dos",
        "mac",
        "unix",
        "netware",
        "os/2",
        "tandem",
        "win9x",
        "z/os",
        "os/400",
        "openvms"};
  }
}
