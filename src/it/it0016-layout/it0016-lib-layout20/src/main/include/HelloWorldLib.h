#ifndef HelloWorldLib_H
#define HelloWorldLib_H

#ifdef WIN32
__declspec(dllexport) 
#endif
extern char* HelloWorldLib_sayHello();

#endif
