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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/** Invokes an unchanged released plugin through Maven's normal mojo container. */
final class SurefireGoalInvoker {
  private final BuildPluginManager manager;

  SurefireGoalInvoker(BuildPluginManager manager) {
    this.manager = manager;
  }

  void execute(MavenSession session, MavenProject project, String executionId, Xpp3Dom configuration,
      List<Dependency> dependencies) throws MojoExecutionException, MojoFailureException {
    Plugin plugin = new Plugin();
    plugin.setGroupId("org.apache.maven.plugins");
    plugin.setArtifactId("maven-surefire-plugin");
    plugin.setVersion(version());
    if (dependencies != null) {
      for (Dependency dependency : dependencies) {
        if ("org.apache.maven.surefire".equals(dependency.getGroupId())
            && !plugin.getVersion().equals(dependency.getVersion())) {
          throw new MojoExecutionException("Surefire provider dependencies must use version " + plugin.getVersion());
        }
        plugin.addDependency(dependency.clone());
      }
    }
    try {
      MojoDescriptor descriptor = manager.getMojoDescriptor(plugin, "test", project.getRemotePluginRepositories(),
          session.getRepositorySession());
      MojoExecution execution = new MojoExecution(descriptor, executionId);
      execution.setConfiguration(mergeConfiguration(configuration, descriptor.getMojoConfiguration()));
      manager.executeMojo(session, execution);
    } catch (MojoFailureException | MojoExecutionException e) {
      // Test failures must fail integration-test immediately, including no-test failures.
      throw e;
    } catch (Exception e) {
      throw new MojoExecutionException("Cannot execute " + plugin.getId() + ":test", e);
    }
  }

  private static String version() throws MojoExecutionException {
    try (InputStream stream = SurefireGoalInvoker.class.getResourceAsStream("surefire.properties")) {
      if (stream == null) {
        throw new IOException("Missing packaged Surefire version");
      }
      Properties properties = new Properties();
      properties.load(stream);
      String version = properties.getProperty("version");
      if (version == null || version.contains("${")) {
        throw new IOException("Invalid packaged Surefire version");
      }
      return version;
    } catch (IOException e) {
      throw new MojoExecutionException("Cannot determine the NAR Surefire version", e);
    }
  }

  static Xpp3Dom mergeConfiguration(Xpp3Dom configuration, PlexusConfiguration descriptorConfiguration) {
    Xpp3Dom result = new Xpp3Dom(configuration);
    Xpp3Dom defaults = copy(descriptorConfiguration);
    for (Xpp3Dom parameterDefaults : defaults.getChildren()) {
      Xpp3Dom parameter = result.getChild(parameterDefaults.getName());
      if (parameter == null) {
        result.addChild(parameterDefaults);
      } else if (parameter.getChildCount() > 0) {
        // NAR has already resolved this collection. A descriptor expression would
        // make Plexus use its scalar value instead of the supplied children.
        String implementation = parameterDefaults.getAttribute("implementation");
        if (parameter.getAttribute("implementation") == null && implementation != null) {
          parameter.setAttribute("implementation", implementation);
        }
      } else {
        Xpp3Dom.mergeXpp3Dom(parameter, parameterDefaults);
      }
    }
    return result;
  }

  static Xpp3Dom copy(PlexusConfiguration source) {
    Xpp3Dom result = new Xpp3Dom(source.getName());
    result.setValue(source.getValue(null));
    for (String name : source.getAttributeNames()) {
      result.setAttribute(name, source.getAttribute(name, null));
    }
    for (PlexusConfiguration child : source.getChildren()) {
      result.addChild(copy(child));
    }
    return result;
  }
}
