#ifdef WIN32
#define DLLEXPORT __declspec(dllexport) 
#else
#define DLLEXPORT
#endif

DLLEXPORT extern void transitiveFunction();
