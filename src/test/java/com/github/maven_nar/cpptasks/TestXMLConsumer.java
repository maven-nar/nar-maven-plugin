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
package com.github.maven_nar.cpptasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import junit.framework.TestCase;

/**
 * Base class for tests on classes that consume or public XML documents.
 *
 * @author Curt Arnold
 * 
 */
public abstract class TestXMLConsumer extends TestCase {
  /**
   * copies a resource to a temporary directory.
   * 
   * @param resourceName
   *          resouce name, such as "files/openshore/history.xml".
   * @param tmpFile
   *          name for temporary file created in /tmp or similar.
   */
  public static void copyResourceToTmpDir(final String resourceName, final String tmpFile) throws IOException {
    String tmpDir = System.getProperty("java.io.tmpdir");

    final File tempdir = Files.createTempDirectory(new File(tmpDir).toPath(), tmpFile + Long.toString(System.nanoTime())).toFile();
    tempdir.deleteOnExit();
    tmpDir = tempdir.getAbsolutePath();
    //
    // attempt to get resource from jar
    // (should succeed unless testing in IDE)
    InputStream src = null;
    if (TestTargetHistoryTable.class.getClassLoader().getResource(resourceName) != null) {
      src = TestTargetHistoryTable.class.getClassLoader().getResourceAsStream(resourceName);
    }
    //
    // if not found, try to find it relative to the current directory
    //
    if (src == null) {
      src = new FileInputStream(resourceName);
    }
    assertNotNull("Could not locate resource " + resourceName, src);
    try {
      final File destFile = new File(tmpDir, tmpFile);
      destFile.deleteOnExit();
      try (FileOutputStream dest = new FileOutputStream(destFile)) {
        int bytesRead = 0;
        final byte[] buffer = new byte[4096];
        do {
          bytesRead = src.read(buffer);
          if (bytesRead > 0) {
            dest.write(buffer, 0, bytesRead);
          }
        } while (bytesRead == buffer.length);
      }
    } finally {
      src.close();
    }
  }

  /**
   * Deletes a file, if it exists, from the user's temporary directory.
   * 
   * @param tmpName
   *          file name, may not be null
   */
  public static void deleteTmpFile(final String tmpName) throws IOException {
    final String tmpDir = System.getProperty("java.io.tmpdir");
    final File tmpFile = new File(tmpDir, tmpName);
    if (tmpFile.exists()) {
      tmpFile.delete();
    }
  }

  /**
   * @param testName
   */
  protected TestXMLConsumer(final String testName) {
    super(testName);
  }
}
