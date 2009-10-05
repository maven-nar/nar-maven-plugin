#include <stdio.h>
#include "HelloWorldLib.h"

#include "com_mycompany_mypackage_HelloWorldSharedLibJNI.h"

JNIEXPORT jstring JNICALL Java_com_mycompany_mypackage_HelloWorldSharedLibJNI_sayHello( JNIEnv *env, jobject obj ) {
	jstring value;           /* the return value */

	char buf[80];            /* working buffer (really only need 20 ) */

	sprintf ( buf, "%s", HelloWorldLib_sayHello());

	value = (*env)->NewStringUTF( env, buf );

	return value;
}

