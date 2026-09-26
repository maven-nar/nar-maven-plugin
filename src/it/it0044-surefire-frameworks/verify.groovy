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
def expected = [
  'junit3': ['JUnit3Test'], 'junit4-old': ['JUnit4Test'], 'junit4': ['JUnit4Test'],
  'pojo': ['PojoTest'], 'testng-old': ['TestNGTest'], 'testng': ['TestNGTest'],
  'testng-groups': ['TestNGFilteringTest'], 'testng-suite': ['TestNGFilteringTest'], 'testng-suite-override': ['TestNGFilteringTest'],
  'testng-suite-property': ['TestNGFilteringTest'],
  'mixed-vintage': ['JupiterTest', 'JUnit4Test'], 'mixed-providers': ['JupiterTest', 'TestNGTest']]
expected.each { framework, classes ->
  classes.each { name ->
    assert new File(basedir, "results/${framework}/${name}.executed").isFile() : "${framework} did not run ${name}"
    String reportName = (framework == 'mixed-providers' && name == 'TestNGTest') || framework in ['testng-suite', 'testng-suite-property'] ? 'TEST-TestSuite.xml' : "TEST-example.${name}.xml"
    File report = new File(basedir, "results/${framework}/${reportName}")
    assert report.isFile()
    def suite = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report).documentElement
    assert suite.getAttribute('tests') == '1'
    assert suite.getAttribute('failures') == '0'
    assert suite.getAttribute('errors') == '0'
    assert suite.getElementsByTagName('testcase').item(0).getAttribute('classname') == 'example.' + name
    if (name == 'TestNGFilteringTest') {
      assert suite.getElementsByTagName('testcase').item(0).getAttribute('name') == 'selected'
    }
  }
}
return true
