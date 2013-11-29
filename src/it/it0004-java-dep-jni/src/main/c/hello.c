#include "it0004_test_Hello.h"

JNIEXPORT jbyte JNICALL Java_it0004_test_Hello_say
  (JNIEnv *env, jclass clazz, jobject object)
{
	return (jbyte)13;
}
