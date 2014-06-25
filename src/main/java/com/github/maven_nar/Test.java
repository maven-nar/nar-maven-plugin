/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;

/**
 * Sets up a test to create
 * 
 * @author Mark Donszelmann
 */
public class Test
    implements Executable
{

    /**
     * Name of the test to create
     * 
     * @required
     * @parameter default-value=""
     */
    private String name = null;

    /**
     * Type of linking used for this test Possible choices are: "shared" or "static". Defaults to "shared".
     * 
     * @parameter default-value=""
     */
    private String link = Library.SHARED;

    /**
     * When true run this test. Defaults to true;
     * 
     * @parameter expresssion=""
     */
    private boolean run = true;

    /**
     * Arguments to be used for running this test. Defaults to empty list. This option is only used if run=true.
     * 
     * @parameter default-value=""
     */
    private List/* <String> */args = new ArrayList();

    public final String getName()
        throws MojoFailureException
    {
        if ( name == null )
        {
            throw new MojoFailureException( "NAR: Please specify <Name> as part of <Test>" );
        }
        return name;
    }

    public final String getLink()
    {
        return link;
    }

    public final boolean shouldRun()
    {
        return run;
    }

    public final List/* <String> */getArgs()
    {
        return args;
    }
}
