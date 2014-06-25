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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.PropertyUtils;

public class NarProperties {
	
	private final static String AOL_PROPERTIES = "aol.properties";
	private final static String CUSTOM_AOL_PROPERTY_KEY = "nar.aolProperties";
	private Properties properties;
	private static NarProperties instance;
	
	private NarProperties(MavenProject project) throws MojoFailureException {
		
		Properties defaults = PropertyUtils.loadProperties( NarUtil.class.getResourceAsStream( AOL_PROPERTIES ) );
        if ( defaults == null )
        {
            throw new MojoFailureException( "NAR: Could not load default properties file: '"+AOL_PROPERTIES+"'." );
        }
        
        properties = new Properties(defaults);
        FileInputStream fis = null;
        String customPropertyLocation = null;
        try 
        {
        	if (project != null) {
        		customPropertyLocation = project.getProperties().getProperty(CUSTOM_AOL_PROPERTY_KEY);
        		if (customPropertyLocation == null) {
        		    // Try and read from the system property in case it's specified there
        		    customPropertyLocation = System.getProperties().getProperty(CUSTOM_AOL_PROPERTY_KEY);
        		}
        		fis = new FileInputStream(customPropertyLocation != null ?
        		                          customPropertyLocation : project.getBasedir()+File.separator+AOL_PROPERTIES);
        		properties.load( fis );
        	}
		} 
        catch (FileNotFoundException e) 
        {
			if (customPropertyLocation != null) {
			    // We tried loading from a custom location - so throw the exception
			    throw new MojoFailureException( "NAR: Could not load custom properties file: '"+customPropertyLocation+"'." );
			}
		} 
        catch (IOException e) 
        {
			// ignore (FIXME)
		}
        finally
        {
            try
            {
                if ( fis != null )
                {
                    fis.close();
                }
            }
            catch ( IOException e )
            {
                // ignore
            }
        }

	}
	
	/**
	 * Retrieve the NarProperties
	 * @param project may be null
	 * @return
	 * @throws MojoFailureException
	 */
	public static NarProperties getInstance(MavenProject project) throws MojoFailureException {
		if (instance == null) {
			instance = new NarProperties(project);
		}
		return instance;
	}
	
	/**
	 * Programmatically inject properties (and possibly overwrite existing properties)
	 * @param project the current maven project
	 * @param properties the properties from input stream
	 */
	public static void inject(MavenProject project, InputStream properties) throws MojoFailureException {
		final Properties defaults = PropertyUtils.loadProperties( properties );
		final NarProperties nar = getInstance(project);
		nar.properties.putAll(defaults);
	}
	
	public String getProperty(String key) {
		return properties.getProperty(key);
	}
}
