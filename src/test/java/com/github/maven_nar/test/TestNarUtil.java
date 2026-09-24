/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2026 NAR Maven Plugin developers.
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
package com.github.maven_nar.test;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;

import junit.framework.TestCase;

import com.github.maven_nar.NarUtil;

public class TestNarUtil extends TestCase {
  public void testParseOctalPermissionGroupWrite() {
    assertEquals(EnumSet.of(PosixFilePermission.GROUP_WRITE), NarUtil.parseOctalPermission("0020"));
  }

  public void testParseOctalPermissionModes() {
    final String[] symbolic = {"---", "--x", "-w-", "-wx", "r--", "r-x", "rw-", "rwx"};

    // Compare all modes against symbolic permissions without requiring a POSIX file system.
    for (int owner = 0; owner < symbolic.length; owner++) {
      for (int group = 0; group < symbolic.length; group++) {
        for (int others = 0; others < symbolic.length; others++) {
          final String octal = "0" + owner + group + others;
          assertEquals("Permissions for " + octal,
              PosixFilePermissions.fromString(symbolic[owner] + symbolic[group] + symbolic[others]),
              NarUtil.parseOctalPermission(octal));
        }
      }
    }
  }

  public void testParseOctalPermissionNull() {
    assertNull(NarUtil.parseOctalPermission(null));
  }
}
