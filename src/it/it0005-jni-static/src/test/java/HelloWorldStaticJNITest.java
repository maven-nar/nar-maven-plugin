import junit.framework.*;

import com.mycompany.mypackage.HelloWorldStaticJNI;

public class HelloWorldStaticJNITest extends TestCase {

    public HelloWorldStaticJNITest(String name) {
      super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNativeHelloWorldJNI() throws Exception {
        HelloWorldStaticJNI app = new HelloWorldStaticJNI();
        
        this.assertEquals( "Hello Static NAR World!", app.sayHello() );
    }
}

