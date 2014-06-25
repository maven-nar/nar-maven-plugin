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
#include <stdio.h>
#include <string.h>

#ifdef TESTOPT
char *testFlag = TESTOPT;
#else
char *testFlag = "testOption not set";
#endif

int main( int argc, const char* argv[] )
{
    printf( "\nHello World C test:" );
    if (strcmp(testFlag, "testOption not set") == 0) {
        printf( "\nTest compiler option not set\n\n" );
        return 1;
    } else {
        printf( "\nTest compiler option set: %s\n\n", testFlag );
        return 0;
    }
}
