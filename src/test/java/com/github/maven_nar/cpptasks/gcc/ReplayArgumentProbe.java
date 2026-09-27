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
package com.github.maven_nar.cpptasks.gcc;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * A standalone linker probe that needs only the JDK when staged in a separate
 * directory.
 */
public class ReplayArgumentProbe {
  public static String encodedArguments(final List<String> values) throws Exception {
    final StringBuilder result = new StringBuilder();
    for (final String value : values)
      result.append(URLEncoder.encode(value, "UTF-8")).append('\n');
    return result.toString();
  }

  public static void main(final String[] args) throws Exception {
    final List<String> values = Arrays.asList(args);
    Files.write(new File(args[values.indexOf("-o") + 1]).toPath(),
        encodedArguments(values.subList(0, values.indexOf("END_ARGUMENTS"))).getBytes(StandardCharsets.UTF_8));
    for (final String arg : args) {
      if (arg.startsWith("-Map="))
        Files.write(new File(arg.substring(5)).toPath(), "map".getBytes(StandardCharsets.UTF_8));
    }
  }
}
