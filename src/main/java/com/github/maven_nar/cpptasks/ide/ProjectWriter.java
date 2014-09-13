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
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks.ide;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


import org.xml.sax.SAXException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.TargetInfo;

/**
 * Project writer interface.
 *
 * @author curta
 *
 */
public interface ProjectWriter {
  /**
   *  Write  project definition file.
   * @param baseName File name base, writer may append appropriate extension
   * @param task task
   * @param projectDef project element
   * @param files source and header files
   * @param targets compilation targets
   * @param linkTarget link target
   * @throws IOException if I/O error is encountered
   * @throws SAXException if I/O error during XML serialization
   */
  void writeProject(final File baseName,
                    final CCTask task,
                    final ProjectDef projectDef,
                    final List<File> files,
                    final Map<String, TargetInfo> targets,
                    final TargetInfo linkTarget)
      throws IOException, SAXException;
}
