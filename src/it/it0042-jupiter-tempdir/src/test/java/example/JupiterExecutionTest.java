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

import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class JupiterExecutionTest {
  @Test
  void executed() throws Exception {
    Files.write(Paths.get(System.getProperty("basedir"), "target", "jupiter-executed"), new byte[] {1});
  }

  @Test
  @Disabled("The verifier requires this body to remain unexecuted")
  void disabled() throws Exception {
    Files.write(Paths.get(System.getProperty("basedir"), "target", "disabled-executed"), new byte[] {1});
  }
}
