package com.github.maven_nar;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class NarMojoTest
    extends AbstractMojoTestCase
{
    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        // required
        super.setUp();
    }

    /** {@inheritDoc} */
    protected void tearDown()
        throws Exception
    {
        // required
        super.tearDown();
    }

    /**
     * @throws Exception if any
     */
    public void testSomething()
        throws Exception
    {
        File pom = getTestFile( "src/test/resources/compile-test/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        NarCompileMojo narMojo = (NarCompileMojo) lookupMojo( "compile", pom );
        assertNotNull( narMojo );
        narMojo.execute();
    }
}
