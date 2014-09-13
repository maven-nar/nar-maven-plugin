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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Environment;

import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;
import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;
import com.github.maven_nar.cpptasks.compiler.LinkerConfiguration;
import com.github.maven_nar.cpptasks.compiler.Processor;
import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;
import com.github.maven_nar.cpptasks.ide.ProjectDef;
import com.github.maven_nar.cpptasks.types.CompilerArgument;
import com.github.maven_nar.cpptasks.types.ConditionalFileSet;
import com.github.maven_nar.cpptasks.types.DefineSet;
import com.github.maven_nar.cpptasks.types.IncludePath;
import com.github.maven_nar.cpptasks.types.LibrarySet;
import com.github.maven_nar.cpptasks.types.LinkerArgument;
import com.github.maven_nar.cpptasks.types.SystemIncludePath;
import com.github.maven_nar.cpptasks.types.SystemLibrarySet;
/**
 * Compile and link task.
 * 
 * <p>
 * This task can compile various source languages and produce executables,
 * shared libraries (aka DLL's) and static libraries. Compiler adaptors are
 * currently available for several C/C++ compilers, FORTRAN, MIDL and Windows
 * Resource files.
 * </p>
 * 
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public class CCTask extends Task {






    private static class SystemLibraryCollector implements FileVisitor {
        private Hashtable<String, File> libraries;
        private Linker linker;
        public SystemLibraryCollector(Linker linker, Hashtable<String, File> libraries) {
            this.linker = linker;
            this.libraries = libraries;
        }
        public void visit(File basedir, String filename) {
            if (linker.bid(filename) > 0) {
                File libfile = new File(basedir, filename);
                String key = linker.getLibraryKey(libfile);
                libraries.put(key, libfile);
            }
        }
    }
    
    private static class ProjectFileCollector implements FileVisitor {
        private final List<File> files;
        /**
         * Creates a new ProjectFileCollector.
         * @param files vector for collected files.
         */
        public ProjectFileCollector(List<File> files) {
            this.files = files;
        }
        /**
         * Called for each file to be considered for collection.
         * @param parentDir parent directory
         * @param filename filename within directory
         */
        public void visit(File parentDir, String filename)  {
            files.add(new File(parentDir, filename));
        }
    }
    
    private static final ProcessorConfiguration[] EMPTY_CONFIG_ARRAY = new ProcessorConfiguration[0];
    /**
     * Builds a Hashtable to targets needing to be rebuilt keyed by compiler
     * configuration
     */
    public static Map<ProcessorConfiguration, Vector<TargetInfo>> getTargetsToBuildByConfiguration(Map<String, TargetInfo> targets) {
        Map<ProcessorConfiguration, Vector<TargetInfo>> targetsByConfig = new HashMap<ProcessorConfiguration, Vector<TargetInfo>>();
        Iterator<TargetInfo> targetEnum = targets.values().iterator();
        while (targetEnum.hasNext()) {
            TargetInfo target = targetEnum.next();
            if (target.getRebuild()) {
                Vector<TargetInfo> targetsForSameConfig = targetsByConfig
                        .get(target.getConfiguration());
                if (targetsForSameConfig != null) {
                    targetsForSameConfig.addElement(target);
                } else {
                    targetsForSameConfig = new Vector<TargetInfo>();
                    targetsForSameConfig.addElement(target);
                    targetsByConfig.put(target.getConfiguration(),
                            targetsForSameConfig);
                }
            }
        }
        return targetsByConfig;
    }
    // FREEHEP
    private int maxCores = 0;
    private boolean ordered = false;
    /** The compiler definitions. */
    private Vector<CompilerDef> _compilers = new Vector<CompilerDef>();
    /** The output file type. */
    //    private LinkType _linkType = LinkType.EXECUTABLE;
    /** The library sets. */
    private Vector _libsets = new Vector();
    /** The linker definitions. */
    private Vector<LinkerDef> _linkers = new Vector<LinkerDef>();
    /** The object directory. */
    private File _objDir;
    /** The output file. */
    private File _outfile;
    /** The linker definitions. */
    private final Vector<TargetDef> targetPlatforms = new Vector<TargetDef>();
    /** The distributer definitions. */
    private Vector<DistributerDef> distributers = new Vector<DistributerDef>();
    private final Vector<VersionInfo> versionInfos = new Vector<VersionInfo>();
    private final Vector<ProjectDef> projects = new Vector<ProjectDef>();
    private boolean projectsOnly = false;

    private boolean decorateLinkerOptions = true;

    public boolean isDecorateLinkerOptions() {
        return decorateLinkerOptions;
    }

    public void setDecorateLinkerOptions(boolean decorateLinkerOptions) {
        this.decorateLinkerOptions = decorateLinkerOptions;
    }

    
    /**
     * If true, stop build on compile failure.
     */
    protected boolean failOnError = true;
    
    /**
     * Content that appears in <cc>and also in <compiler>are maintained by a
     * captive CompilerDef instance
     */
    private final CompilerDef compilerDef = new CompilerDef();
    /** The OS390 dataset to build to object to */
    private String dataset;
    /**
     * 
     * Depth of dependency checking
     * 
     * Values < 0 indicate full dependency checking Values >= 0 indicate
     * partial dependency checking and for superficial compilation checks. Will
     * throw BuildException before attempting link
     */
    private int dependencyDepth = -1;
    /**
     * Content that appears in <cc>and also in <linker>are maintained by a
     * captive CompilerDef instance
     */
    private final LinkerDef linkerDef = new LinkerDef();
    /**
     * contains the subsystem, output type and
     *  
     */
    private final LinkType linkType = new LinkType();
    /**
     * The property name which will be set with the physical filename of the
     * file that is generated by the linker
     */
    private String outputFileProperty;
    /**
     * if relentless = true, compilations should attempt to compile as many
     * files as possible before throwing a BuildException
     */
    private boolean relentless;

    /**
     * At which log-level do we log command-lines in the build?
     */
    private int  commandLogLevel = Project.MSG_VERBOSE;

    public CCTask() {
    }
    /**
     * Adds a compiler definition or reference.
     * 
     * @param compiler
     *            compiler
     * @throws NullPointerException
     *             if compiler is null
     */
    public void addConfiguredCompiler(CompilerDef compiler) {
        if (compiler == null) {
            throw new NullPointerException("compiler");
        }
        compiler.setProject(getProject());
        _compilers.addElement(compiler);
    }
    /**
     * Adds a compiler command-line arg. Argument will be inherited by all
     * nested compiler elements that do not have inherit="false".
     *  
     */
    public void addConfiguredCompilerArg(CompilerArgument arg) {
        compilerDef.addConfiguredCompilerArg(arg);
    }
    /**
     * Adds a defineset. Will be inherited by all compiler elements that do not
     * have inherit="false".
     * 
     * @param defs
     *            Define set
     */
    public void addConfiguredDefineset(DefineSet defs) {
        compilerDef.addConfiguredDefineset(defs);
    }
    /**
     * Adds a linker definition. The first linker that is not disqualified by
     * its "if" and "unless" attributes will perform the link. If no child
     * linker element is active, the linker implied by the cc elements name or
     * classname attribute will be used.
     * 
     * @param linker
     *            linker
     * @throws NullPointerException
     *             if linker is null
     */
    public void addConfiguredLinker(LinkerDef linker) {
        if (linker == null) {
            throw new NullPointerException("linker");
        }
        linker.setProject(getProject());
        _linkers.addElement(linker);
    }
    /**
     * Adds a linker command-line arg. Argument will be inherited by all nested
     * linker elements that do not have inherit="false".
     */
    public void addConfiguredLinkerArg(LinkerArgument arg) {
        linkerDef.addConfiguredLinkerArg(arg);
    }
    /**
     * Add an environment variable to the launched process.
     */
    public void addEnv(Environment.Variable var) {
        compilerDef.addEnv(var);
        linkerDef.addEnv(var);
    }
    /**
     * Adds a source file set.
     * 
     * Files in these filesets will be auctioned to the available compiler
     * configurations, with the default compiler implied by the cc element
     * bidding last. If no compiler is interested in the file, it will be
     * passed to the linker.
     * 
     * To have a file be processed by a particular compiler configuration, add
     * a fileset to the corresponding compiler element.
     */
    public void addFileset(ConditionalFileSet srcSet) {
        compilerDef.addFileset(srcSet);
    }
    /**
     * Adds a library set.
     * 
     * Library sets will be inherited by all linker elements that do not have
     * inherit="false".
     * 
     * @param libset
     *            library set
     * @throws NullPointerException
     *             if libset is null.
     */
    public void addLibset(LibrarySet libset) {
        if (libset == null) {
            throw new NullPointerException("libset");
        }
        linkerDef.addLibset(libset);
    }
    /**
     * Adds a system library set. Timestamps and locations of system library
     * sets are not used in dependency analysis.
     * 
     * Essential libraries (such as C Runtime libraries) should not be
     * specified since the task will attempt to identify the correct libraries
     * based on the multithread, debug and runtime attributes.
     * 
     * System library sets will be inherited by all linker elements that do not
     * have inherit="false".
     * 
     * @param libset
     *            library set
     * @throws NullPointerException
     *             if libset is null.
     */
    public void addSyslibset(SystemLibrarySet libset) {
        if (libset == null) {
            throw new NullPointerException("libset");
        }
        linkerDef.addSyslibset(libset);
    }
    /**
     * Specifies the generation of IDE project file.  Experimental.
     * @param projectDef project file generation specification
     */
    public void addProject(final ProjectDef projectDef) {
        if (projectDef == null) {
            throw new NullPointerException("projectDef");
        }
        projects.addElement(projectDef);
    }
    public void setProjectsOnly(final boolean value) {
    	projectsOnly = value;
    }
    /**
     * Checks all targets that are not forced to be rebuilt or are missing
     * object files to be checked for modified include files
     * 
     * @return total number of targets to be rebuilt
     *  
     */
    protected int checkForChangedIncludeFiles(Map<String, TargetInfo> targets) {
        int potentialTargets = 0;
        int definiteTargets = 0;
        Iterator<TargetInfo> targetEnum = targets.values().iterator();
        while (targetEnum.hasNext()) {
            TargetInfo target = targetEnum.next();
            if (!target.getRebuild()) {
                potentialTargets++;
            } else {
                definiteTargets++;
            }
        }
        //
        //     If there were remaining targets that
        //        might be out of date
        //
        if (potentialTargets > 0) {
            log("Starting dependency analysis for "
                    + Integer.toString(potentialTargets) + " files.");
            DependencyTable dependencyTable = new DependencyTable(_objDir);
            try {
                dependencyTable.load();
            } catch (Exception ex) {
                log("Problem reading dependencies.xml: " + ex.toString());
            }
            targetEnum = targets.values().iterator();
            while (targetEnum.hasNext()) {
                TargetInfo target = targetEnum.next();
                if (!target.getRebuild()) {
                    if (dependencyTable.needsRebuild(this, target,
                            dependencyDepth)) {
                        target.mustRebuild();
                    }
                }
            }
            dependencyTable.commit(this);
        }
        //
        //   count files being rebuilt now
        //
        int currentTargets = 0;
        targetEnum = targets.values().iterator();
        while (targetEnum.hasNext()) {
            TargetInfo target = targetEnum.next();
            if (target.getRebuild()) {
                currentTargets++;
            }
        }
        if (potentialTargets > 0) {
            log(Integer.toString(potentialTargets - currentTargets
                    + definiteTargets)
                    + " files are up to date.");
            log(Integer.toString(currentTargets - definiteTargets)
                    + " files to be recompiled from dependency analysis.");
        }
        log(Integer.toString(currentTargets) + " total files to be compiled.");
        return currentTargets;
    }
    protected LinkerConfiguration collectExplicitObjectFiles(
            Vector<File> objectFiles, Vector<File> sysObjectFiles, VersionInfo versionInfo) {
        //
        //    find the first eligible linker
        //
        //
        ProcessorConfiguration linkerConfig = null;
        LinkerDef selectedLinkerDef = null;
        Linker selectedLinker = null;
        Hashtable<String, File> sysLibraries = new Hashtable<String, File>();
        TargetDef targetPlatform = getTargetPlatform();
        FileVisitor objCollector = null;
        FileVisitor sysLibraryCollector = null;
        for (int i = 0; i < _linkers.size(); i++) {
            LinkerDef currentLinkerDef = (LinkerDef) _linkers.elementAt(i);
            if (currentLinkerDef.isActive()) {
                selectedLinkerDef = currentLinkerDef;
                selectedLinker = currentLinkerDef.getProcessor().getLinker(
                        linkType);
                //
                //   skip the linker if it doesn't know how to
                //      produce the specified link type
                if (selectedLinker != null) {
                    linkerConfig = currentLinkerDef.createConfiguration(this,
                            linkType, linkerDef, targetPlatform, versionInfo);
                    if (linkerConfig != null) {
                        //
                        //   create collectors for object files
                        //      and system libraries
                        objCollector = new ObjectFileCollector(selectedLinker,
                                objectFiles);
                        sysLibraryCollector = new SystemLibraryCollector(
                                selectedLinker, sysLibraries);
                        //
                        //    if the <linker> has embedded <fileset>'s
                        //       (such as linker specific libraries)
                        //       add them as object files.
                        //
                        if (currentLinkerDef.hasFileSets()) {
                            currentLinkerDef.visitFiles(objCollector);
                        }
                        //
                        //    user libraries are just a specialized form
                        //       of an object fileset
                        selectedLinkerDef.visitUserLibraries(selectedLinker,
                                objCollector);
                    }
                    break;
                }
            }
        }
        if (linkerConfig == null) {
            linkerConfig = linkerDef.createConfiguration(this, linkType, null, targetPlatform, versionInfo);
            selectedLinker = (Linker) linkerDef.getProcessor().getLinker(
                    linkType);
            objCollector = new ObjectFileCollector(selectedLinker, objectFiles);
            sysLibraryCollector = new SystemLibraryCollector(selectedLinker,
                    sysLibraries);
        }
        //
        //  unless there was a <linker> element that
        //     explicitly did not inherit files from
        //        containing <cc> element
        if (selectedLinkerDef == null || selectedLinkerDef.getInherit()) {
            linkerDef.visitUserLibraries(selectedLinker, objCollector);
            linkerDef.visitSystemLibraries(selectedLinker, sysLibraryCollector);
        }
        //
        //   if there was a <syslibset> in a nested <linker>
        //       evaluate it last so it takes priority over
        //       identically named libs from <cc> element
        //
        if (selectedLinkerDef != null) {
            //
            //    add any system libraries to the hashtable
            //       done in reverse order so the earliest
            //        on the classpath takes priority
            selectedLinkerDef.visitSystemLibraries(selectedLinker,
                    sysLibraryCollector);
        }
        //
        //   copy over any system libraries to the
        //      object files vector
        //
        Enumeration<File> sysLibEnum = sysLibraries.elements();
        while (sysLibEnum.hasMoreElements()) {
            sysObjectFiles.addElement(sysLibEnum.nextElement());
        }
        return (LinkerConfiguration) linkerConfig;
    }
    /**
     * Adds an include path.
     * 
     * Include paths will be inherited by nested compiler elements that do not
     * have inherit="false".
     */
    public IncludePath createIncludePath() {
        return compilerDef.createIncludePath();
    }
    /**
     * Specifies precompilation prototype file and exclusions. Inherited by all
     * compilers that do not have inherit="false".
     *  
     */
    public PrecompileDef createPrecompile() throws BuildException {
        return compilerDef.createPrecompile();
    }
    /**
     * Adds a system include path. Locations and timestamps of files located
     * using the system include paths are not used in dependency analysis.
     * 
     * 
     * Standard include locations should not be specified. The compiler
     * adapters should recognized the settings from the appropriate environment
     * variables or configuration files.
     * 
     * System include paths will be inherited by nested compiler elements that
     * do not have inherit="false".
     */
    public SystemIncludePath createSysIncludePath() {
        return compilerDef.createSysIncludePath();
    }
    /**
     * Executes the task. Compiles the given files.
     * 
     * @throws BuildException
     *             if someting goes wrong with the build
     */
    public void execute() throws BuildException {
        //
        //   if link type allowed objdir to be defaulted
        //      provide it from outfile
        if (_objDir == null) {
        	if(_outfile != null) {
        		_objDir = new File(_outfile.getParent());
        	} else {
        		_objDir = new File(".");
        	}
        }
        
        //
        //   if the object directory does not exist
        //
        if (!_objDir.exists()) {
            throw new BuildException("Object directory does not exist");
        }
        TargetHistoryTable objHistory = new TargetHistoryTable(this, _objDir);

        //
        //   get the first active version info
        //
        VersionInfo versionInfo = null;
        Enumeration<VersionInfo> versionEnum = versionInfos.elements();
        while (versionEnum.hasMoreElements()) {
        	versionInfo = (VersionInfo) versionEnum.nextElement();
        	versionInfo = versionInfo.merge();
        	if (versionInfo.isActive()) {
        		break;
        	} else {
        		versionInfo = null;
        	}
        }
        
        
        //
        //  determine the eventual linker configuration
        //      (may be null) and collect any explicit
        //          object files or libraries
        Vector<File> objectFiles = new Vector<File>();
        Vector<File> sysObjectFiles = new Vector<File>();
        LinkerConfiguration linkerConfig = collectExplicitObjectFiles(
                objectFiles, sysObjectFiles, versionInfo);
        
        
        //
        //   Assemble hashtable of all files
        //       that we know how to compile (keyed by output file name)
        //
        Map<String, TargetInfo> targets = getTargets(linkerConfig, objectFiles, versionInfo, _outfile);
        TargetInfo linkTarget = null;
        //
        //   if output file is not specified,
        //      then skip link step
        //
        if (_outfile != null) {
            linkTarget = getLinkTarget(linkerConfig, objectFiles,
                    sysObjectFiles, targets, versionInfo);
        }
        
        if (projects.size() > 0) {
        	List<File> files = new ArrayList<File>();
        	ProjectFileCollector matcher = new ProjectFileCollector(files);
            for (int i = 0; i < _compilers.size(); i++) {
                CompilerDef currentCompilerDef = (CompilerDef) _compilers
                        .elementAt(i);
                if (currentCompilerDef.isActive()) {
                    if (currentCompilerDef.hasFileSets()) {
                        currentCompilerDef.visitFiles(matcher);
                    }
                }
            }
            compilerDef.visitFiles(matcher);
        	
        	
        	Enumeration<ProjectDef> iter = projects.elements();
        	while (iter.hasMoreElements()) {
        		ProjectDef projectDef = iter.nextElement();
        		if (projectDef.isActive()) {
        			projectDef.execute(this, files, targets, linkTarget);
        		}
        	}
        }
        if (projectsOnly) return;
      
        
        
        //
        //     mark targets that don't have a history record or
        //        whose source last modification time is not
        //        the same as the history to be rebuilt
        //
        objHistory.markForRebuild(targets);
        CCTaskProgressMonitor monitor = new CCTaskProgressMonitor(objHistory, versionInfo);
        //
        //      check for changed include files
        //
        int rebuildCount = checkForChangedIncludeFiles(targets);
        if (rebuildCount > 0) {
            BuildException compileException = null;
            //
            //    compile all targets with getRebuild() == true
            //
            Map<ProcessorConfiguration, Vector<TargetInfo>> targetsByConfig =
                    getTargetsToBuildByConfiguration(targets);
            //
            //    build array containing Vectors with precompiled generation
            //       steps going first
            //
            Vector<TargetInfo>[] targetVectors = new Vector[targetsByConfig.size()];
            int index = 0;
            Iterator<Vector<TargetInfo>> targetVectorEnum = targetsByConfig.values().iterator();
            while (targetVectorEnum.hasNext()) {
                Vector<TargetInfo> targetsForConfig = targetVectorEnum.next();
                //
                //    get the configuration from the first entry
                //
                CompilerConfiguration config = (CompilerConfiguration) ((TargetInfo) targetsForConfig
                        .elementAt(0)).getConfiguration();
                if (config.isPrecompileGeneration()) {
                    targetVectors[index++] = targetsForConfig;
                }
            }
            targetVectorEnum = targetsByConfig.values().iterator();
            while (targetVectorEnum.hasNext()) {
                Vector<TargetInfo> targetsForConfig = targetVectorEnum.next();
                for (int i = 0; i < targetVectors.length; i++) {
                    if (targetVectors[i] == targetsForConfig) {
                        break;
                    }
                    if (targetVectors[i] == null) {
                        targetVectors[i] = targetsForConfig;
                        break;
                    }
                }
            }
            // BEGINFREEHEP
            Progress progress = new Progress(getObjdir(), rebuildCount);
            progress.start();
            // ENDFREEHEP
            
            for (int i = 0; i < targetVectors.length; i++) {
                //
                //    get the targets for this configuration
                //
                Vector<TargetInfo> targetsForConfig = targetVectors[i];
                //
                //    get the configuration from the first entry
                //
                CompilerConfiguration config = (CompilerConfiguration) ((TargetInfo) targetsForConfig
                        .elementAt(0)).getConfiguration();
                //
                //    prepare the list of source files
                //
                
                // BEGINFREEHEP
                int noOfCores = Runtime.getRuntime().availableProcessors();
                log("Found "+noOfCores+" processors available");
                if (maxCores > 0) {
                    noOfCores = Math.min(maxCores, noOfCores);
                    log("Limited processors to "+noOfCores);
                }
                int noOfFiles = targetsForConfig.size();
                if (noOfFiles < noOfCores) {
                    noOfCores = noOfFiles;
                    log("Limited used processors to "+noOfCores);                   
                }
                if (ordered) {
                	noOfCores = 1;
                	log("Limited processors to 1 due to ordering of source files");
                }
                
                List<String>[] sourceFiles = new List[noOfCores];
                for (int j = 0; j < sourceFiles.length; j++) {
                    sourceFiles[j] = new ArrayList<String>(noOfFiles / sourceFiles.length);
                }
                Enumeration<TargetInfo> targetsEnum = targetsForConfig.elements();
                index = 0;
                while (targetsEnum.hasMoreElements()) {
                    TargetInfo targetInfo = targetsEnum.nextElement();
                    sourceFiles[index++].add(targetInfo.getSources()[0]
                            .toString());
                    index %= sourceFiles.length;
                }

                // setup cores/cpus
                Core[] cores = new Core[noOfCores];
                for (int j = 0; j < cores.length; j++) {
                    cores[j] = new Core(this, j, config, _objDir, sourceFiles[j],
                            relentless, monitor);
                    log("\nStarting Core " + j + " with "
                            + sourceFiles[j].size() + " source files...");
                }

                // starting cores
                for (int j = 0; j < cores.length; j++) {
                    cores[j].start();
                }
                
                // checking cores
                boolean alive = false;
                try {
                    do {
                        alive = false;
                        for (int j = 0; j < cores.length; j++) {
                            if (cores[j] != null) {
                                if (cores[j].isAlive()) {
                                    alive = true;
                                } else {
                                    Exception exception = cores[j].getException();
                                    if (exception != null) {
                                        if ((compileException == null) && (exception instanceof BuildException)) {
                                            compileException = (BuildException)exception;
                                        } else {
                                            log(cores[j].getName()+" "+exception+"                                  ", Project.MSG_ERR);
                                        }
                                        if (!relentless) {
                                            cores[j] = null;
                                            alive = false;
                                            break;
                                        }
                                    }
                                    cores[j] = null;
                                }
                            }
                        }
                        if (alive) {
                            // wait for a maximum of 5 seconds or #files*2 seconds.
                            Thread.sleep(Math.min(5000, sourceFiles[0].size()*2000));
                        }
                    } while (alive);
                } catch (InterruptedException e) {
                    break;
                }

                // killing leftovers
                for (int j = 0; j < cores.length; j++) {
                    if (cores[j] != null) {
                        cores[j].interrupt();
                        log(cores[j].getName()+" interrupted                                          ");
                    }
                }
                
                if ((!relentless) && (compileException != null)) {
                    break;
                }
                // ENDFREEHEP

                
                /* OLD CODE
                String[] sourceFiles = new String[targetsForConfig.size()];
                Enumeration targetsEnum = targetsForConfig.elements();
                index = 0;
                while (targetsEnum.hasMoreElements()) {
                    TargetInfo targetInfo = ((TargetInfo) targetsEnum
                            .nextElement());
                    sourceFiles[index++] = targetInfo.getSources()[0]
                            .toString();
                }
                try {
                    config.compile(this, _objDir, sourceFiles, relentless,
                            monitor);
                } catch (BuildException ex) {
                    if (compileException == null) {
                        compileException = ex;
                    }
                    if (!relentless)
                        break;
                }
                */
            }
            
            // BEGINFREEHEP
            progress.exit();
            try {
                progress.join();
            } catch (InterruptedException ex) {
            }
            // ENDFREEHEP

            //
            //   save the details of the object file compilation
            //     settings to disk for dependency analysis
            //
            try {
                objHistory.commit();
            } catch (IOException ex) {
                this.log("Error writing history.xml: " + ex.toString());
            }
            //
            //  if we threw a compile exception and
            //     didn't throw it at the time because
            //     we were relentless then
            //        save the history and
            //           throw the exception
            //
            if (compileException != null) {
            	if (failOnError) {
            		throw compileException;
            	} else {
                    log(compileException.getMessage(), Project.MSG_ERR);
                    return;
                }
            }
        }
        //
        //    if the dependency tree was not fully
        //      evaluated, then throw an exception
        //      since we really didn't do what we
        //      should have done
        //
        //
        if (dependencyDepth >= 0) {
            throw new BuildException(
                    "All files at depth "
                            + Integer.toString(dependencyDepth)
                            + " from changes successfully compiled.\n"
                            + "Remove or change dependencyDepth to -1 to perform full compilation.");
        }
        //
        //   if no link target then
        //       commit the history for the object files
        //           and leave the task
        if (linkTarget != null) {
            //
            //    get the history for the link target (may be the same
            //        as the object history)
            TargetHistoryTable linkHistory = getLinkHistory(objHistory);
            //
            //    see if it needs to be rebuilt
            //
            linkHistory.markForRebuild(linkTarget);
            //
            //    if it needs to be rebuilt, rebuild it
            //
            File output = linkTarget.getOutput();
            if (linkTarget.getRebuild()) {
                LinkerConfiguration linkConfig = (LinkerConfiguration) linkTarget
                        .getConfiguration();
                // BEGINFREEHEP
                log("Linking...");
                log("Starting link {" + linkConfig.getIdentifier() + "}");
                // ENDFREEHEP
                if (failOnError) {
                	linkConfig.link(this, linkTarget);
                } else {
                	try {
                    	linkConfig.link(this, linkTarget);                		
                	} catch(BuildException ex) {
                        log(ex.getMessage(), Project.MSG_ERR);
                        return;                		
                	}
                }
                if (outputFileProperty != null)
                    getProject().setProperty(outputFileProperty,
                            output.getAbsolutePath());
                linkHistory.update(linkTarget);
                try {
                    linkHistory.commit();
                } catch (IOException ex) {
                    log("Error writing link history.xml: " + ex.toString());
                }
            } else {
                if (outputFileProperty != null)
                    getProject().setProperty(outputFileProperty,
                            output.getAbsolutePath());
            }
        }
    }
    
    // BEGINFREEHEP
    class Core extends Thread {
        private CCTask task;
        private CompilerConfiguration config;
        private File objDir;
        private List<String> sourceFiles;
        private boolean relentless;
        private CCTaskProgressMonitor monitor;
        private Exception compileException;

        Core(CCTask task, int coreNo, CompilerConfiguration config, File objDir,
                List<String> set, boolean relentless,
                CCTaskProgressMonitor monitor) {
            super("Core "+coreNo);
            this.task = task;
            this.config = config;
            this.objDir = objDir;
            this.sourceFiles = set;
            this.relentless = relentless;
            this.monitor = monitor;
        }

        public Exception getException() {
            return compileException;
        }

        public void run() {
            super.run();
            try {
                String[] sources = new String[sourceFiles.size()];
                sources = sourceFiles.toArray(sources);
                
                config.compile(task, objDir, sources, relentless, monitor);
            } catch (Exception ex) {
                if (compileException == null) {
                    compileException = ex;
                }
            }
        }
    }

    // ENDFREEHEP

    // BEGINFREEHEP
    class Progress extends Thread {

        private boolean stop = false;
        private File objDir;
        private int rebuildCount;

        public Progress(File objDir, int rebuildCount) {
            this.objDir = objDir;
            this.rebuildCount = rebuildCount;
        }

        public void run() {
            if (rebuildCount < 10)
                return;
            try {
                FileFilter updatedFiles = new FileFilter() {
                    private long startTime = System.currentTimeMillis();

                    public boolean accept(File file) {
                        return file.lastModified() > startTime
                                && !file.getName().endsWith(".xml");
                    }
                };
                while (!stop) {
                    System.err.print("\r"
                            + objDir.listFiles(updatedFiles).length + " / "
                            + rebuildCount + " files compiled...");
                    System.err.print("\r");
                    System.err.flush();
                    if (!stop) {
                        Thread.sleep(5000);
                    }
                }
            } catch (InterruptedException e) {
            }
            System.err
                    .print("\r                                                                    ");
            System.err.print("\r");
            System.err.flush();
            log(Integer.toString(rebuildCount) + " files were compiled.");
        }

        public void exit() {
            stop = true;
        }

    }

    // ENDFREEHEP

    
    /**
     * Gets the dataset.
     * 
     * @return Returns a String
     */
    public String getDataset() {
        return dataset;
    }
    protected TargetHistoryTable getLinkHistory(TargetHistoryTable objHistory) {
        File outputFileDir = new File(_outfile.getParent());
        //
        //   if the output file is being produced in the link
        //        directory, then we can use the same history file
        //
        if (_objDir.equals(outputFileDir)) {
            return objHistory;
        }
        return new TargetHistoryTable(this, outputFileDir);
    }
    protected TargetInfo getLinkTarget(LinkerConfiguration linkerConfig,
            Vector<File> objectFiles, Vector<File> sysObjectFiles, 
            Map<String, TargetInfo> compileTargets, VersionInfo versionInfo) {
        //
        //  walk the compile phase targets and
        //     add those sources that have already been
        //     assigned to the linker or
        //     our output files the linker knows how to consume
        //     files the linker knows how to consume
        //
        Iterator<TargetInfo> compileTargetsIterator = compileTargets.values().iterator();
        while (compileTargetsIterator.hasNext()) {
            TargetInfo compileTarget = compileTargetsIterator.next();
            //
            //   output of compile tasks
            //
            int bid = linkerConfig.bid(compileTarget.getOutput().toString());
            if (bid > 0) {
                objectFiles.addElement(compileTarget.getOutput());
            }
        }
        File[] objectFileArray = new File[objectFiles.size()];
        objectFiles.copyInto(objectFileArray);
        File[] sysObjectFileArray = new File[sysObjectFiles.size()];
        sysObjectFiles.copyInto(sysObjectFileArray);
        String baseName = _outfile.getName();
        String[] fullNames = linkerConfig.getOutputFileNames(baseName, versionInfo);
        File outputFile = new File(_outfile.getParent(), fullNames[0]);
        return new TargetInfo(linkerConfig, objectFileArray,
                sysObjectFileArray, outputFile, linkerConfig.getRebuild());
    }
    public File getObjdir() {
        return _objDir;
    }
    public File getOutfile() {
        return _outfile;
    }
    
    public TargetDef getTargetPlatform() {
    	return null;
    }
    /**
     * This method collects a Hashtable, keyed by output file name, of
     * TargetInfo's for every source file that is specified in the filesets of
     * the <cc>and nested <compiler>elements. The TargetInfo's contain the
     * appropriate compiler configurations for their possible compilation
     *  
     */
    private Map<String, TargetInfo> getTargets(LinkerConfiguration linkerConfig,
            Vector<File> objectFiles, VersionInfo versionInfo, File outputFile) {
        // FREEHEP
        final List<String> order = new ArrayList<String>();
        
        Map<String, TargetInfo> targets = new TreeMap<String, TargetInfo>(new Comparator<String>() {
            // Order according to "order" List followed by alphabetical order
            public int compare(String f0, String f1) {
                if (order.isEmpty()) return f0.compareTo(f1);

                // Not sure why DUNS is trimming the trailing file extension - but this breaks multi compilers like resource/midl if same basename as c/cpp
                // so moved the comparison to above to avoid trimming the extension if there is no order, which half fixes the issue.

                f0 = f0.lastIndexOf('.') < 0 ? f0 : f0.substring(0, f0.lastIndexOf('.'));
                f1 = f1.lastIndexOf('.') < 0 ? f1 : f1.substring(0, f1.lastIndexOf('.'));

                // make sure we use only one core
                ordered = true;
                
                // order according to list or alphabetical
                int i0 = order.indexOf(f0);
                int i1 = order.indexOf(f1);
                
                if (i0 < 0) {
                	if (i1 < 0) {
                		// none in list
                		return f0.compareTo(f1);
                	} else {
                		// i1 in list
                		return +1;
                	}
                } else {
                	if (i1 < 0) {
                		// i0 in list
                		return -1;
                	} else {
                		// both in list
                		return i0 == i1 ? 0 : i0 < i1 ? -1 : +1;
                	}
                }
            }
        });
        
        TargetDef targetPlatform = getTargetPlatform();
        //
        //   find active (specialized) compilers
        //
        Vector<ProcessorConfiguration> biddingProcessors = new Vector<ProcessorConfiguration>(_compilers.size());
        for (int i = 0; i < _compilers.size(); i++) {
            CompilerDef currentCompilerDef = (CompilerDef) _compilers
                    .elementAt(i);
            if (currentCompilerDef.isActive()) {
                ProcessorConfiguration config = currentCompilerDef
                        .createConfiguration(this, linkType, compilerDef, 
                        		targetPlatform, versionInfo);
                //
                //   see if this processor had a precompile child element
                //
                PrecompileDef precompileDef = currentCompilerDef
                        .getActivePrecompile(compilerDef);
                ProcessorConfiguration[] localConfigs = new ProcessorConfiguration[]{config};
                //
                //    if it does then
                //
                if (precompileDef != null) {
                    File prototype = precompileDef.getPrototype();
                    //
                    //  will throw exceptions if prototype doesn't exist, etc
                    //
                    if (!prototype.exists()) {
                        throw new BuildException("prototype ("
                                + prototype.toString() + ") does not exist.");
                    }
                    if (prototype.isDirectory()) {
                        throw new BuildException("prototype ("
                                + prototype.toString() + ") is a directory.");
                    }
                    String[] exceptFiles = precompileDef.getExceptFiles();
                    //
                    //  create a precompile building and precompile using
                    //      variants of the configuration
                    //      or return null if compiler doesn't support
                    //      precompilation
                    CompilerConfiguration[] configs = ((CompilerConfiguration) config)
                            .createPrecompileConfigurations(prototype,
                                    exceptFiles);
                    if (configs != null && configs.length == 2) {
                        //
                        //   visit the precompiled file to add it into the
                        //      targets list (just like any other file if
                        //      compiler doesn't support precompilation)
                        TargetMatcher matcher = new TargetMatcher(this,
                                _objDir,
                                new ProcessorConfiguration[]{configs[0]},
                                linkerConfig, objectFiles, targets, versionInfo);
     
                        matcher.visit(new File(prototype.getParent()),
                                prototype.getName());
                        //
                        //   only the configuration that uses the
                        //      precompiled header gets added to the bidding list
                        biddingProcessors.addElement(configs[1]);
                        localConfigs = new ProcessorConfiguration[2];
                        localConfigs[0] = configs[1];
                        localConfigs[1] = config;
                    }
                }
            	// BEGINFREEHEP
                // a little trick here, the inner function needs the list to be final, so we repopulate it
                order.clear();
            	List<String> newOrder = currentCompilerDef.getOrder();
            	if (newOrder != null) {
            		order.addAll(newOrder);
            	}
                // ENDFREEHEP
            	
                //
                //   if the compiler has a fileset
                //       then allow it to add its files
                //       to the set of potential targets
                if (currentCompilerDef.hasFileSets()) {
                    TargetMatcher matcher = new TargetMatcher(this, _objDir,
                            localConfigs, linkerConfig, objectFiles, targets,
							versionInfo);
                    currentCompilerDef.visitFiles(matcher);
                }
                biddingProcessors.addElement(config);
            }
        }
        //
        //    add fallback compiler at the end
        //
        ProcessorConfiguration config = compilerDef.createConfiguration(this,
                linkType, null, targetPlatform, versionInfo);
        biddingProcessors.addElement(config);
        ProcessorConfiguration[] bidders = new ProcessorConfiguration[biddingProcessors
                .size()];
        biddingProcessors.copyInto(bidders);
        //
        //   bid out the <fileset>'s in the cctask
        //
        TargetMatcher matcher = new TargetMatcher(this, _objDir, bidders,
                linkerConfig, objectFiles, targets, versionInfo);
        compilerDef.visitFiles(matcher);
        
        if (outputFile != null && versionInfo != null) {
        	boolean isDebug = linkerConfig.isDebug();
    		try {
    			linkerConfig.getLinker().addVersionFiles(versionInfo, linkType,
    				outputFile,
					isDebug,
    				_objDir, matcher);
    		} catch(IOException ex) {
    			throw new BuildException(ex);
    		}
        }
        return targets;
    }
    /**
     * Sets the default compiler adapter. Use the "name" attribute when the
     * compiler is a supported compiler.
     * 
     * @param classname
     *            fully qualified classname which implements CompilerAdapter
     */
    public void setClassname(String classname) {
        compilerDef.setClassname(classname);
        linkerDef.setClassname(classname);
    }
    /**
     * Sets the dataset for OS/390 builds.
     * 
     * @param dataset
     *            The dataset to set
     */
    public void setDataset(String dataset) {
        this.dataset = dataset;
    }
    /**
     * Enables or disables generation of debug info.
     */
    public void setDebug(boolean debug) {
        compilerDef.setDebug(debug);
        linkerDef.setDebug(debug);     
    }
    
    /**
     * Gets debug state.
     * @return true if building for debugging
     */
    public boolean getDebug() {
    	return compilerDef.getDebug(null, 0);
    }
    
    /**
     * Deprecated.
     * 
     * Controls the depth of the dependency evaluation. Used to do a quick
     * check of changes before a full build.
     * 
     * Any negative value which will perform full dependency checking. Positive
     * values will truncate dependency checking. A value of 0 will cause only
     * those files that changed to be recompiled, a value of 1 which cause
     * files that changed or that explicitly include a file that changed to be
     * recompiled.
     * 
     * Any non-negative value will cause a BuildException to be thrown before
     * attempting a link or completing the task.
     *  
     */
    public void setDependencyDepth(int depth) {
        dependencyDepth = depth;
    }
    /**
     * Enables generation of exception handling code
     */
    public void setExceptions(boolean exceptions) {
        compilerDef.setExceptions(exceptions);
    }
    /**
     * Enables run-time type information.
     */
    public void setRtti(boolean rtti) {
        compilerDef.setRtti(rtti);
    }
    //    public LinkType getLinkType() {
    //      return linkType;
    //    }
    /**
     * Enables or disables incremental linking.
     * 
     * @param incremental
     *            new state
     */
    public void setIncremental(boolean incremental) {
        linkerDef.setIncremental(incremental);
    }
    /**
     * Set use of libtool.
     * 
     * If set to true, the "libtool " will be prepended to the command line for
     * compatible processors
     * 
     * @param libtool
     *            If true, use libtool.
     */
    public void setLibtool(boolean libtool) {
        compilerDef.setLibtool(libtool);
        linkerDef.setLibtool(libtool);
    }
    /**
     * Sets the output file type. Supported values "executable", "shared", and
     * "static".  Deprecated, specify outtype instead.
     * 
     * @deprecated
     */
    public void setLink(OutputTypeEnum outputType) {
        linkType.setOutputType(outputType);
    }

    // BEGINFREEHEP
    public void setLinkCPP(boolean linkCPP) {
        linkType.setLinkCPP(linkCPP);
    }

    public void setLinkFortran(boolean linkFortran) {
        linkType.setLinkFortran(linkFortran);
    }

    public void setLinkFortranMain(boolean linkFortranMain) {
        linkType.setLinkFortranMain(linkFortranMain);
    }
    // ENDFREEHEP

    /**
     * Enables or disables generation of multithreaded code
     * 
     * @param multi
     *            If true, generated code may be multithreaded.
     */
    public void setMultithreaded(boolean multi) {
        compilerDef.setMultithreaded(multi);
    }
    //
    //  keep near duplicate comment at CompilerDef.setName in sync
    //
    /**
     * Sets type of the default compiler and linker.
     * 
     * <table width="100%" border="1"> <thead>Supported compilers </thead>
     * <tr>
     * <td>gcc (default)</td>
     * <td>GCC C++ compiler</td>
     * </tr>
     * <tr>
     * <td>g++</td>
     * <td>GCC C++ compiler</td>
     * </tr>
     * <tr>
     * <td>c++</td>
     * <td>GCC C++ compiler</td>
     * </tr>
     * <tr>
     * <td>g77</td>
     * <td>GNU FORTRAN compiler</td>
     * </tr>
     * <tr>
     * <td>msvc</td>
     * <td>Microsoft Visual C++</td>
     * </tr>
     * <tr>
     * <td>bcc</td>
     * <td>Borland C++ Compiler</td>
     * </tr>
     * <tr>
     * <td>msrc</td>
     * <td>Microsoft Resource Compiler</td>
     * </tr>
     * <tr>
     * <td>brc</td>
     * <td>Borland Resource Compiler</td>
     * </tr>
     * <tr>
     * <td>df</td>
     * <td>Compaq Visual Fortran Compiler</td>
     * </tr>
     * <tr>
     * <td>midl</td>
     * <td>Microsoft MIDL Compiler</td>
     * </tr>
     * <tr>
     * <td>icl</td>
     * <td>Intel C++ compiler for Windows (IA-32)</td>
     * </tr>
     * <tr>
     * <td>ecl</td>
     * <td>Intel C++ compiler for Windows (IA-64)</td>
     * </tr>
     * <tr>
     * <td>icc</td>
     * <td>Intel C++ compiler for Linux (IA-32)</td>
     * </tr>
     * <tr>
     * <td>ecc</td>
     * <td>Intel C++ compiler for Linux (IA-64)</td>
     * </tr>
     * <tr>
     * <td>CC</td>
     * <td>Sun ONE C++ compiler</td>
     * </tr>
     * <tr>
     * <td>aCC</td>
     * <td>HP aC++ C++ Compiler</td>
     * </tr>
     * <tr>
     * <td>os390</td>
     * <td>OS390 C Compiler</td>
     * </tr>
     * <tr>
     * <td>os400</td>
     * <td>Icc Compiler</td>
     * </tr>
     * <tr>
     * <td>sunc89</td>
     * <td>Sun C89 C Compiler</td>
     * </tr>
     * <tr>
     * <td>xlC</td>
     * <td>VisualAge C Compiler</td>
     * </tr>
     * <tr>
     * <td>uic</td>
     * <td>Qt user interface compiler (creates .h, .cpp and moc_*.cpp files).</td>
     * </tr>
     * <tr>
     * <td>moc</td>
     * <td>Qt meta-object compiler</td>
     * </tr>
     * <tr>
     * <td>xpidl</td>
     * <td>Mozilla xpidl compiler (creates .h and .xpt files).</td>
     * </tr>
     * <tr>
     * <td>wcl</td>
     * <td>OpenWatcom C/C++ compiler</td>
     * </tr>
     * <tr>
     * <td>wfl</td>
     * <td>OpenWatcom FORTRAN compiler</td>
     * </tr>
     * </table>
     *  
     */
    public void setName(CompilerEnum name) {
        compilerDef.setName(name);
        Processor compiler = compilerDef.getProcessor();
        Linker linker = compiler.getLinker(linkType);
        linkerDef.setProcessor(linker);
    }
    /**
     * Do not propagate old environment when new environment variables are
     * specified.
     */
    public void setNewenvironment(boolean newenv) {
        compilerDef.setNewenvironment(newenv);
        linkerDef.setNewenvironment(newenv);
    }
    /**
     * Sets the destination directory for object files.
     * 
     * Generally this should be a property expression that evaluates to
     * distinct debug and release object file directories.
     * 
     * @param dir
     *            object directory
     */
    public void setObjdir(File dir) {
        if (dir == null) {
            throw new NullPointerException("dir");
        }
        _objDir = dir;
    }
    /**
     * Sets the output file name. If not specified, the task will only compile
     * files and not attempt to link. If an extension is not specified, the
     * task may use a system appropriate extension and prefix, for example,
     * outfile="example" may result in "libexample.so" being created.
     * 
     * @param outfile
     *            output file name
     */
    public void setOutfile(File outfile) {
        //
        //   if file name was empty, skip link step
        //
        if (outfile == null || outfile.toString().length() > 0) {
            _outfile = outfile;
        }
    }
    /**
     * Specifies the name of a property to set with the physical filename that
     * is produced by the linker
     */
    public void setOutputFileProperty(String outputFileProperty) {
        this.outputFileProperty = outputFileProperty;
    }
    /**
     * Sets the output file type. Supported values "executable", "shared", and
     * "static".
     */
    public void setOuttype(OutputTypeEnum outputType) {
        linkType.setOutputType(outputType);
    }
    
    /**
     * Gets output type.
     * @return output type
     */
    public String getOuttype() {
    	return linkType.getOutputType();
    }
    
    /**
     * Sets the project.
     */
    public void setProject(Project project) {
        super.setProject(project);
        compilerDef.setProject(project);
        linkerDef.setProject(project);
    }
    /**
     * If set to true, all files will be rebuilt.
     * 
     * @param rebuildAll If true, all files will be rebuilt. If false, up to
     * date files will not be rebuilt.
     */
    public void setRebuild(boolean rebuildAll) {
        compilerDef.setRebuild(rebuildAll);
        linkerDef.setRebuild(rebuildAll);
    }
    /**
     * If set to true, compilation errors will not stop the task until all
     * files have been attempted.
     * 
     * @param relentless
     *            If true, don't stop on the first compilation error
     *  
     */
    public void setRelentless(boolean relentless) {
        this.relentless = relentless;
    }
    /**
     * Sets the type of runtime library, possible values "dynamic", "static".
     */
    public void setRuntime(RuntimeType rtlType) {
        linkType.setStaticRuntime((rtlType.getIndex() == 1));
    }
    /**
     * Sets the nature of the subsystem under which that the program will
     * execute.
     * 
     * <table width="100%" border="1"> <thead>Supported subsystems </thead>
     * <tr>
     * <td>gui</td>
     * <td>Graphical User Interface</td>
     * </tr>
     * <tr>
     * <td>console</td>
     * <td>Command Line Console</td>
     * </tr>
     * <tr>
     * <td>other</td>
     * <td>Other</td>
     * </tr>
     * </table>
     * 
     * @param subsystem
     *            subsystem
     * @throws NullPointerException
     *             if subsystem is null
     */
    public void setSubsystem(SubsystemEnum subsystem) {
        if (subsystem == null) {
            throw new NullPointerException("subsystem");
        }
        linkType.setSubsystem(subsystem);
    }
    
    /**
     * Gets subsystem name.
     * @return Subsystem name
     */
    public String getSubsystem() {
    	return linkType.getSubsystem();
    }
    
    /**
     * Enumerated attribute with the values "none", "severe", "default",
     * "production", "diagnostic", and "aserror".
     */
    public void setWarnings(WarningLevelEnum level) {
        compilerDef.setWarnings(level);
    }

    // BEGINFREEHEP
    public void setMaxCores(int maxCores) {
        this.maxCores = maxCores;
    }

    public int getMaxCores() {
        return maxCores;
    }

    // ENDFREEHEP

    /**
     * Indicates whether the build will continue
     * even if there are compilation errors; defaults to true.
     * @param fail if true halt the build on failure
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * Gets the failonerror flag.
     * @return the failonerror flag
     */
    public boolean getFailonerror() {
        return failOnError;
    }
    /**
     * Adds a target definition or reference (Non-functional prototype).
     * 
     * @param target
     *            target
     * @throws NullPointerException
     *             if compiler is null
     */
    public void addConfiguredTarget(TargetDef target) {
        if (target == null) {
            throw new NullPointerException("target");
        }
        target.setProject(getProject());
        targetPlatforms.addElement(target);
    }
    /**
     * Adds a distributer definition or reference (Non-functional prototype).
     * 
     * @param distributer
     *            distributer
     * @throws NullPointerException
     *             if compiler is null
     */
    public void addConfiguredDistributer(DistributerDef distributer) {
        if (distributer == null) {
            throw new NullPointerException("distributer");
        }
        distributer.setProject(getProject());
        distributers.addElement(distributer);
    }
    /**
     * Sets optimization.
     * @param optimization
     */
    public void setOptimize(OptimizationEnum optimization) {
    	compilerDef.setOptimize(optimization);
    }
    
    /**
     * Adds desriptive version information to be included in the
     * generated file.  The first active version info block will
     * be used.
     */
    public void addConfiguredVersioninfo(VersionInfo newVersionInfo) {
        newVersionInfo.setProject(this.getProject());
        versionInfos.addElement(newVersionInfo);
    }

    /**
     * Set commandLogLevel
     * 
     * ( CUtil.runCommand() will honor this... )
     * 
     * @parameter commandLogLevel  The log-level for command-logs, default is MSG_VERBOSE.
     */
    public void setCommandLogLevel( int commandLogLevel ) {
	this.commandLogLevel = commandLogLevel;
    }

    /**
     *  Get the commandLogLevel
     * 
     * @return The current commandLogLevel
     */
    public int getCommandLogLevel() {
	return commandLogLevel;
    }

}
