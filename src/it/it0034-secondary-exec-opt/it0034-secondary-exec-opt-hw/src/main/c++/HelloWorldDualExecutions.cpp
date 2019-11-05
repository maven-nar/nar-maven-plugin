#include <stdio.h>
#include <cassert>

int main(int argc, char *argv[]) {
	printf("Helloworld\n");

#ifdef _EXECUTION_1_FLAG
   printf("_EXECUTION_1_FLAG is defined\n");
#endif

#ifdef _EXECUTION_2_FLAG
   printf("_EXECUTION_2_FLAG is defined\n");
#endif
   
#ifdef _PARENT_FLAG
   printf("_PARENT_FLAG is defined.\n");
#else
   printf("_PARENT_FLAG should be defined for all executions.\n");
   assert (false);
#endif

	return 0;
}


