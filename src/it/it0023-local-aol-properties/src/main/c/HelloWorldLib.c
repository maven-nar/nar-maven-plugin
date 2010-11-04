#include <stdio.h>
#include "HelloWorldLib.h"

#ifdef LOCAL_AOL
char* HelloWorldLib_sayHello() {
	return "Hello NAR LIB World!";
}
#endif

