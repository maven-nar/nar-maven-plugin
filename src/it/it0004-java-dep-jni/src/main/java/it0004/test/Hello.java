package it0004.test;

import it0003.HelloWorldJNI;

public class Hello {
    static
    {
        NarSystem.loadLibrary();
    }

    public native static byte say(HelloWorldJNI app);
}
