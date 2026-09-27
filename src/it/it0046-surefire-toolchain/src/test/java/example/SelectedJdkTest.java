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
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public class SelectedJdkTest {
  @Test public void selectedJdkActuallyExecutes() throws Exception {
    String actual = System.getProperty("java.specification.version");
    assertEquals(System.getProperty("expected.version", "1.8"), actual);
    Path marker = Paths.get(System.getProperty("basedir"), "results", System.getProperty("selected.mode"), "jdk.executed");
    Files.createDirectories(marker.getParent());
    Files.write(marker, actual.getBytes(StandardCharsets.UTF_8));
  }
}
