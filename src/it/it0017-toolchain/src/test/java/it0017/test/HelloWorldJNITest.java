package it0017.test;

import it0017.HelloWorldJNI;
import org.junit.Assert;
import org.junit.Test;

public class HelloWorldJNITest
{
    @Test public final void testNativeHelloWorldJNI()
        throws Exception
    {
        HelloWorldJNI app = new HelloWorldJNI();
        Assert.assertEquals( "Hello NAR World!", app.sayHello() );
    }
}
