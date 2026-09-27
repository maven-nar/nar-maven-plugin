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
['direct', 'context', 'explicit', 'zero-core-count'].each { mode ->
  File marker = new File(basedir, "results/${mode}/jdk.executed")
  assert marker.isFile() : "${mode} did not execute"
  if (mode != 'explicit') { assert marker.text == '1.8' }
  def report = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(new File(basedir, "results/${mode}/TEST-example.SelectedJdkTest.xml")).documentElement
  assert report.getAttribute('tests') == '1'
  assert report.getAttribute('errors') == '0'
  assert report.getAttribute('failures') == '0'
}
return true
