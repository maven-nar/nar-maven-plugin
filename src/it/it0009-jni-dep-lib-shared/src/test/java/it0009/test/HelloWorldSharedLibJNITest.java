package it0009.test;

import it0009.HelloWorldSharedLibJNI;
import org.junit.Assert;
import org.junit.Test;

public class HelloWorldSharedLibJNITest
{
    @Test public final void testNativeHelloWorldSharedLibJNI()
        throws Exception
    {
        HelloWorldSharedLibJNI app = new HelloWorldSharedLibJNI();

        Assert.assertEquals( "Hello NAR LIB World!", app.sayHello() );
    }
}
