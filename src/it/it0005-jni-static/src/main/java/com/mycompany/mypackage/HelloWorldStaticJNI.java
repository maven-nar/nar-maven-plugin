package com.mycompany.mypackage;

public class HelloWorldStaticJNI {
    static {
        NarSystem.loadLibrary();
    }

    public native String sayHello();

    public static void main( String[] args ) {
        HelloWorldStaticJNI app = new HelloWorldStaticJNI();
        System.out.println( app.sayHello() );
    }
}

