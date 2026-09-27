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
// Each successful execution appends a marker, catching accidental Surefire deduplication.
def collectionCases = ['includes-default', 'includes-explicit', 'excludes-default', 'excludes-explicit', 'classpath-property']
def normal = ['default', 'filtered', 'promoted', 'never', 'pertest', 'ignored-failure', 'ignored-error', 'override-excludes'] + collectionCases
normal.each { name ->
  def expected = name == 'default' ? ['ordinary', 'first', 'second'] : ['first', 'second']
  assert new File(basedir, "results/${name}/executions").readLines('UTF-8') == expected : name
}
['failure', 'error', 'crash'].each { name ->
  assert new File(basedir, "results/${name}/executions").readLines('UTF-8') == ['first'] : 'Failure did not stop integration-test immediately'
}
['no-tests', 'empty-ok', 'explicit-empty', 'explicit-empty-ok', 'excluded', 'missing', 'missing-fail',
 'invalid-jvm', 'skip-nar', 'skip-nar-tests', 'skip-nar-exec', 'skip-all', 'skip-native-plugin', 'dry-run'].each { name ->
  assert !new File(basedir, "results/${name}/executions").exists() : name
}
def reportCounts = ['default': [3, 0, 0, 1], 'filtered': [1, 0, 0, 0], 'failure': [3, 1, 0, 1],
 'ignored-failure': [3, 1, 0, 1], 'error': [3, 0, 1, 1], 'ignored-error': [3, 0, 1, 1]]
// Counts and markers together prevent property collisions from passing with no tests.
collectionCases.each { reportCounts[it] = [3, 0, 0, 1] }
reportCounts.each { name, counts ->
  File report = new File(basedir, "results/${name}/reports/TEST-example.ContractTest.xml")
  assert report.isFile()
  def suite = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report).documentElement
  assert ['tests', 'failures', 'errors', 'skipped'].collect { suite.getAttribute(it).toInteger() } == counts : name
  assert new File(report.parentFile, 'example.ContractTest-output.txt').text.contains('forwarded output')
}
assert new File(basedir, 'results/default/ordinary-reports/TEST-example.ContractTest.xml').isFile()
assert new File(basedir, 'build.log').text.contains('The forked VM terminated without properly saying goodbye')
assert new File(basedir, 'build.log').text.contains('Given path to java executor does not exist')
return true
