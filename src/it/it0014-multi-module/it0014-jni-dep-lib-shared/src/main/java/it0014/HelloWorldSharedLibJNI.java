package it0014;

public class HelloWorldSharedLibJNI
{
    static
    {
        NarSystem.loadLibrary();
    }

    public final native String sayHello();

    public static void main( String[] args )
    {
        HelloWorldSharedLibJNI app = new HelloWorldSharedLibJNI();
        System.out.println( app.sayHello() );
    }
}
