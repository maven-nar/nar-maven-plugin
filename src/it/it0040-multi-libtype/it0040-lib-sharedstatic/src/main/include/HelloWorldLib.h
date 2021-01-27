/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
#ifndef HelloWorldLib_H
#define HelloWorldLib_H

#ifdef WIN32
#ifdef LIB_EXPORTS
#pragma message( "Export" )
#define LIB_EXPORT __declspec(dllexport)
#else
#ifdef LIB_IMPORTS
#pragma message( "Import" )
#define LIB_EXPORT __declspec(dllimport)
#else
#pragma message( "Static" )
#define LIB_EXPORT
#endif
#endif
#else
#define LIB_EXPORT
#endif


LIB_EXPORT extern char* HelloWorldLib_sayHello();

#endif
