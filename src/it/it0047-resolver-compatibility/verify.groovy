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
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

// Repository POMs and archives are test data; the consuming project POM is checked in.
def group = 'com.github.maven-nar.its.resolver'
def remote = new File(basedir, 'remote')
def cache = new File(basedir, 'cache path')
assert cache.mkdirs()
def published = [:]
def write = { File file, byte[] bytes ->
  file.parentFile.mkdirs()
  file.bytes = bytes
  new File(file.path + '.sha1').text = MessageDigest.getInstance('SHA-1').digest(bytes).encodeHex().toString()
}
def publish = { String version, String resolved, String marker ->
  def dir = new File(remote, "${group.replace('.', '/')}/native-lib/${version}")
  def files = [:]
  def pom = "<project><modelVersion>4.0.0</modelVersion><groupId>${group}</groupId><artifactId>native-lib</artifactId><version>${version}</version><packaging>nar</packaging></project>"
  write(new File(dir, "native-lib-${resolved}.pom"), pom.getBytes('UTF-8'))
  [null, 'noarch', 'amd64-Linux-gpp-shared'].each { classifier ->
    def bytes = new ByteArrayOutputStream()
    def jar = new JarOutputStream(bytes)
    if (classifier == null) {
      jar.putNextEntry(new JarEntry("META-INF/nar/${group}/native-lib/nar.properties"))
      jar.write(("nar.noarch=${group}:native-lib:nar:noarch\n" +
          "amd64-Linux-gpp.nar.shared=${group}:native-lib:nar:amd64-Linux-gpp-shared\n").getBytes('UTF-8'))
    } else {
      jar.putNextEntry(new JarEntry(classifier == 'noarch' ? 'include/marker.h' : 'lib/amd64-Linux-gpp/shared/marker.txt'))
      jar.write(marker.getBytes('UTF-8'))
    }
    jar.closeEntry()
    jar.close()
    def file = new File(dir, "native-lib-${resolved}${classifier == null ? '' : '-' + classifier}.nar")
    write(file, bytes.toByteArray())
    files[classifier ?: 'main'] = file
  }
  if (version.endsWith('-SNAPSHOT') && version != resolved) {
    def timestamp = resolved.substring(version.length() - 'SNAPSHOT'.length(), resolved.lastIndexOf('-'))
    def buildNumber = resolved.substring(resolved.lastIndexOf('-') + 1)
    def updated = timestamp.replace('.', '')
    def entries = [null, 'noarch', 'amd64-Linux-gpp-shared'].collect { classifier ->
      "<snapshotVersion><extension>nar</extension>${classifier == null ? '' : '<classifier>' + classifier + '</classifier>'}<value>${resolved}</value><updated>${updated}</updated></snapshotVersion>"
    }.join('') + "<snapshotVersion><extension>pom</extension><value>${resolved}</value><updated>${updated}</updated></snapshotVersion>"
    write(new File(dir, 'maven-metadata.xml'), ("<metadata><groupId>${group}</groupId><artifactId>native-lib</artifactId><version>${version}</version>" +
        "<versioning><snapshot><timestamp>${timestamp}</timestamp><buildNumber>${buildNumber}</buildNumber></snapshot><lastUpdated>${updated}</lastUpdated><snapshotVersions>${entries}</snapshotVersions></versioning></metadata>").getBytes('UTF-8'))
  }
  published[version] = files
}
def pluginVersion = new File(basedir, 'pom.xml').text.find(/<version>([^<]+)<\/version>\s*<extensions>/) { all, value -> value }
assert pluginVersion
String goal = "com.github.maven-nar:nar-maven-plugin:${pluginVersion}:"
def invoker = new DefaultInvoker().setMavenHome(new File(System.getProperty('maven.home')))
def run = { String name, String artifactVersion, List extra, boolean success ->
  // Reuse only the repository cache. ZIP timestamps can be equal between quickly
  // published versions, which would test assembly's incremental-copy policy instead.
  new File(basedir, 'target/nar').deleteDir()
  new File(basedir, 'target/assembled').deleteDir()
  def request = new DefaultInvocationRequest().setBaseDirectory(basedir).setBatchMode(true)
  request.setLocalRepositoryDirectory(cache)
  request.setGoals([goal + 'nar-download-dependencies', goal + 'nar-unpack-dependencies', goal + 'nar-assembly'] + extra)
  def properties = new Properties()
  properties.setProperty('fixture.version', artifactVersion)
  properties.setProperty('maven.plugin.validation', 'verbose')
  request.setProperties(properties)
  def log = new File(basedir, name + '.log')
  log.text = ''
  request.setOutputHandler({ line -> log.append(line + '\n') } as org.apache.maven.shared.invoker.InvocationOutputHandler)
  request.setErrorHandler({ line -> log.append(line + '\n') } as org.apache.maven.shared.invoker.InvocationOutputHandler)
  def result = invoker.execute(request)
  assert result.executionException == null : result.executionException
  assert (result.exitCode == 0) == success : name + ': ' + log.text
  return log.text
}
def verify = { String version, String marker ->
  // Assembly must use the same resolved attachments rather than reconstruct repository paths.
  assert new File(basedir, 'target/assembled/include/marker.h').text == marker
  assert new File(basedir, 'target/assembled/lib/amd64-Linux-gpp/shared/marker.txt').text == marker
  // Base-version extraction paths must agree with NAR's native classpath construction.
  assert new File(basedir, "target/nar/native-lib-${version}-noarch/include/marker.h").text == marker
  assert new File(basedir, "target/nar/native-lib-${version}-amd64-Linux-gpp-shared/lib/amd64-Linux-gpp/shared/marker.txt").text == marker
}
publish('1.0', '1.0', 'release')
def logs = [run('release', '1.0', [], true)]
verify('1.0', 'release')
logs << run('offline', '1.0', ['-o'], true)
verify('1.0', 'release')
publish('2.0-SNAPSHOT', '2.0-20260927.120000-1', 'snapshot-one')
logs << run('snapshot', '2.0-SNAPSHOT', [], true)
verify('2.0-SNAPSHOT', 'snapshot-one')
publish('2.0-SNAPSHOT', '2.0-20260927.120100-2', 'snapshot-two')
logs << run('snapshot-update', '2.0-SNAPSHOT', ['-U'], true)
verify('2.0-SNAPSHOT', 'snapshot-two')
publish('3.0-SNAPSHOT', '3.0-SNAPSHOT', 'installed-snapshot')
published['3.0-SNAPSHOT'].each { classifier, file ->
  def request = new DefaultInvocationRequest().setBaseDirectory(basedir).setBatchMode(true)
  request.setLocalRepositoryDirectory(cache)
  request.setGoals(['org.apache.maven.plugins:maven-install-plugin:3.2.0:install-file'])
  def properties = new Properties()
  properties.putAll([groupId: group, artifactId: 'native-lib', version: '3.0-SNAPSHOT', packaging: 'nar',
      file: file.absolutePath, generatePom: 'true'])
  if (classifier != 'main') { properties.setProperty('classifier', classifier) }
  request.setProperties(properties)
  assert invoker.execute(request).exitCode == 0 : 'Install local snapshot ' + classifier
}
new File(remote, "${group.replace('.', '/')}/native-lib/3.0-SNAPSHOT").deleteDir()
logs << run('installed-snapshot', '3.0-SNAPSHOT', ['-o'], true)
verify('3.0-SNAPSHOT', 'installed-snapshot')
publish('4.0', '4.0', 'missing-attachment')
assert published['4.0']['noarch'].delete()
assert run('missing-attachment', '4.0', [], false).contains('nar not found')
run('missing-offline', 'absent', ['-o'], false)
// Resolver 1.4 in Maven 3.6 ignores these flags; newer hosts exercise the split layout.
cache = new File(basedir, 'split cache path')
assert cache.mkdirs()
logs << run('split', '1.0', ['-Daether.enhancedLocalRepository.split=true',
    '-Daether.enhancedLocalRepository.splitRemoteRepository=true'], true)
verify('1.0', 'release')
// Check the actual loaded artifact in both caches, including the split layout.
def expected = new File(basedir, "plugin-under-test/com/github/maven-nar/nar-maven-plugin/${pluginVersion}/nar-maven-plugin-${pluginVersion}.jar").bytes
['cache path', 'split cache path'].each { name ->
  def jars = []
  new File(basedir, name).eachFileRecurse { file ->
    if (file.name == "nar-maven-plugin-${pluginVersion}.jar") { jars << file }
  }
  assert jars.size() == 1 : jars
  assert Arrays.equals(jars[0].bytes, expected) : 'The fixture loaded a different NAR build'
}
logs.each { text ->
  assert !text.contains('deprecated Maven 2.x compatibility layer') : 'NAR still requires maven-compat'
  assert !text.contains("Parameter 'localRepository' uses deprecated parameter expression") : 'Legacy local repository injection'
}
return true
