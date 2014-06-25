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
