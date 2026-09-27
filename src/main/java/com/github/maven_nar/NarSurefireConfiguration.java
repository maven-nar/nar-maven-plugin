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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/** Translation into Surefire's public goal parameters. No provider logic lives here. */
final class NarSurefireConfiguration {
  private final Xpp3Dom configuration = new Xpp3Dom("configuration");

  NarSurefireConfiguration value(String name, Object value) {
    if (value != null) {
      Xpp3Dom child = new Xpp3Dom(name);
      child.setValue(value instanceof File ? ((File) value).getAbsolutePath() : value.toString());
      configuration.addChild(child);
    }
    return this;
  }

  void list(String name, String element, List<?> values) {
    if (values != null && !values.isEmpty()) {
      Xpp3Dom list = new Xpp3Dom(name);
      for (Object value : values) {
        Xpp3Dom child = new Xpp3Dom(element);
        child.setValue(value instanceof File ? ((File) value).getAbsolutePath() : value.toString());
        list.addChild(child);
      }
      configuration.addChild(list);
    }
  }

  void map(String name, Map<String, String> values) {
    if (values != null && !values.isEmpty()) {
      Xpp3Dom map = new Xpp3Dom(name);
      for (Map.Entry<String, String> entry : values.entrySet()) {
        Xpp3Dom child = new Xpp3Dom(entry.getKey());
        child.setValue(entry.getValue());
        map.addChild(child);
      }
      configuration.addChild(map);
    }
  }

  void properties(String name, Properties values) {
    if (values != null && !values.isEmpty()) {
      Xpp3Dom properties = new Xpp3Dom(name);
      for (String key : values.stringPropertyNames()) {
        Xpp3Dom property = new Xpp3Dom("property");
        Xpp3Dom propertyName = new Xpp3Dom("name");
        propertyName.setValue(key);
        Xpp3Dom propertyValue = new Xpp3Dom("value");
        propertyValue.setValue(values.getProperty(key));
        property.addChild(propertyName);
        property.addChild(propertyValue);
        properties.addChild(property);
      }
      configuration.addChild(properties);
    }
  }

  void forks(String legacyMode, String count, Boolean reuse, boolean nativeTests, boolean toolchain)
      throws MojoFailureException {
    String effectiveCount = "1";
    boolean effectiveReuse = true;
    if (count == null) {
      if ("never".equals(legacyMode) || "none".equals(legacyMode)) {
        effectiveCount = "0";
      } else if ("always".equals(legacyMode) || "pertest".equals(legacyMode)) {
        effectiveReuse = false;
      } else if (legacyMode != null && !"once".equals(legacyMode)) {
        throw new MojoFailureException("Invalid forkMode: " + legacyMode);
      }
    } else {
      effectiveCount = count;
    }
    if (reuse != null) {
      effectiveReuse = reuse;
    }
    if (toolchain && isZero(effectiveCount)) {
      effectiveCount = "1";
    }
    if (nativeTests) {
      effectiveCount = "1";
      effectiveReuse = false;
    }
    value("forkCount", effectiveCount).value("reuseForks", effectiveReuse);
  }

  private static boolean isZero(String count) {
    String trimmed = count.trim();
    try {
      return trimmed.endsWith("C")
          ? Double.parseDouble(trimmed.substring(0, trimmed.length() - 1)) == 0d
          : Integer.parseInt(trimmed) == 0;
    } catch (NumberFormatException e) {
      // Let Surefire report invalid modern fork counts using its own validation.
      return false;
    }
  }

  Xpp3Dom toDom() {
    return new Xpp3Dom(configuration);
  }
}
