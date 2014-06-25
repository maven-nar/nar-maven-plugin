package it0025;

import org.junit.Assert;
import org.junit.Test;

public class HelloTest
{
    @Test public final void testTimes()
    {
        Assert.assertEquals(42, Hello.times(3, 14));
    }
}
