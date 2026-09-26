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

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;
import static org.junit.Assert.*;

public class NarSurefireConfigurationTest {
  @Test
  public void legacyForkAliases() throws Exception {
    assertForks("never", null, null, false, false, "0", "true");
    assertForks("none", null, null, false, false, "0", "true");
    assertForks("once", null, null, false, false, "1", "true");
    assertForks("always", null, null, false, false, "1", "false");
    assertForks("pertest", null, null, false, false, "1", "false");
  }

  @Test
  public void nativeIsolationOverridesExplicitForkRequests() throws Exception {
    assertForks("never", "0", true, true, false, "1", "false");
    assertForks("once", "2C", true, true, false, "1", "false");
  }

  @Test
  public void modernControlsAndToolchainPromotion() throws Exception {
    assertForks("once", "2C", false, false, false, "2C", "false");
    assertForks("never", null, null, false, true, "1", "true");
  }

  @Test
  public void zeroCoreMultiplierStillForksForSelectedToolchain() throws Exception {
    assertForks("once", "0C", true, false, true, "1", "true");
    assertForks("once", "0.0C", true, false, true, "1", "true");
    assertForks("once", "00", true, false, true, "1", "true");
  }

  @Test(expected = MojoFailureException.class)
  public void invalidLegacyModeIsRejected() throws Exception {
    new NarSurefireConfiguration().forks("typo", null, null, false, false);
  }

  @Test
  public void descriptorDefaultsAreCopiedIncludingAttributesWithoutMutation() {
    XmlPlexusConfiguration source = new XmlPlexusConfiguration("configuration");
    XmlPlexusConfiguration parameter = new XmlPlexusConfiguration("reportsDirectory");
    parameter.setAttribute("default-value", "${project.build.directory}/surefire-reports");
    parameter.setAttribute("implementation", "java.io.File");
    parameter.setValue("${surefire.reportsDirectory}");
    source.addChild(parameter);
    Xpp3Dom copied = SurefireGoalInvoker.copy(source);
    assertEquals("java.io.File", copied.getChild("reportsDirectory").getAttribute("implementation"));
    NarSurefireConfiguration config = new NarSurefireConfiguration();
    config.value("reportsDirectory", "/NAR reports");
    Xpp3Dom merged = SurefireGoalInvoker.mergeConfiguration(config.toDom(), source);
    assertEquals("/NAR reports", merged.getChild("reportsDirectory").getValue());
    assertEquals("${surefire.reportsDirectory}", parameter.getValue());
    assertEquals("${project.build.directory}/surefire-reports", parameter.getAttribute("default-value"));
  }

  private void assertForks(String mode, String count, Boolean reuse, boolean nativeTests, boolean toolchain,
      String expectedCount, String expectedReuse) throws Exception {
    NarSurefireConfiguration config = new NarSurefireConfiguration();
    config.forks(mode, count, reuse, nativeTests, toolchain);
    assertEquals(expectedCount, config.toDom().getChild("forkCount").getValue());
    assertEquals(expectedReuse, config.toDom().getChild("reuseForks").getValue());
  }
}
