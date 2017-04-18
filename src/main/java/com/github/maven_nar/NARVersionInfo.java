package com.github.maven_nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.github.maven_nar.cpptasks.VersionInfo;
import org.apache.tools.ant.BuildException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;

/**
 * Keeps info on a system library
 * 
 * @author Mark Donszelmann
 */
public class NARVersionInfo
{
    /**
     * Name of the system library
     * 
     * @parameter expression=""
     * @required
     */
    //private String name;

    /**
     * Type of linking for this system library
     * 
     * @parameter expression="" default-value="shared"
     * @required
     */
    //private String type = Library.SHARED;
    
      /**
     * file version.
     *
     */
    private String fileVersion;
    /**
     * Product version.
     *
     */
    private String productVersion;
    /**
     * file language.
     *
     */
    private String language;

    /**
     * comments.
     *
     */
    private String fileComments;
    /**
     * Company name.
     *
     */
    private String companyName;
    /**
     * Description.
     *
     */
    private String fileDescription;
    /**
     * internal name.
     */
    private String internalName;
    /**
     * legal copyright.
     *
     */
    private String legalCopyright;
    /**
     * legal trademark.
     *
     */
    private String legalTrademarks;
    /**
     * original filename.
     *
     */
    private String originalFilename;
    /**
     * private build.
     *
     */
    private String privateBuild;
    /**
     * product name.
     *
     */
    private String productName;
    /**
     * Special build
     */
    private String specialBuild;
    /**
     * compatibility version
     *
     */
    private String compatibilityVersion;

    /**
     * prerease build.
     *
     */
    private Boolean prerelease;

    /**
     * prerease build.
     *
     */
    private Boolean patched;
    
		
		public NARVersionInfo()
		{
			
		}
		
    public final VersionInfo getVersionInfo( Project antProject )
        throws MojoFailureException
    {
        /*if ( name == null )
        {
            throw new MojoFailureException( "NAR: Please specify <Name> as part of <SysLib>" );
        }
        SystemLibrarySet sysLibSet = new SystemLibrarySet();
        sysLibSet.setProject( antProject );
        sysLibSet.setLibs( new CUtil.StringArrayBuilder( name ) );
        LibraryTypeEnum sysLibType = new LibraryTypeEnum();
        sysLibType.setValue( type );
        sysLibSet.setType( sysLibType );
        return sysLibSet;*/

            if(fileVersion == null &&
                   productVersion == null &&
                    language == null &&
                    fileComments == null &&
                    companyName == null &&
                    fileDescription==null &&
                    internalName == null &&
                    legalCopyright == null &&
                    legalTrademarks == null &&
                    originalFilename == null &&
                    privateBuild == null &&
                     productName == null &&
                    specialBuild == null &&
                    compatibilityVersion == null &&
                    prerelease == null &&
                    patched == null 
                    )
            {
                    return null;
            }
        VersionInfo versionInfo = new VersionInfo();
        
        try
        {
        			if(fileVersion != null)
        			{
        					versionInfo.setFileversion(fileVersion);
        			}
        			
        			if(productVersion != null)
        			{
        					versionInfo.setProductversion(productVersion);
        			}
        			
        			if(language != null)
        			{
        					versionInfo.setLanguage(language);
        			}
        			
        			if(fileComments != null)
        			{
        					versionInfo.setFilecomments(fileComments);
        			}
        			
        			if(companyName != null)
        			{
        					versionInfo.setCompanyname(companyName);
        			}
        			if(fileDescription != null)
                    {
                            versionInfo.setFiledescription(fileDescription);
                    }
        			if(internalName != null)
        			{
        					versionInfo.setInternalname(internalName);
        			}
        			
        			if(legalCopyright != null)
        			{
        					versionInfo.setLegalcopyright(legalCopyright);
        			}
        			
        			if(legalTrademarks != null)
        			{
        					versionInfo.setLegaltrademarks(legalTrademarks);
        			}
        			
        			if(originalFilename != null)
        			{
        					versionInfo.setOriginalfilename(originalFilename);
        			}
        			
        			if(privateBuild != null)
        			{
        					versionInfo.setPrivatebuild(privateBuild);
        			}
        			
        			if(productName != null)
        			{
        					versionInfo.setProductname(productName);
        			}
        			
        			if(specialBuild != null)
        			{
        					versionInfo.setSpecialbuild(specialBuild);
        			}
        			
        			if(compatibilityVersion != null)
        			{
        					versionInfo.setCompatibilityversion(compatibilityVersion);
        			}
        			
        			if(prerelease != null)
        			{
        					versionInfo.setPrerelease(prerelease.booleanValue());
        			}
        			
        			if(patched != null)
        			{
        					versionInfo.setPatched(patched.booleanValue());
        			}
        			
        			
        			
        			
        			
        			
        }
        catch(BuildException be)
        {
        	
        	throw new MojoFailureException("NAR: Read artifact version failed", be);
        }
        
        return versionInfo;
        
    }
}
