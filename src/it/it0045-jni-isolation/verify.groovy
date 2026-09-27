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
def processes = ['FirstNativeTest', 'SecondNativeTest'].collect { name ->
  File marker = new File(basedir, "target/${name}.executed")
  assert marker.isFile() : "${name} did not finish JNI calls"
  File report = new File(basedir, "target/surefire-reports/TEST-example.${name}.xml")
  def suite = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report).documentElement
  assert suite.getAttribute('tests') == '1'
  assert suite.getAttribute('failures') == '0'
  assert suite.getAttribute('errors') == '0'
  marker.text
}
assert processes.toSet().size() == 2 : 'Native classes reused a JVM despite required isolation'
return true
