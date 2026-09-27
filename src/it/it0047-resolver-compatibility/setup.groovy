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
import java.security.MessageDigest

new File(basedir, 'remote').mkdirs()
// Invoker installed the current build here. Never fall back to another cached NAR snapshot.
def version = new File(basedir, 'pom.xml').text.find(/<version>([^<]+)<\/version>\s*<extensions>/) { all, value -> value }
assert version
def path = "com/github/maven-nar/nar-maven-plugin/${version}"
['jar', 'pom'].each { extension ->
  def name = "nar-maven-plugin-${version}.${extension}"
  def source = new File(localRepositoryPath, "${path}/${name}")
  assert source.isFile()
  def target = new File(basedir, "plugin-under-test/${path}/${name}")
  target.parentFile.mkdirs()
  target.bytes = source.bytes
  new File(target.path + '.sha1').text = MessageDigest.getInstance('SHA-1').digest(target.bytes).encodeHex().toString()
}
return true
