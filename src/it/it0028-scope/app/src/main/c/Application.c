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
#include <Operations.h>
#include <stdio.h>

int main(int argc, char *argv[]) {
	long n1, n2, n3, result, value;

	n1 = 2;
	n2 = 6;
	n3 = 3;
	value = 4;

	result = Operation1(n1, n2, n3);

	printf("Operation1 : %ld\n", result);

	/* If the dynamic library loading succeeds the result of Operation1 will be 4 */
	if(result == value) {
		return 0;
	} else {
		return 1;
	}
}
