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

public class ProcessLibraryCommand {
    
    /**
     * The executable to run
     * 
     * @parameter default-value=""
     */
    private String executable;
    
    /**
     * The library type that this command is valid for
     * 
     * @parameter default-value=""
     */
    private String libraryType;
    
    /**
     * Any additional arguments to pass into the executable
     * 
     * @parameter default-value=""
     */
    private List<String> arguments;
    
    public List<String> getCommandList() {
	List<String> command = new ArrayList<String>();
	command.add(executable);
	if (arguments != null) {
		command.addAll(arguments);
	}
	return command;	
    }
    
    public String getExecutable() {
        return executable;
    }
    public void setExecutable(String executable) {
        this.executable = executable;
    }
    public List<String> getArguments() {
        return arguments;
    }
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }
    public String getType() {
	return libraryType;
    }
    public void setType(String type) {
    	libraryType = type;
    }

}
