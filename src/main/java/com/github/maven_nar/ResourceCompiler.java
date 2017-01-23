package com.github.maven_nar;

import com.github.maven_nar.cpptasks.CompilerDef;
import com.github.maven_nar.cpptasks.CompilerEnum;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;


public class ResourceCompiler {

         public static CompilerDef getCompiler( String type, String output, Project antProject )
        throws MojoFailureException, MojoExecutionException
    {
        String name = "msrc";

        CompilerDef compiler = new CompilerDef();
        compiler.setProject( antProject );
        CompilerEnum compilerName = new CompilerEnum();
        compilerName.setValue( name );
        compiler.setName( compilerName );
            
           return compiler;
    }
}
