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
package com.github.maven_nar;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

public class NarIntegrationTestSupportTest {
  @Test
  public void nativePathsPrecedeConfiguredTailWithoutQuotingSpaces() {
    Map<String, String> configured = Collections.singletonMap("LD_LIBRARY_PATH", "/configured tail");
    Map<String, String> result = NarIntegrationTestSupport.loaderEnvironment(OS.LINUX,
        Arrays.asList("/project with spaces/jni", "/dependency/shared"), configured,
        Collections.singletonMap("LD_LIBRARY_PATH", "/inherited"), new Properties());
    assertEquals("/project with spaces/jni:/dependency/shared:/configured tail", result.get("LD_LIBRARY_PATH"));
    assertEquals("/configured tail", configured.get("LD_LIBRARY_PATH"));
  }

  @Test
  public void platformLoaderNamesAndInheritedTails() {
    for (String[] platform : new String[][] {{OS.MACOSX, "DYLD_LIBRARY_PATH"}, {OS.AIX, "LIBPATH"},
        {OS.LINUX, "LD_LIBRARY_PATH"}}) {
      assertEquals("/native:/existing", NarIntegrationTestSupport.loaderEnvironment(platform[0],
          Collections.singletonList("/native"), Collections.<String, String>emptyMap(),
          Collections.singletonMap(platform[1], "/existing"), new Properties()).get(platform[1]));
    }
  }

  @Test
  public void emptyConfiguredTailDoesNotAddCurrentDirectory() {
    assertEquals("/native", NarIntegrationTestSupport.loaderEnvironment(OS.LINUX,
        Collections.singletonList("/native"), Collections.singletonMap("LD_LIBRARY_PATH", ""),
        Collections.singletonMap("LD_LIBRARY_PATH", "/ignored"), new Properties()).get("LD_LIBRARY_PATH"));
  }

  @Test
  public void windowsKeysAreCaseInsensitiveAndSystemRootOverrideSurvives() {
    Map<String, String> configured = new LinkedHashMap<>();
    configured.put("PATH", "old");
    configured.put("Path", "configured tail");
    configured.put("systemroot", "custom Windows");
    Map<String, String> result = NarIntegrationTestSupport.loaderEnvironment(OS.WINDOWS,
        Arrays.asList("native path", "dependency path"), configured,
        Collections.singletonMap("SystemRoot", "inherited Windows"), new Properties());
    assertEquals(2, result.size());
    assertEquals("native path;dependency path;configured tail", result.get("PATH"));
    assertEquals("custom Windows", result.get("SystemRoot"));
  }

  @Test
  public void windowsInheritedPathAndRootDefaults() {
    Map<String, String> result = NarIntegrationTestSupport.loaderEnvironment(OS.WINDOWS,
        Collections.singletonList("native path"), Collections.<String, String>emptyMap(),
        Collections.singletonMap("Path", "inherited tail"), new Properties());
    assertEquals("native path;inherited tail", result.get("PATH"));
    assertEquals("C:" + (char) 92 + "Windows", result.get("SystemRoot"));
  }

  @Test
  public void noNativePathsLeavesConfiguredEnvironmentAlone() {
    Map<String, String> configured = Collections.singletonMap("OTHER", "a value");
    assertEquals(configured, NarIntegrationTestSupport.loaderEnvironment(OS.LINUX,
        Collections.<String>emptyList(), configured, Collections.singletonMap("LD_LIBRARY_PATH", "inherited"), new Properties()));
  }

  @Test
  public void legacySystemPropertyFallbackIsUsedOnlyWithoutAnEnvironmentValue() {
    Properties properties = new Properties();
    properties.setProperty("LD_LIBRARY_PATH", "/legacy property");
    properties.setProperty("SystemRoot", "legacy Windows");
    Map<String, String> empty = Collections.emptyMap();
    assertEquals("/native:/legacy property", NarIntegrationTestSupport.loaderEnvironment(OS.LINUX,
        Collections.singletonList("/native"), empty, empty, properties).get("LD_LIBRARY_PATH"));
    assertEquals("/native:/environment", NarIntegrationTestSupport.loaderEnvironment(OS.LINUX,
        Collections.singletonList("/native"), empty, Collections.singletonMap("LD_LIBRARY_PATH", "/environment"),
        properties).get("LD_LIBRARY_PATH"));
    assertEquals("legacy Windows", NarIntegrationTestSupport.loaderEnvironment(OS.WINDOWS,
        Collections.<String>emptyList(), empty, empty, properties).get("SystemRoot"));
  }

}
