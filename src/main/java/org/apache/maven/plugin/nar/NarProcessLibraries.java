package org.apache.maven.plugin.nar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Adds the ability to run command line tools to post-process the compiled
 * libraries (ie: ranlib/ar/etc)
 * 
 * @goal nar-process-libraries
 * @phase process-classes
 * @requiresSession
 * @requiresProject
 * @author Richard Kerr
 */
public class NarProcessLibraries extends AbstractCompileMojo {

    /**
     * List of commands to execute
     * 
     * @parameter expression=""
     */
    private List<ProcessLibraryCommand> commands;

    @Override
    public void narExecute() throws MojoFailureException, MojoExecutionException {
	getLog().info("Running process libraries");
	// For each of the libraries defined for this build
	for (Library library : getLibraries()) {
	    getLog().info("Processing library " + library);
	    String type = library.getType();
	    File outDir;
	    // Find what the output directory is
	    if (type.equalsIgnoreCase(Library.EXECUTABLE)) {
		outDir = getLayout().getBinDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
			getMavenProject().getVersion(), getAOL().toString());
	    } else {
		outDir = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
			getMavenProject().getVersion(), getAOL().toString(), type);
	    }
	    // Then run the commands that are applicable for this library type
	    for (ProcessLibraryCommand command : commands) {
		if (command.getType().equalsIgnoreCase(type))
		    runCommand(command, outDir);
	    }
	}

    }

    private void runCommand(ProcessLibraryCommand command, File outputDirectory) throws MojoFailureException, MojoExecutionException {
	ProcessBuilder p = new ProcessBuilder(command.getCommandList());
	p.command().add(outputDirectory.toString() + "/" + getOutput(false));
	p.redirectErrorStream(true);
	getLog().info("Running command \"" + p.command() + "\"");
	try {
	    Process process = p.start();
	    BufferedInputStream bis = new BufferedInputStream(process.getInputStream());
	    byte[] buffer = new byte[1024];
	    int endOfStream = 0;
	    do {
		endOfStream = bis.read(buffer);
		getLog().debug(new String(buffer, 0, endOfStream == -1 ? 0 : endOfStream));
	    } while (endOfStream != -1);
	    
	    if (process.waitFor() != 0) {
		// TODO: Maybe this shouldn't be an exception, it might have still worked?!
		throw new MojoFailureException("Process exited abnormally");
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    throw new MojoFailureException("Failed to run the command \"" + p.command() + "\"");
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

}
