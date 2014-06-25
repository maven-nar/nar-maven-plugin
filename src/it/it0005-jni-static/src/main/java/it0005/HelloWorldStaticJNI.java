package it0005;

public class HelloWorldStaticJNI
{
    static
    {
        NarSystem.loadLibrary();
    }

    public final native String sayHello();

    public static void main( String[] args )
    {
        HelloWorldStaticJNI app = new HelloWorldStaticJNI();
        System.out.println( app.sayHello() );
    }
}
