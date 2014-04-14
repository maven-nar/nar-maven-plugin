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
