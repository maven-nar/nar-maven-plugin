import junit.framework.*;

import com.mycompany.mypackage.HelloWorldJNI;

public class HelloWorldJNITest extends TestCase {

    public HelloWorldJNITest(String name) {
      super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNativeHelloWorldJNI() throws Exception {
        HelloWorldJNI app = new HelloWorldJNI();
        
        this.assertEquals( "Hello NAR World!", app.sayHello() );
    }
}

