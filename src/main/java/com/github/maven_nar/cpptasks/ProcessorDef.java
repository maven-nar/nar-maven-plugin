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
package com.github.maven_nar.cpptasks;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Reference;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;
import com.github.maven_nar.cpptasks.types.CommandLineArgument;
import com.github.maven_nar.cpptasks.types.ConditionalFileSet;
/**
 * An abstract compiler/linker definition.
 * 
 * @author Curt Arnold
 */
public abstract class ProcessorDef extends DataType {
    /**
     * Returns the equivalent Boolean object for the specified value
     * 
     * Equivalent to Boolean.valueOf in JDK 1.4
     * 
     * @param val
     *            boolean value
     * @return Boolean.TRUE or Boolean.FALSE
     */
    protected static Boolean booleanValueOf(boolean val) {
        if (val) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
    /**
     * if true, targets will be built for debugging
     */
    private Boolean debug;
    private Environment env = null;
    /**
     * Reference for "extends" processor definition
     */
    private Reference extendsRef = null;
    /**
     * Name of property that must be present or definition will be ignored. May
     * be null.
     */
    private String ifProp;
    /**
     * if true, processor definition inherits values from containing cc
     * element
     */
    private boolean inherit;
    private Boolean libtool = null;
    protected boolean newEnvironment = false;
    /**
     * Processor.
     */
    private Processor processor;
    /**
     * Collection of <compilerarg>or <linkerarg>contained by definition
     */
    private final Vector processorArgs = new Vector();
    /**
     * Collection of <compilerparam>or <linkerparam>contained by definition
     */
    private final Vector processorParams = new Vector();
    /**
     * if true, all targets will be unconditionally rebuilt
     */
    private Boolean rebuild;
    /**
     * Collection of <fileset>contained by definition
     */
    private final Vector srcSets = new Vector();
    /**
     * Name of property that if present will cause definition to be ignored.
     * May be null.
     */
    private String unlessProp;
    /**
     * Constructor
     *  
     */
    protected ProcessorDef() throws NullPointerException {
        inherit = true;
    }
    /**
     * Adds a <compilerarg>or <linkerarg>
     * 
     * @param arg
     *            command line argument, must not be null
     * @throws NullPointerException
     *             if arg is null
     * @throws BuildException
     *             if this definition is a reference
     */
    protected void addConfiguredProcessorArg(CommandLineArgument arg)
            throws NullPointerException, BuildException {
        if (arg == null) {
            throw new NullPointerException("arg");
        }
        if (isReference()) {
            throw noChildrenAllowed();
        }
        processorArgs.addElement(arg);
    }
    /**
     * Adds a <compilerarg>or <linkerarg>
     * 
     * @param param
     *            command line argument, must not be null
     * @throws NullPointerException
     *             if arg is null
     * @throws BuildException
     *             if this definition is a reference
     */
    protected void addConfiguredProcessorParam(ProcessorParam param)
            throws NullPointerException, BuildException {
        if (param == null) {
            throw new NullPointerException("param");
        }
        if (isReference()) {
            throw noChildrenAllowed();
        }
        processorParams.addElement(param);
    }
    /**
     * Add an environment variable to the launched process.
     */
    public void addEnv(Environment.Variable var) {
        if (env == null) {
            env = new Environment();
        }
        env.addVariable(var);
    }
    /**
     * Because linkers have multiples and none of them share the environment settings here is a hack to give direct access to copy it just before running
     * @return
     */
    public Environment getEnv() {
        return env;
    }
    /**
     * Adds a source file set.
     * 
     * Files in these set will be processed by this configuration and will not
     * participate in the auction.
     * 
     * @param srcSet
     *            Fileset identifying files that should be processed by this
     *            processor
     * @throws BuildException
     *             if processor definition is a reference
     */
    public void addFileset(ConditionalFileSet srcSet) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        srcSet.setProject(getProject());
        srcSets.addElement(srcSet);
    }
    /**
     * Creates a configuration
     * 
     * @param baseDef
     *            reference to def from containing cc element, may be null
     * @return configuration
     *  
     */
    public ProcessorConfiguration createConfiguration(CCTask task,
            LinkType linkType, ProcessorDef baseDef, 
			TargetDef targetPlatform, VersionInfo versionInfo) {
        if (isReference()) {
            return ((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).createConfiguration(task, linkType,
                    baseDef, targetPlatform, versionInfo);
        }
        ProcessorDef[] defaultProviders = getDefaultProviders(baseDef);
        Processor proc = getProcessor(linkType);
        return proc.createConfiguration(task, linkType, defaultProviders, this, targetPlatform, versionInfo);
    }
    /**
     * Prepares list of processor arguments ( compilerarg, linkerarg ) that
     * are active for the current project settings.
     * 
     * @return active compiler arguments
     */
    public CommandLineArgument[] getActiveProcessorArgs() {
    	Project p = getProject();
        if (p == null) {
            throw new java.lang.IllegalStateException("project must be set");
        }
        if (isReference()) {
            return ((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).getActiveProcessorArgs();
        }
        Vector activeArgs = new Vector(processorArgs.size());
        for (int i = 0; i < processorArgs.size(); i++) {
            CommandLineArgument arg = (CommandLineArgument) processorArgs
                    .elementAt(i);
            if (arg.isActive(p)) {
                activeArgs.addElement(arg);
            }
        }
        CommandLineArgument[] array = new CommandLineArgument[activeArgs.size()];
        activeArgs.copyInto(array);
        return array;
    }
    /**
     * Prepares list of processor arguments ( compilerarg, linkerarg) that
     * are active for the current project settings.
     * 
     * @return active compiler arguments
     */
    public ProcessorParam[] getActiveProcessorParams() {
    	Project p = getProject();
        if (p == null) {
            throw new java.lang.IllegalStateException("project must be set");
        }
        if (isReference()) {
            return ((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).getActiveProcessorParams();
        }
        Vector activeParams = new Vector(processorParams.size());
        for (int i = 0; i < processorParams.size(); i++) {
            ProcessorParam param = (ProcessorParam) processorParams
                    .elementAt(i);
            if (param.isActive(p)) {
                activeParams.addElement(param);
            }
        }
        ProcessorParam[] array = new ProcessorParam[activeParams.size()];
        activeParams.copyInto(array);
        return array;
    }
    /**
     * Gets boolean indicating debug build
     * 
     * @param defaultProviders
     *            array of ProcessorDef's in descending priority
     * @param index
     *            index to first element in array that should be considered
     * @return if true, built targets for debugging
     */
    public boolean getDebug(ProcessorDef[] defaultProviders, int index) {
        if (isReference()) {
            return ((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).getDebug(defaultProviders, index);
        }
        if (debug != null) {
            return debug.booleanValue();
        } else {
            if (defaultProviders != null && index < defaultProviders.length) {
                return defaultProviders[index].getDebug(defaultProviders,
                        index + 1);
            }
        }
        return false;
    }
    /**
     * Creates an chain of objects which provide default values in descending
     * order of significance.
     * 
     * @param baseDef
     *            corresponding ProcessorDef from CCTask, will be last element
     *            in array unless inherit = false
     * @return default provider array
     *  
     */
    protected final ProcessorDef[] getDefaultProviders(ProcessorDef baseDef) {
        ProcessorDef extendsDef = getExtends();
        Vector chain = new Vector();
        while (extendsDef != null && !chain.contains(extendsDef)) {
            chain.addElement(extendsDef);
            extendsDef = extendsDef.getExtends();
        }
        if (baseDef != null && getInherit()) {
            chain.addElement(baseDef);
        }
        ProcessorDef[] defaultProviders = new ProcessorDef[chain.size()];
        chain.copyInto(defaultProviders);
        return defaultProviders;
    }
    /**
     * Gets the ProcessorDef specified by the extends attribute
     * 
     * @return Base ProcessorDef, null if extends is not specified
     * @throws BuildException
     *             if reference is not same type object
     */
    public ProcessorDef getExtends() throws BuildException {
        if (extendsRef != null) {
            Object obj = extendsRef.getReferencedObject(getProject());
            if (!getClass().isInstance(obj)) {
                throw new BuildException("Referenced object "
                        + extendsRef.getRefId() + " not correct type, is "
                        + obj.getClass().getName() + " should be "
                        + getClass().getName());
            }
            return (ProcessorDef) obj;
        }
        return null;
    }
    /**
     * Gets the inherit attribute. If the inherit value is true, this processor
     * definition will inherit default values from the containing cc element.
     * 
     * @return if true then properties from the containing <cc>element are
     *         used.
     */
    public final boolean getInherit() {
        return inherit;
    }
    public boolean getLibtool() {
        if (libtool != null) {
            return libtool.booleanValue();
        }
        if (isReference()) {
            return ((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).getLibtool();
        }
        ProcessorDef extendsDef = getExtends();
        if (extendsDef != null) {
            return extendsDef.getLibtool();
        }
        return false;
    }
    /**
     * Obtains the appropriate processor (compiler, linker)
     * 
     * @return processor
     */
    protected Processor getProcessor() {
        if (isReference()) {
            return ((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).getProcessor();
        }
        //
        //   if a processor has not been explicitly set
        //      then may be set by an extended definition
        if (processor == null) {
            ProcessorDef extendsDef = getExtends();
            if (extendsDef != null) {
                return extendsDef.getProcessor();
            }
        }
        return processor;
    }

    /**
     * Obtains the appropriate processor (compiler, linker) based on the 
     * LinkType.
     *
     * @return processor
     */
    protected Processor getProcessor(LinkType linkType) {
      // by default ignore the linkType.
      return getProcessor();
    }
    /**
     * Gets a boolean value indicating whether all targets must be rebuilt
     * regardless of dependency analysis.
     * 
     * @param defaultProviders
     *            array of ProcessorDef's in descending priority
     * @param index
     *            index to first element in array that should be considered
     * @return true if all targets should be rebuilt.
     */
    public boolean getRebuild(ProcessorDef[] defaultProviders, int index) {
        if (isReference()) {
            return ((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).getRebuild(defaultProviders, index);
        }
        if (rebuild != null) {
            return rebuild.booleanValue();
        } else {
            if (defaultProviders != null && index < defaultProviders.length) {
                return defaultProviders[index].getRebuild(defaultProviders,
                        index + 1);
            }
        }
        return false;
    }
    /**
     * Returns true if the processor definition contains embedded file set
     * definitions
     * 
     * @return true if processor definition contains embedded filesets
     */
    public boolean hasFileSets() {
        if (isReference()) {
            return ((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).hasFileSets();
        }
        return srcSets.size() > 0;
    }
    /**
     * Determine if this def should be used.
     * 
     * Definition will be active if the "if" variable (if specified) is set and
     * the "unless" variable (if specified) is not set and that all reference
     * or extended definitions are active
     * 
     * @return true if processor is active
     * @throws IllegalStateException
     *             if not properly initialized
     * @throws BuildException
     *             if "if" or "unless" variable contains suspicious values
     *             "false" or "no" which indicates possible confusion
     */
    public boolean isActive() throws BuildException, IllegalStateException {
        Project project = getProject();
        if (!CUtil.isActive(project, ifProp, unlessProp)) {
            return false;
        }
        if (isReference()) {
            if (!((ProcessorDef) getCheckedRef(ProcessorDef.class,
                    "ProcessorDef")).isActive()) {
                return false;
            }
        }
        //
        //  walk through any extended definitions
        //
        ProcessorDef[] defaultProviders = getDefaultProviders(null);
        for (int i = 0; i < defaultProviders.length; i++) {
            if (!defaultProviders[i].isActive()) {
                return false;
            }
        }
        return true;
    }
    /**
     * Sets the class name for the adapter. Use the "name" attribute when the
     * tool is supported.
     * 
     * @param className
     *            full class name
     *  
     */
    public void setClassname(String className) throws BuildException {
        Object proc = null;
        try {
            Class implClass = ProcessorDef.class.getClassLoader().loadClass(
                    className);
            try {
                Method getInstance = implClass.getMethod("getInstance",
                        new Class[0]);
                proc = getInstance.invoke(null, new Object[0]);
            } catch (Exception ex) {
                proc = implClass.newInstance();
            }
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
        setProcessor((Processor) proc);
    }
    /**
     * If set true, all targets will be built for debugging.
     * 
     * @param debug
     *            true if targets should be built for debugging
     * @throws BuildException
     *             if processor definition is a reference
     */
    public void setDebug(boolean debug) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.debug = booleanValueOf(debug);
    }
    /**
     * Sets a description of the current data type.
     */
    public void setDescription(String desc) {
        super.setDescription(desc);
    }
    /**
     * Specifies that this element extends the element with id attribute with a
     * matching value. The configuration will be constructed from the settings
     * of this element, element referenced by extends, and the containing cc
     * element.
     * 
     * @param extendsRef
     *            Reference to the extended processor definition.
     * @throws BuildException
     *             if this processor definition is a reference
     */
    public void setExtends(Reference extendsRef) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.extendsRef = extendsRef;
    }
    /**
     * Sets an id that can be used to reference this element.
     * 
     * @param id
     *            id
     */
    public void setId(String id) {
        //
        //  this is actually accomplished by a different
        //     mechanism, but we can document it
        //
    }
    /**
     * Sets the property name for the 'if' condition.
     * 
     * The configuration will be ignored unless the property is defined.
     * 
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") will throw an exception when
     * evaluated.
     * 
     * @param propName
     *            name of property
     */
    public void setIf(String propName) {
        ifProp = propName;
    }
    /**
     * If inherit has the default value of true, defines, includes and other
     * settings from the containing cc element will be inherited.
     * 
     * @param inherit
     *            new value
     * @throws BuildException
     *             if processor definition is a reference
     */
    public void setInherit(boolean inherit) throws BuildException {
        if (isReference()) {
            throw super.tooManyAttributes();
        }
        this.inherit = inherit;
    }
    /**
     * Set use of libtool.
     * 
     * If set to true, the "libtool " will be prepended to the command line
     * 
     * @param libtool
     *            If true, use libtool.
     */
    public void setLibtool(boolean libtool) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.libtool = booleanValueOf(libtool);
    }
    /**
     * Do not propagate old environment when new environment variables are
     * specified.
     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }
    /**
     * Sets the processor
     * 
     * @param processor
     *            processor, may not be null.
     * @throws BuildException
     *             if ProcessorDef is a reference
     * @throws NullPointerException
     *             if processor is null
     */
    protected void setProcessor(Processor processor) throws BuildException,
            NullPointerException {
        if (processor == null) {
            throw new NullPointerException("processor");
        }
        if (isReference()) {
            throw super.tooManyAttributes();
        }
        if (env == null && !newEnvironment) {
            this.processor = processor;
        } else {
            this.processor = processor.changeEnvironment(newEnvironment, env);
        }
    }
    /**
     * If set true, all targets will be unconditionally rebuilt.
     * 
     * @param rebuild
     *            if true, rebuild all targets.
     * @throws BuildException
     *             if processor definition is a reference
     */
    public void setRebuild(boolean rebuild) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.rebuild = booleanValueOf(rebuild);
    }
    /**
     * Specifies that this element should behave as if the content of the
     * element with the matching id attribute was inserted at this location. If
     * specified, no other attributes or child content should be specified,
     * other than "if", "unless" and "description".
     * 
     * @param ref
     *            Reference to other element
     *  
     */
    public void setRefid(org.apache.tools.ant.types.Reference ref) {
        super.setRefid(ref);
    }
    /**
     * Set the property name for the 'unless' condition.
     * 
     * If named property is set, the configuration will be ignored.
     * 
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") of the behavior will throw an
     * exception when evaluated.
     * 
     * @param propName
     *            name of property
     */
    public void setUnless(String propName) {
        unlessProp = propName;
    }
    /**
     * This method calls the FileVistor's visit function for every file in the
     * processors definition
     * 
     * @param visitor
     *            object whose visit method is called for every file
     */
    public void visitFiles(FileVisitor visitor) {
    	Project p = getProject();
        if (p == null) {
            throw new java.lang.IllegalStateException(
                    "project must be set before this call");
        }
        if (isReference()) {
            ((ProcessorDef) getCheckedRef(ProcessorDef.class, "ProcessorDef"))
                    .visitFiles(visitor);
        }
        //
        //   if this processor extends another,
        //      visit its files first
        //
        ProcessorDef extendsDef = getExtends();
        if (extendsDef != null) {
            extendsDef.visitFiles(visitor);
        }
        
        for (int i = 0; i < srcSets.size(); i++) {
            ConditionalFileSet srcSet = (ConditionalFileSet) srcSets
                    .elementAt(i);
            if (srcSet.isActive()) {
                // Find matching source files
                DirectoryScanner scanner = srcSet.getDirectoryScanner(p);
                // Check each source file - see if it needs compilation
                String[] fileNames = scanner.getIncludedFiles();
                File parentDir = scanner.getBasedir();
                for (int j = 0; j < fileNames.length; j++) {
                    String currentFile = fileNames[j];
                    visitor.visit(parentDir, currentFile);
                }
            }
        }
    }
}
