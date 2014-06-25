package com.github.maven_nar.cpptasks;

import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Enumeration of cpu types.
 *
 * @author Curt Arnold
 *
 */
public final class CPUEnum
    extends EnumeratedAttribute {

  /**
   * Constructor.
   *
   * Set by default to "pentium3"
   *
   * @see java.lang.Object#Object()
   */
  public CPUEnum() {
    setValue("pentium3");
  }

  /**
   * Gets list of acceptable values.
   *
   * @see org.apache.tools.ant.types.EnumeratedAttribute#getValues()
   */
  public String[] getValues() {
    return new String[] {
        "i386",
        "i486",
        "i586",
        "i686",
        "pentium",
        "pentium-mmx",
        "pentiumpro",
        "pentium2",
        "pentium3",
        "pentium4",
        "k6",
        "k6-2",
        "k6-3",
        "athlon",
        "athlon-tbird",
        "athlon-4",
        "athlon-xp",
        "athlon-mp",
        "winchip-c6",
        "winchip2",
        "c3" };
  }

}
