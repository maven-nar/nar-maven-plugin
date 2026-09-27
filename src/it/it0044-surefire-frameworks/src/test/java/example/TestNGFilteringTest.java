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
public class TestNGFilteringTest {
  @org.testng.annotations.Test(groups = "selected")
  public void selected() throws Exception {
    java.nio.file.Path file = java.nio.file.Paths.get(System.getProperty("basedir"), "results",
        System.getProperty("framework"), "TestNGFilteringTest.executed");
    java.nio.file.Files.createDirectories(file.getParent());
    java.nio.file.Files.write(file, new byte[] {1});
  }
  @org.testng.annotations.Test(groups = "blocked")
  public void excluded() { throw new AssertionError("Excluded TestNG group/method executed"); }
}
