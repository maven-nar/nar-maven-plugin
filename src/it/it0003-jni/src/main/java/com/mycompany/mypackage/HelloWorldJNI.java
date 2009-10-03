package com.mycompany.mypackage;

public class HelloWorldJNI {
    static {
        NarSystem.loadLibrary();
    }

    public native String sayHello();

    public static void main( String[] args ) {
        HelloWorldJNI app = new HelloWorldJNI();
        System.out.println( app.sayHello() );
    }
}

