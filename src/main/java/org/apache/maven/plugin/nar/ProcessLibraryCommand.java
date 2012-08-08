package org.apache.maven.plugin.nar;

import java.util.ArrayList;
import java.util.List;

public class ProcessLibraryCommand {
    
    /**
     * The executable to run
     * 
     * @parameter expression=""
     */
    private String executable;
    
    /**
     * The library type that this command is valid for
     * 
     * @parameter expression=""
     */
    private String libraryType;
    
    /**
     * Any additional arguments to pass into the executable
     * 
     * @parameter expression=""
     */
    private List<String> arguments;
    
    public List<String> getCommandList() {
	List<String> command = new ArrayList<String>();
	command.add(executable);
	command.addAll(arguments);
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
	// TODO Auto-generated method stub
	return libraryType;
    }

}
