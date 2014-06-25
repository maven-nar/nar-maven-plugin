package it0003.test;

import it0003.HelloWorldJNI;
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

    @Test public final void testNativeTimesWorldJNI()
        throws Exception
    {
        HelloWorldJNI app = new HelloWorldJNI();
	Assert.assertEquals( 35, app.timesHello(5, 7));
    }
}
