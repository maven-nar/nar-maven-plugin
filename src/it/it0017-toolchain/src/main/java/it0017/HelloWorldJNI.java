package it0017;

public class HelloWorldJNI
{
    static
    {
        NarSystem.loadLibrary();
    }

    public final native String sayHello();

    public static void main( String[] args )
    {
        HelloWorldJNI app = new HelloWorldJNI();
        System.out.println( app.sayHello() );
    }
}
