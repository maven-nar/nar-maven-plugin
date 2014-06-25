package it0005.test;

import it0005.HelloWorldStaticJNI;
import org.junit.Assert;
import org.junit.Test;

public class HelloWorldStaticJNITest
{
    @Test public final void testNativeHelloWorldJNI()
        throws Exception
    {
        HelloWorldStaticJNI app = new HelloWorldStaticJNI();

        Assert.assertEquals( "Hello Static NAR World!", app.sayHello() );
    }
}
