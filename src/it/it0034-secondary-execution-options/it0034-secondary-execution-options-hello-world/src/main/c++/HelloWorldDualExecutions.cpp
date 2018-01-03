#include <stdio.h>
#include <cassert>

int main(int argc, char *argv[]) {
	printf("Helloworld\n");

#ifdef _PARENT_FLAG_PRIMARY
   printf("_PARENT_FLAG_PRIMARY is defined\n");
#endif

#ifdef _PARENT_FLAG_SECONDARY
   printf("_PARENT_FLAG_SECONDARY is defined\n");
#endif

#if defined(_PARENT_FLAG_PRIMARY) && defined(_PARENT_FLAG_SECONDARY)
   printf("Both flags defined, this is in error.\n");
   assert (false);
#endif

	return 0;
}


