package com.github.maven_nar.cpptasks.compiler;

import org.apache.tools.ant.BuildException;

import com.github.maven_nar.cpptasks.CCTask;
import com.github.maven_nar.cpptasks.LinkerParam;
import com.github.maven_nar.cpptasks.TargetInfo;
/**
 * A configuration for a linker
 * 
 * @author Curt Arnold
 */
public interface LinkerConfiguration extends ProcessorConfiguration {
    public LinkerParam getParam(String name);
    void link(CCTask task, TargetInfo linkTarget) throws BuildException;
    Linker getLinker();
    boolean isDebug();
}
