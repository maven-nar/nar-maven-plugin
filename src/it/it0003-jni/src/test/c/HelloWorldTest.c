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
