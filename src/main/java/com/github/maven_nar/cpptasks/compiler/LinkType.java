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
package com.github.maven_nar.cpptasks.compiler;
import com.github.maven_nar.cpptasks.OutputTypeEnum;
import com.github.maven_nar.cpptasks.SubsystemEnum;
/**
 * This class represents the target platform for the compile and link step. The
 * name is an anachronism and should be changed.
 * 
 * @author Curt Arnold
 */
public class LinkType {
    private OutputTypeEnum outputType = new OutputTypeEnum();
    private boolean staticRuntime = false;
    private SubsystemEnum subsystem = new SubsystemEnum();
    
// BEGINFREEHEP
    private boolean linkCPP = true;
	private boolean linkFortran = false;
	private boolean linkFortranMain = false;
// ENDFREEHEP
	
    /**
     * Constructor
     * 
     * By default, an gui executable with a dynamically linked runtime
     *  
     */
    public LinkType() {
    }
    /**
     * Gets whether the link should produce an executable
     * 
     * @return boolean
     */
    public boolean isExecutable() {
        String value = outputType.getValue();
        return value.equals("executable");
    }
    
    public boolean isJNIModule() {
        String value = outputType.getValue();
        return value.equals("jni");    	
    }
    /**
     * Gets whether the link should produce a plugin module.
     * 
     * @return boolean
     */
    public boolean isPluginModule() {
        String value = outputType.getValue();
        return value.equals("plugin");
    }
    /**
     * Gets whether the link should produce a shared library.
     * 
     * @return boolean
     */
    public boolean isSharedLibrary() {
        String value = outputType.getValue();
// FREEHEP
        return value.equals("shared") || value.equals("plugin") || value.equals("jni");
    }
    /**
     * Gets whether the link should produce a static library.
     * 
     * @return boolean
     */
    public boolean isStaticLibrary() {
        String value = outputType.getValue();
        return value.equals("static");
    }
    /**
     * Gets whether the module should use a statically linked runtime library.
     * 
     * @return boolean
     */
    public boolean isStaticRuntime() {
        return staticRuntime;
    }
    /**
     * Gets whether the link should produce a module for a console subsystem.
     * 
     * @return boolean
     */
    public boolean isSubsystemConsole() {
        String value = subsystem.getValue();
        return value.equals("console");
    }
    /**
     * Gets whether the link should produce a module for a graphical user
     * interface subsystem.
     * 
     * @return boolean
     */
    public boolean isSubsystemGUI() {
        String value = subsystem.getValue();
        return value.equals("gui");
    }
    /**
     * Sets the output type (execuable, shared, etc).
     * 
     * @param outputType may not be null
     */
    public void setOutputType(OutputTypeEnum outputType) {
        if (outputType == null) {
            throw new IllegalArgumentException("outputType");
        }
        this.outputType = outputType;
    }
    
    /**
     * Gets the output type.
     * @return output type
     */
    public String getOutputType() {
    	return outputType.getValue();
    }
    
    /**
     * Requests use of a static runtime library.
     * 
     * @param staticRuntime
     *            if true, use static runtime library if possible.
     */
    public void setStaticRuntime(boolean staticRuntime) {
        this.staticRuntime = staticRuntime;
    }
    /**
     * Sets the subsystem (gui, console, etc).
     * 
     * @param subsystem
     *            subsystem, may not be null
     */
    public void setSubsystem(SubsystemEnum subsystem) {
        if (subsystem == null) {
            throw new IllegalArgumentException("subsystem");
        }
        this.subsystem = subsystem;
    }
    
    /**
     * Get subsystem name.
     * @return subsystem name
     */
    public String getSubsystem() {
    	return subsystem.getValue();
    }
    
// BEGINFREEHEP
    public void setLinkCPP(boolean linkCPP) {
    	this.linkCPP = linkCPP;
    }
    
    public boolean linkCPP() {
    	return linkCPP;
    }

    public void setLinkFortran(boolean linkFortran) {
    	this.linkFortran = linkFortran;
    }
    
    public boolean linkFortran() {
		return linkFortran;
	}

    public void setLinkFortranMain(boolean linkFortranMain) {
		this.linkFortranMain = linkFortranMain;
	}
    
    public boolean linkFortranMain() {
    	return linkFortranMain;
    }
	// ENDFREEHEP
    
}
