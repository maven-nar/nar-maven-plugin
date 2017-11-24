#include <stdio.h>
#include "libtest1.h"
#include "libtest2.h"


int main(int argc, char *argv[]) {
	printf("Call test1\n");
	test1();
	printf("----------\nCall test2\n");
	test2();
	printf("----------\n");
	return 0;
}
