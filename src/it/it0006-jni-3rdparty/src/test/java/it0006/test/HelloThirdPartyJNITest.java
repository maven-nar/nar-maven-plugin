package it0006.test;

import it0003.HelloWorldJNI;
import org.junit.Assert;
import org.junit.Test;

public class HelloThirdPartyJNITest
{
    @Test public final void testNativeHelloThirdPartyJNI()
        throws Exception
    {
        HelloWorldJNI app = new HelloWorldJNI();

        Assert.assertEquals( "Hello NAR World!", app.sayHello() );
    }
}
