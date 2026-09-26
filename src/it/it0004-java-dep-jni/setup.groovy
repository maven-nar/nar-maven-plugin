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
/*
 * Simulate multiple NAR artifacts in the same repository. The plugin must choose
 * the correct artifact by AOL rather than the latest SNAPSHOT timestamp.
 */

import java.text.DateFormat
import java.text.SimpleDateFormat

File repoDir = new File(localRepositoryPath, 'com/github/maven-nar/its/nar/it0003-jni/1.0-SNAPSHOT')
File[] artifacts = repoDir.listFiles({ File dir, String name ->
  name.startsWith('it0003-jni-') && name.contains('SNAPSHOT') && name.endsWith('jni.nar')
} as FilenameFilter)

// Expect only one it0003-jni SNAPSHOT NAR.
if (artifacts.length == 1) {
  Calendar now = Calendar.getInstance()
  Calendar tenMinsago = (Calendar) now.clone()
  tenMinsago.add(Calendar.MINUTE, -10)
  DateFormat formatter = new SimpleDateFormat('yyyyMMdd.HHmmss')

  // Create the fake artifact.
  File fakeArtifact = new File(repoDir,
      artifacts[0].getName().replace('SNAPSHOT', formatter.format(now.getTime()) + '-1'))
  fakeArtifact.createNewFile()
}

return true
