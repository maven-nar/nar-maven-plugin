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
// There are no native source declarations in the consumer; all headers must
// therefore come from the dependency's compiled classes.
File headers = new File(basedir, 'consumer/target/nar/javah-include')
assert headers.list().toList().sort() == [
  'dependency_Container_Api.h',
  'dependency_Constants.h',
  'dependency_GenericApi.h',
  'dependency_Payload.h',
  'dependency_OwnerApi.h'
].sort()
// Base's constructor type parameter java must not shadow qualified names in the generated source.
String api = new File(headers, 'dependency_Container_Api.h').text
assert api.contains('Java_dependency_Container_00024Api_call')
assert api.contains('jthrowable, jintArray')
// Covariant return checking needs the dependency API's nested Marker interface.
assert api.contains('Java_dependency_Container_00024Api_self')
// Base<String> and Text must agree on Supplier<String>; inherited get() stays non-native.
assert !api.contains('Java_dependency_Container_00024Api_get')
// The enum's get() implementation lives in its constant-specific class body.
// Generating its header requires reconstructing the inherited Supplier<String> contract.
String generic = new File(headers, 'dependency_GenericApi.h').text
assert generic.contains('Java_dependency_GenericApi_call')
// The synthesized contract implementation must remain non-native.
assert !generic.contains('Java_dependency_GenericApi_get')
// Ordered<GenericApi> must use Enum's inherited comparison, without a new native method.
assert !generic.contains('Java_dependency_GenericApi_compareTo')
// Contract's generic override resolves the abstract/default conflict without a JNI entry.
assert !generic.contains('Java_dependency_GenericApi_value')
// The two items() contracts need the narrower List<String> return without a JNI entry.
assert !generic.contains('Java_dependency_GenericApi_items')
// TextList<?> carries CharSequence's bound through Collection; its implementation stays non-native.
assert !generic.contains('Java_dependency_GenericApi_boundedItems')
// Both classes are generated together; Bound<Payload> requires Comparable<Payload>.
// Payload requires selecting the callable null/int constructor before compilation.
String payload = new File(headers, 'dependency_Payload.h').text
assert payload.contains('Java_dependency_Payload_call')
assert !payload.contains('Java_dependency_Payload_compareTo')
// Owner wildcard capture must expose the generic overload and reject the ambiguous call.
assert new File(headers, 'dependency_OwnerApi.h').text.contains('Java_dependency_OwnerApi_call')
String constants = new File(headers, 'dependency_Constants.h').text
assert constants.contains('1234567890123')
assert !constants.contains('__nar_header')
assert new File(basedir, 'consumer/target/nar/javac-headers/javac.args').isFile()
assert !new File(basedir, 'consumer/src/main/java').exists()

// Constructor selection must not depend on failed compiler attempts.
assert !new File(basedir, 'consumer/target/nar/javac-headers/javac.log').text.contains('Compilation attempt 2:')
return true
