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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;

public class ContractTest {
  @Test
  public void forwarding() throws Exception {
    String id = System.getProperty("nar.integrationTest.executionId", "ordinary");
    String name = System.getProperty("contract.case");
    Path results = Paths.get(System.getProperty("basedir"), "results", name);
    Files.createDirectories(results);
    Files.write(results.resolve("executions"), (id + "\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    if ("ordinary".equals(id)) {
      return;
    }
    assertEquals("legacy value", System.getProperty("legacy"));
    assertEquals("file value", System.getProperty("fileOnly"));
    assertEquals("variables", System.getProperty("precedence"));
    assertTrue(new File(System.getProperty("localRepository")).isDirectory());
    assertEquals("working directory", new File(System.getProperty("user.dir")).getName());
    assertNotNull(getClass().getResource("/extra.txt"));
    if (!"never".equals(name)) {
      assertEquals("environment with spaces", System.getenv("NAR_CONTRACT_ENV"));
      assertEquals("argument with spaces", System.getProperty("contract.arg"));
      assertEquals(new File(System.getProperty("user.dir")).getCanonicalFile(), new File("").getCanonicalFile());
    }
    if (Boolean.getBoolean("contract.promote") || "never".equals(name)) {
      assertEquals("cli value", System.getProperty("unrelated.cli"));
    } else {
      assertNull(System.getProperty("unrelated.cli"));
    }
    System.out.println("forwarded output " + id);
    if (Boolean.getBoolean("contract.crash")) {
      System.exit(13);
    }
    assertFalse("intentional assertion failure", Boolean.getBoolean("contract.fail"));
    if (Boolean.getBoolean("contract.error")) {
      throw new IllegalStateException("intentional execution error");
    }
  }

  @Test
  public void anotherMethod() {
    assertTrue(true);
  }

  @Ignore("skip contract")
  @Test
  public void skipped() {
    fail("Ignored method executed");
  }
}
