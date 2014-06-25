package it0025;

import it0003.HelloWorldJNI;

public class Hello
{
    public static int times(int x, int y)
    {
        return new HelloWorldJNI().timesHello(x, y);
    }
}
