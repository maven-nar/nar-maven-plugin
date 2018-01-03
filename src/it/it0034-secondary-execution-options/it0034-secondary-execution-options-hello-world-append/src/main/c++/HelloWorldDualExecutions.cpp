#include <stdio.h>
#include <cassert>

int main(int argc, char *argv[]) {
#ifdef _APPEND_FLAG_PRIMARY
   printf("_APPEND_FLAG_PRIMARY is defined\n");
#endif

#ifdef _APPEND_FLAG_SECONDARY
   printf("_APPEND_FLAG_SECONDARY is defined\n");
#endif

#if defined(_APPEND_FLAG_PRIMARY) && defined(_APPEND_FLAG_SECONDARY)
   printf("Both flags defined, this is in error.\n");
   assert (false);
#endif

	return 0;
}


