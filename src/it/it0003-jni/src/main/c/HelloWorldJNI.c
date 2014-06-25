
#include <stdio.h>
#include "it0003_HelloWorldJNI.h"

#ifdef TESTOPT
#error "TESTOPT is set!"
#endif

JNIEXPORT jstring JNICALL Java_it0003_HelloWorldJNI_sayHello( JNIEnv *env, jobject obj ) {
	jstring value;           /* the return value */

	char buf[40];            /* working buffer (really only need 20 ) */


	sprintf ( buf, "%s", "Hello NAR World!" );

	value = (*env)->NewStringUTF( env, buf );

	return value;
}

JNIEXPORT jint JNICALL Java_it0003_HelloWorldJNI_timesHello
  (JNIEnv *env, jobject obj, jint x, jint y) {
	return x * y;
}

