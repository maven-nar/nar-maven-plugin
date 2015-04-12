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
#include "Operations.h"

#include <CallDynamicOperation.h>
#include <SystemOperation.h>

#include <string.h>

long Operation1(long n1, long n2, long n3) {
	long result;

	/* 
	 * CallDynamicOperation take in first parameter the file name of the library to load.
	 * The directory containing the library file is added to the path by nar-maven-plugin
	 * test goal, so we only need the library name (without its path) to load it
	 */
#ifdef WIN32
	result = CallDynamicOperation("it0028-scope-runtime-shared-1.0-SNAPSHOT.dll", n1, n2);
#else
	result = CallDynamicOperation("libit0028-scope-runtime-shared-1.0-SNAPSHOT.so", n1, n2);
#endif

	result = SystemOperation(result, n3);

	return result;
}
