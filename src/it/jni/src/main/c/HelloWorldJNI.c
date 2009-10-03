#include <stdio.h>
#include "com_mycompany_mypackage_HelloWorldJNI.h"

JNIEXPORT jstring JNICALL Java_com_mycompany_mypackage_HelloWorldJNI_sayHello( JNIEnv *env, jobject obj ) {
	jstring value;           /* the return value */

	char buf[40];            /* working buffer (really only need 20 ) */


	sprintf ( buf, "%s", "Hello NAR World!" );

	value = (*env)->NewStringUTF( env, buf );

	return value;
}

