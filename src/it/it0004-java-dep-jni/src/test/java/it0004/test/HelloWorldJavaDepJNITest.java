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
package it0004.test;

import it0003.HelloWorldJNI;
import org.junit.Assert;
import org.junit.Test;

public class HelloWorldJavaDepJNITest
{
    @Test public final void testNativeHelloWorldJavaDepJNI()
        throws Exception
    {
        HelloWorldJNI app = new HelloWorldJNI();

        Assert.assertEquals( "Hello NAR World!", app.sayHello() );
    }

    @Test public final void testNativeMethod()
    {
        Assert.assertEquals(13, Hello.say(null));
    }
}
