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
package example;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TempDirTest {
  @TempDir
  Path directory;

  @Test
  void field() throws Exception {
    exercise(directory, "field");
  }

  @Test
  void parameter(@TempDir Path parameterDirectory) throws Exception {
    exercise(parameterDirectory, "parameter");
  }

  private static void exercise(Path directory, String name) throws Exception {
    assertNotNull(directory, "Jupiter must inject @TempDir");
    Path file = directory.resolve("content.txt");
    Files.write(file, "Jupiter".getBytes(StandardCharsets.UTF_8));
    assertEquals("Jupiter", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    Files.write(Paths.get(System.getProperty("basedir"), "target", name + "-tempdir"),
        directory.toString().getBytes(StandardCharsets.UTF_8));
  }
}
