import junit.framework.*;

import com.mycompany.mypackage.HelloWorldJNI;

public class HelloWorldJavaDepJNITest extends TestCase {

    public HelloWorldJavaDepJNITest(String name) {
      super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNativeHelloWorldJavaDepJNI() throws Exception {
        HelloWorldJNI app = new HelloWorldJNI();
        
        this.assertEquals( "Hello NAR World!", app.sayHello() );
    }
}

