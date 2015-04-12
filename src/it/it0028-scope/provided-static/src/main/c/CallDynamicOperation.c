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
#include "CallDynamicOperation.h"

#include <stdio.h>

#ifdef WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#endif

#ifdef WIN32
const char *w32error(int i_code, TCHAR *msgBuf, unsigned int size) {
	DWORD res = FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM, /* dwFlags */
	NULL, /* lpSource */
	i_code, /* dwMessageId */
	MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), /* dwLanguageId */
	(LPTSTR) msgBuf, /* lpBuffer */
	size, /* nSize */
	NULL /* Arguments */);

	if (!res && size >= 50) {
		sprintf(msgBuf, "Error %d in FormatMessage for the code %d\n", (int) res, i_code);
	}

	return msgBuf;
}
#endif

long CallDynamicOperation(const char *libDynamicOperationPath, long n1, long n2) {
	long result;

	typedef long (*DynamicOperation_type)(long, long);
	DynamicOperation_type DynamicOperation;

#ifdef WIN32

	HINSTANCE library = NULL;
	TCHAR msgBuf[255];

	library = LoadLibrary(libDynamicOperationPath);
	if (library == NULL) {
		printf("CallDynamicOperation: LoadLibrary failed : %s\n", w32error(GetLastError(), msgBuf, 255));
		return 0;
	}

	DynamicOperation = (DynamicOperation_type)GetProcAddress((HMODULE)library, "DynamicOperation");
	if (DynamicOperation == NULL) {
		printf("CallDynamicOperation: GetProcAddress failed : %s\n", w32error(GetLastError(), msgBuf, 255));
		return 0;
	}

	result = (*DynamicOperation)(n1, n2);

	FreeLibrary(library);

#else

	void *handle;
	char *error;

	handle = dlopen(libDynamicOperationPath, RTLD_LAZY);
	if (!handle) {
		printf("CallDynamicOperation: dlopen failed : %s\n", dlerror());
		return 0;
	}

	/* Clear any existing error */
	dlerror();

	DynamicOperation = (DynamicOperation_type)dlsym(handle, "DynamicOperation");
	if ((error = dlerror()) != 0) {
		printf("CallDynamicOperation: dlsym failed : %s\n", error);
		return 0;
	}

	result = (*DynamicOperation)(n1, n2);

	dlclose(handle);

#endif

	return result;
}

