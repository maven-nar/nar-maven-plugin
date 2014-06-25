package it0014.test;

import it0014.HelloWorldSharedLibJNI;
import org.junit.Assert;
import org.junit.Test;

public class HelloWorldSharedLibJNITest
{
    @Test
    public void testNativeHelloWorldSharedLibJNI()
        throws Exception
    {
        HelloWorldSharedLibJNI app = new HelloWorldSharedLibJNI();

        Assert.assertEquals( "Hello NAR LIB World!", app.sayHello() );
    }
}
