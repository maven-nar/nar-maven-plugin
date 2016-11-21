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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.PropertyUtils;

public class NarProperties {

  private final static String AOL_PROPERTIES = "aol.properties";
  private final static String CUSTOM_AOL_PROPERTY_KEY = "nar.aolProperties";
  private static Map<MavenProject,NarProperties> instances = new HashMap<MavenProject,NarProperties>();

  /**
   * Retrieve the NarProperties
   * 
   * @param project
   *          may be null
   * @return
   * @throws MojoFailureException
   */
  public static NarProperties getInstance(final MavenProject project) throws MojoFailureException {
    NarProperties instance = instances.get(project);
    if (instance == null) {
      if (project == null) {
          instance = new NarProperties(project, null, null);
      } else {
        String customPropertyLocation = project.getProperties().getProperty(CUSTOM_AOL_PROPERTY_KEY);
        if (customPropertyLocation == null) {
          // Try and read from the system property in case it's specified there
          customPropertyLocation = System.getProperties().getProperty(CUSTOM_AOL_PROPERTY_KEY);
        }
        File narFile = (customPropertyLocation != null) ? new File(customPropertyLocation) :
            new File(project.getBasedir(), AOL_PROPERTIES);
        if (narFile.exists()) {
          instance = new NarProperties(project, narFile, customPropertyLocation);
        } else {
          // use instance from parent
          instance = getInstance(project.getParent());
        }
      }
      instances.put(project,instance);
    }
    return instance;
  }

  /**
   * Programmatically inject properties (and possibly overwrite existing
   * properties)
   * 
   * @param project
   *          the current maven project
   * @param properties
   *          the properties from input stream
   */
  public static void inject(final MavenProject project, final InputStream properties) throws MojoFailureException {
    final Properties defaults = PropertyUtils.loadProperties(properties);
    final NarProperties nar = getInstance(project);
    nar.properties.putAll(defaults);
  }

  private final Properties properties;

  private NarProperties(final MavenProject project, File narFile, String customPropertyLocation) throws MojoFailureException {

    final Properties defaults = PropertyUtils.loadProperties(NarUtil.class.getResourceAsStream(AOL_PROPERTIES));
    if (defaults == null) {
      throw new MojoFailureException("NAR: Could not load default properties file: '" + AOL_PROPERTIES + "'.");
    }

    this.properties = new Properties(defaults);
    FileInputStream fis = null;
    try {
      if (project != null) {
        fis = new FileInputStream(narFile);
        this.properties.load(fis);
      }
    } catch (final FileNotFoundException e) {
      if (customPropertyLocation != null) {
        // We tried loading from a custom location - so throw the exception
        throw new MojoFailureException("NAR: Could not load custom properties file: '" + customPropertyLocation + "'.");
      }
    } catch (final IOException e) {
      // ignore (FIXME)
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
      } catch (final IOException e) {
        // ignore
      }
    }

  }

  public Collection<String> getKnownAOLs() {
    final Collection<String> result = new LinkedHashSet<>();
    final Pattern pattern = Pattern.compile("([^.]+)\\.([^.]+)\\.([^.]+).*");
    final Enumeration<?> e = this.properties.propertyNames();
    while (e.hasMoreElements()) {
      final Object key = e.nextElement();
      if (key instanceof String) {
        final Matcher matcher = pattern.matcher((String) key);
        if (matcher.matches()) {
          result.add(matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3));
        }
      }
    }
    return result;
  }

  public String getProperty(final String key) {
    return this.properties.getProperty(key);
  }
}
