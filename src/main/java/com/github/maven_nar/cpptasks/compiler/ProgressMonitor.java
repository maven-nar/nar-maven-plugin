package com.github.maven_nar.cpptasks.compiler;
/**
 * Interface to receive notification of compile progress
 * 
 * @author Curt Arnold
 */
public interface ProgressMonitor {
    public void finish(ProcessorConfiguration config, boolean normal);
    /**
     * Called to notify monitor of progress
     *  
     */
    void progress(String[] sources);
    public void start(ProcessorConfiguration config);
}
