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

// Verify loading from the packaged artifacts, not the project's classes directory.
List<File> entries = [new File(localRepositoryPath,
    'org/scijava/native-lib-loader/2.3.0/native-lib-loader-2.3.0.jar')]
entries.add(new File(localRepositoryPath, 'org/slf4j/slf4j-api/1.7.6/slf4j-api-1.7.6.jar'))
new File(basedir, 'target').eachFile { File file ->
  if (file.name.startsWith('it0026-native-lib-loader-2.3.0-1.0-SNAPSHOT') && file.isFile()) {
    entries.add(file)
  }
}
assert entries.size() > 2 : 'No packaged NAR artifacts found'
assert entries.every { it.isFile() }
String classPath = entries.collect { it.absolutePath }.join(File.pathSeparator)
String java = new File(System.getProperty('java.home'), 'bin/java').absolutePath
Process process = new ProcessBuilder(java, '-classpath', classPath, 'it0026.test.Hello')
    .redirectErrorStream(true).start()
println process.inputStream.getText('UTF-8')
assert process.waitFor() == 108 : 'Packaged native library returned an unexpected result'
return true
