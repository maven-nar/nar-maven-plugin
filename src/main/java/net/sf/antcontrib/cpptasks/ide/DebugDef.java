/*
*
* Copyright 2004-2005 The Ant-Contrib project
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package net.sf.antcontrib.cpptasks.ide;

import java.io.File;

import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.DataType;


/**
*  Specifies a debugging configuration for a project.
*
* @author Curt Arnold
*/
public final class DebugDef
   extends DataType {

    /**
     * Working directory for debug runs.
     */
    private File dir;
    /**
     * Name of executable.
     */
    private String executable;
    /**
     * Environment used to hold environment variables.
     */
    private Environment env = new Environment();
    /**
     * Command line used to hold command line arguments.
     */
    private Commandline cmdl = new Commandline();

    /**
     * Constructor.
     *
     */
     public DebugDef() {
     }


     /**
      * Set the name of the executable program.
      * @param value the name of the executable program
      */
     public void setExecutable(final String value) {
         this.executable = value;
     }

     /**
      * Get the name of the executable program.
      * @return the name of the executable program, may be null.
      */
     public String getExecutable() {
         return executable;
     }

     /**
      * Set the working directory of the process.
      * @param d the working directory of the process
      */
     public void setDir(final File d) {
        this.dir = d;
     }

     /**
      * Get the working directory of the process.
      * @return the working directory of the process, may be null.
      */
     public File getDir() {
        return dir;
     }

     /**
      * Add an environment variable.
      *
      * @param var new environment variable
      */
     public void addEnv(final Environment.Variable var) {
        env.addVariable(var);
     }

     /**
      * Get the variable list as an array.
      * @return array of key=value assignment strings
      */
     public String[] getVariables() {
        return env.getVariables();
     }


     /**
      * Adds a command-line argument.
      *
      * @return new command line argument created
      */
     public Commandline.Argument createArg() {
         return cmdl.createArgument();
     }

     /**
      * Returns all arguments defined by <code>addLine</code>,
      * <code>addValue</code> or the argument object.
      * @return array of command line arguments, may be zero-length.
      */
     public String[] getArguments() {
        return cmdl.getArguments();
     }

}
