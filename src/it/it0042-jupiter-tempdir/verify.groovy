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
// A successful Maven exit alone cannot distinguish execution from zero discovery.
assert new File(basedir, 'target/jupiter-executed').isFile() : 'Jupiter did not execute the plain sentinel'
assert !new File(basedir, 'target/disabled-executed').exists() : '@Disabled was ignored'
assert !new File(basedir, 'target/ordinary-reports').exists() : 'Ordinary Surefire ran the tests'
def expected = ['JupiterExecutionTest': ['executed', 'disabled'], 'TempDirTest': ['field', 'parameter']]
expected.each { name, methods ->
  File report = new File(basedir, "target/surefire-reports/TEST-example.${name}.xml")
  assert report.isFile() : "Missing report ${report}"
  def suite = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report).documentElement
  assert suite.getAttribute('tests') == '2'
  assert suite.getAttribute('failures') == '0'
  assert suite.getAttribute('errors') == '0'
  assert suite.getAttribute('skipped') == (name == 'JupiterExecutionTest' ? '1' : '0')
  assert (0..<suite.getElementsByTagName('testcase').length).collect { suite.getElementsByTagName('testcase').item(it).getAttribute('name').split('\\(')[0] }.sort() == methods.sort()
}
['field', 'parameter'].each { name ->
  File marker = new File(basedir, "target/${name}-tempdir")
  assert marker.isFile() : "@TempDir ${name} test did not finish"
  assert !new File(marker.getText('UTF-8')).exists() : '@TempDir cleanup failed'
}
return true
