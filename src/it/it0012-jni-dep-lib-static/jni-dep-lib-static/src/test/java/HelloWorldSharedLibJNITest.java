import junit.framework.*;

import com.mycompany.mypackage.HelloWorldSharedLibJNI;

public class HelloWorldSharedLibJNITest extends TestCase {

    public HelloWorldSharedLibJNITest(String name) {
      super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNativeHelloWorldSharedLibJNI() throws Exception {
        HelloWorldSharedLibJNI app = new HelloWorldSharedLibJNI();
        
        this.assertEquals( "Hello NAR LIB World!", app.sayHello() );
    }
}

