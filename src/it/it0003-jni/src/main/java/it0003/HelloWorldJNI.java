package it0003;

public class HelloWorldJNI
{
    static
    {
        NarSystem.loadLibrary();
    }

    public final native String sayHello();
    public final native int timesHello(int x, int y);

    public static void main( String[] args )
    {
        HelloWorldJNI app = new HelloWorldJNI();
        System.out.println( app.sayHello() );

	System.out.println( app.timesHello(5, 7) );
    }
}
