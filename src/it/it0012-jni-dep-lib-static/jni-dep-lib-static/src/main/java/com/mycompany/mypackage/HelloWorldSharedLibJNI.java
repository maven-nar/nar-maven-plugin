package com.mycompany.mypackage;

public class HelloWorldSharedLibJNI {
    static {
        NarSystem.loadLibrary();
    }

    public native String sayHello();

    public static void main( String[] args ) {
        HelloWorldSharedLibJNI app = new HelloWorldSharedLibJNI();
        System.out.println( app.sayHello() );
    }
}

