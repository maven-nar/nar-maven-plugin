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
import java.nio.file.Paths;
import java.lang.management.ManagementFactory;
import org.junit.Test;
import static org.junit.Assert.*;

public class FirstNativeTest {
  @Test
  public void loadsOwnAndDependencyLibrariesInFreshJvm() throws Exception {
    assertNull("Each test class must get a fresh JVM", System.getProperty("nar.native.isolation"));
    System.setProperty("nar.native.isolation", "used");
    assertEquals("Hello NAR LIB World!", new NativeLibrary().message());
    assertEquals(35, new it0003.HelloWorldJNI().timesHello(5, 7));
    String os = System.getProperty("os.name");
    if (!os.startsWith("Windows")) {
      String key = os.startsWith("Mac") ? "DYLD_LIBRARY_PATH" : os.equals("AIX") ? "LIBPATH" : "LD_LIBRARY_PATH";
      String path = System.getenv(key);
      assertTrue(path, path.contains("native output"));
      assertTrue(path, path.contains("test dependencies"));
      assertTrue(path, path.endsWith("configured tail"));
    }
    assertTrue(System.getProperty("surefire.test.class.path"), System.getProperty("surefire.test.class.path").contains("custom-native-artifact.jar"));
    Files.write(Paths.get(System.getProperty("basedir"), "target", "FirstNativeTest.executed"),
        ManagementFactory.getRuntimeMXBean().getName().getBytes(StandardCharsets.UTF_8));
  }
}
