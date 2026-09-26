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
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;
import static org.junit.Assert.*;

public class SurefireGoalInvokerTest {
  @Test
  public void suppliedCollectionsRejectDescriptorValuesAndChildren() {
    String[][] parameters = {
        {"includes", "include", "java.util.List", "${surefire.includes}"},
        {"excludes", "exclude", "java.util.List", "${surefire.excludes}"},
        {"additionalClasspathElements", "additionalClasspathElement", "java.lang.String[]",
            "${maven.test.additionalClasspath}"},
        {"suiteXmlFiles", "suiteXmlFile", "java.io.File[]", "${surefire.suiteXmlFiles}"}
    };
    for (String[] entry : parameters) {
      NarSurefireConfiguration supplied = new NarSurefireConfiguration();
      supplied.list(entry[0], entry[1], Arrays.asList("first NAR value", "second NAR value"));
      XmlPlexusConfiguration descriptor = new XmlPlexusConfiguration("configuration");
      XmlPlexusConfiguration defaults = parameter(entry[0], entry[2], entry[3]);
      defaults.setAttribute("default-value", "descriptor fallback");
      defaults.setAttribute("descriptor-only", "must not be inherited");
      defaults.addChild(element(entry[1], "descriptor child"));
      descriptor.addChild(defaults);

      Xpp3Dom actual = SurefireGoalInvoker.mergeConfiguration(supplied.toDom(), descriptor).getChild(entry[0]);
      assertNull(entry[0], actual.getValue());
      assertNull(entry[0], actual.getAttribute("default-value"));
      assertNull(entry[0], actual.getAttribute("descriptor-only"));
      assertEquals(entry[2], actual.getAttribute("implementation"));
      assertEquals(2, actual.getChildCount());
      assertEquals("first NAR value", actual.getChild(0).getValue());
      assertEquals("second NAR value", actual.getChild(1).getValue());
    }
  }

  @Test
  public void suppliedSubtreeAndImplementationArePreserved() {
    Xpp3Dom supplied = new Xpp3Dom("configuration");
    Xpp3Dom properties = new Xpp3Dom("systemProperties");
    properties.setAttribute("implementation", "java.util.Properties");
    properties.setAttribute("supplied-only", "keep");
    Xpp3Dom property = new Xpp3Dom("property");
    Xpp3Dom name = new Xpp3Dom("name");
    name.setValue("nar.property");
    Xpp3Dom value = new Xpp3Dom("value");
    value.setValue("NAR value");
    property.addChild(name);
    property.addChild(value);
    properties.addChild(property);
    supplied.addChild(properties);
    XmlPlexusConfiguration descriptor = new XmlPlexusConfiguration("configuration");
    descriptor.addChild(parameter("systemProperties", "java.util.Map", "${descriptor.properties}"));

    Xpp3Dom actual = SurefireGoalInvoker.mergeConfiguration(supplied, descriptor).getChild("systemProperties");
    assertEquals(properties.toString(), actual.toString());
  }

  @Test
  public void scalarAndEmptyParametersRetainDefaultMerging() {
    NarSurefireConfiguration supplied = new NarSurefireConfiguration();
    supplied.value("reportsDirectory", "/NAR reports");
    supplied.value("argLine", "");
    Xpp3Dom configuration = supplied.toDom();
    configuration.addChild(new Xpp3Dom("includes"));
    XmlPlexusConfiguration descriptor = new XmlPlexusConfiguration("configuration");
    XmlPlexusConfiguration reports = parameter("reportsDirectory", "java.io.File", "${surefire.reportsDirectory}");
    reports.setAttribute("default-value", "${project.build.directory}/surefire-reports");
    descriptor.addChild(reports);
    descriptor.addChild(parameter("argLine", "java.lang.String", "${argLine}"));
    descriptor.addChild(parameter("includes", "java.util.List", "${surefire.includes}"));

    Xpp3Dom actual = SurefireGoalInvoker.mergeConfiguration(configuration, descriptor);
    assertEquals("/NAR reports", actual.getChild("reportsDirectory").getValue());
    assertEquals("java.io.File", actual.getChild("reportsDirectory").getAttribute("implementation"));
    assertEquals("${project.build.directory}/surefire-reports",
        actual.getChild("reportsDirectory").getAttribute("default-value"));
    assertEquals("${argLine}", actual.getChild("argLine").getValue());
    assertEquals("${surefire.includes}", actual.getChild("includes").getValue());
  }

  @Test
  public void omittedParametersKeepCompleteDescriptorDefaults() {
    XmlPlexusConfiguration descriptor = new XmlPlexusConfiguration("configuration");
    XmlPlexusConfiguration project = parameter("project", "org.apache.maven.project.MavenProject", null);
    project.setAttribute("default-value", "${project}");
    descriptor.addChild(project);
    XmlPlexusConfiguration session = parameter("session", "org.apache.maven.execution.MavenSession", null);
    session.setAttribute("default-value", "${session}");
    descriptor.addChild(session);
    descriptor.addChild(parameter("pluginArtifactMap", "java.util.Map", "${plugin.artifactMap}"));
    XmlPlexusConfiguration includes = parameter("includes", "java.util.List", "${surefire.includes}");
    includes.addChild(element("include", "**/*Test.java"));
    descriptor.addChild(includes);

    Xpp3Dom actual = SurefireGoalInvoker.mergeConfiguration(new Xpp3Dom("configuration"), descriptor);
    assertEquals(SurefireGoalInvoker.copy(descriptor).toString(), actual.toString());
  }

  @Test
  public void repeatedMergesDoNotMutateInputsOrShareResults() {
    NarSurefireConfiguration supplied = new NarSurefireConfiguration();
    supplied.list("includes", "include", Collections.singletonList("**/NarTest.java"));
    supplied.list("excludes", "exclude", Collections.singletonList("**/NestedTest.java"));
    Xpp3Dom configuration = supplied.toDom();
    XmlPlexusConfiguration descriptor = new XmlPlexusConfiguration("configuration");
    descriptor.addChild(parameter("includes", "java.util.List", "${surefire.includes}"));
    XmlPlexusConfiguration suite = parameter("suiteXmlFiles", "java.io.File[]", "${surefire.suiteXmlFiles}");
    suite.addChild(element("suiteXmlFile", "default-suite.xml"));
    descriptor.addChild(suite);
    String beforeConfiguration = configuration.toString();
    String beforeDescriptor = SurefireGoalInvoker.copy(descriptor).toString();

    Xpp3Dom first = SurefireGoalInvoker.mergeConfiguration(configuration, descriptor);
    String expected = first.toString();
    first.getChild("includes").getChild(0).setValue("changed include");
    first.getChild("excludes").getChild(0).setValue("changed exclude");
    first.getChild("suiteXmlFiles").getChild(0).setValue("changed suite");
    assertEquals(expected, SurefireGoalInvoker.mergeConfiguration(configuration, descriptor).toString());
    assertEquals(beforeConfiguration, configuration.toString());
    assertEquals(beforeDescriptor, SurefireGoalInvoker.copy(descriptor).toString());
  }

  private static XmlPlexusConfiguration parameter(String name, String implementation, String expression) {
    XmlPlexusConfiguration parameter = element(name, expression);
    parameter.setAttribute("implementation", implementation);
    return parameter;
  }

  private static XmlPlexusConfiguration element(String name, String value) {
    XmlPlexusConfiguration element = new XmlPlexusConfiguration(name);
    element.setValue(value);
    return element;
  }
}
