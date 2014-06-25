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
package com.github.maven_nar.cpptasks.types;
import java.io.File;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;

import com.github.maven_nar.cpptasks.CUtil;
import com.github.maven_nar.cpptasks.FileVisitor;
import com.github.maven_nar.cpptasks.compiler.Linker;
/**
 * A set of library names. Libraries can also be added to a link by specifying
 * them in a fileset.
 * 
 * For most Unix-like compilers, libset will result in a series of -l and -L
 * linker arguments. For Windows compilers, the library names will be used to
 * locate the appropriate library files which will be added to the linkers
 * input file list as if they had been specified in a fileset.
 * 
 * @author Mark A Russell <a
 *         href="mailto:mark_russell@csgsystems.com">mark_russell@csg_systems.com
 *         </a>
 * @author Adam Murdoch
 * @author Curt Arnold
 */
public class LibrarySet extends DataType {
    private String dataset;
    private boolean explicitCaseSensitive;
    private String ifCond;
    private String[] libnames;
    private final FileSet set = new FileSet();
    private String unlessCond;
    private LibraryTypeEnum libraryType;
    public LibrarySet() {
        libnames = new String[0];
    }
    public void execute() throws org.apache.tools.ant.BuildException {
        throw new org.apache.tools.ant.BuildException(
                "Not an actual task, but looks like one for documentation purposes");
    }
    /**
     * Gets the dataset. Used on OS390 if the libs are in a dataset.
     * 
     * @return Returns a String
     */
    public String getDataset() {
        if (isReference()) {
            LibrarySet master = ((LibrarySet) getCheckedRef(LibrarySet.class, "LibrarySet"));
            return master.getDataset();
        }
        return dataset;
    }
    public File getDir(final Project project) {
        if (isReference()) {
            LibrarySet master = ((LibrarySet) getCheckedRef(LibrarySet.class, "LibrarySet"));
            return master.getDir(project);
        }
        return set.getDir(project);
    }
    protected FileSet getFileSet() {
        if (isReference()) {
            LibrarySet master = ((LibrarySet) getCheckedRef(LibrarySet.class, "LibrarySet"));
            return master.getFileSet();
        }
        return set;
    }
    public String[] getLibs() {
        if (isReference()) {
            LibrarySet master = ((LibrarySet) getCheckedRef(LibrarySet.class, "LibrarySet"));
            return master.getLibs();
        }
        String[] retval = (String[]) libnames.clone();
        return retval;
    }
    
    /**
     * Gets preferred library type
     * 
     * @return library type, may be null.
     */
    public LibraryTypeEnum getType() {
        if (isReference()) {
            LibrarySet master = ((LibrarySet) getCheckedRef(LibrarySet.class, "LibrarySet"));
            return master.getType();
        }
    	return libraryType;
    }
    /**
     * Returns true if the define's if and unless conditions (if any) are
     * satisfied.
     */
    public boolean isActive(final org.apache.tools.ant.Project p) {
        if (p == null) {
            throw new NullPointerException("p");
        }
        if (ifCond != null) {
            String ifValue = p.getProperty(ifCond);
            if (ifValue != null) {
                if (ifValue.equals("no") || ifValue.equals("false")) {
                    throw new BuildException(
                            "property "
                                    + ifCond
                                    + " used as if condition has value "
                                    + ifValue
                                    + " which suggests a misunderstanding of if attributes");
                }
            } else {
                return false;
            }
        }
        if (unlessCond != null) {
            String unlessValue = p.getProperty(unlessCond);
            if (unlessValue != null) {
                if (unlessValue.equals("no") || unlessValue.equals("false")) {
                    throw new BuildException(
                            "property "
                                    + unlessCond
                                    + " used as unless condition has value "
                                    + unlessValue
                                    + " which suggests a misunderstanding of unless attributes");
                }
                return false;
            }
        }
        if (isReference()) {
            LibrarySet master = ((LibrarySet) getCheckedRef(LibrarySet.class, "LibrarySet"));
            return master.isActive(project);
        }
        if (libnames.length == 0) {
            p.log("libnames not specified or empty.", Project.MSG_WARN);
            return false;
        }
        return true;
    }
    /**
     * Sets case sensitivity of the file system. If not set, will default to
     * the linker's case sensitivity.
     * 
     * @param isCaseSensitive
     *            "true"|"on"|"yes" if file system is case sensitive,
     *            "false"|"off"|"no" when not.
     */
    public void setCaseSensitive(final boolean isCaseSensitive) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        explicitCaseSensitive = true;
        set.setCaseSensitive(isCaseSensitive);
    }
    /**
     * Sets the dataset. Used on OS390 if the libs are in a dataset.
     * 
     * @param dataset
     *            The dataset to set
     */
    public void setDataset(final String dataset) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.dataset = dataset;
    }
    /**
     * Library directory.
     * 
     * @param dir
     *            library directory
     *  
     */
    public void setDir(final File dir) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        set.setDir(dir);
    }
    /**
     * Sets the property name for the 'if' condition.
     * 
     * The library set will be ignored unless the property is defined.
     * 
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") will throw an exception when
     * evaluated.
     * 
     * @param propName
     *            property name
     */
    public void setIf(String propName) {
        ifCond = propName;
    }
    /**
     * Comma-separated list of library names without leading prefixes, such as
     * "lib", or extensions, such as ".so" or ".a".
     *  
     */
    public void setLibs(final CUtil.StringArrayBuilder libs) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        libnames = libs.getValue();
        //
        //   earlier implementations would warn of suspicious library names
        //    (like libpthread for pthread or kernel.lib for kernel).
        //    visitLibraries now provides better feedback and ld type linkers
        //    should provide adequate feedback so the check here is not necessary.
    }
    public void setProject(final Project project) {
        set.setProject(project);
        super.setProject(project);
    }
    /**
     * Set the property name for the 'unless' condition.
     * 
     * If named property is set, the library set will be ignored.
     * 
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") of the behavior will throw an
     * exception when evaluated.
     * 
     * @param propName
     *            name of property
     */
    public void setUnless(String propName) {
        unlessCond = propName;
    }
    
    /**
     * Sets the preferred library type. Supported values "shared", "static", and
     * "framework".  "framework" is equivalent to "shared" on non-Darwin platforms. 
     */
    public void setType(final LibraryTypeEnum type) {
        if (isReference()) {
            throw tooManyAttributes();
        }
    	this.libraryType = type;
    }
    
    public void visitLibraries(final Project project,
                               final Linker linker,
                               final File[] libpath,
                               final FileVisitor visitor) throws BuildException {
        if (isReference()) {
            LibrarySet master = ((LibrarySet) getCheckedRef(LibrarySet.class, "LibrarySet"));
            master.visitLibraries(project, linker, libpath, visitor);
        }
        //
        //  if there was a libs attribute then
        //     add the corresponding patterns to the FileSet
        //
        if (libnames != null) {
            for (int i = 0; i < libnames.length; i++) {
                String[] patterns = linker.getLibraryPatterns(new String[] { libnames[i] }, libraryType);
                if (patterns.length > 0) {
                    FileSet localSet = (FileSet) set.clone();
                    //
                    //   unless explicitly set
                    //      will default to the linker case sensitivity
                    //
                    if (!explicitCaseSensitive) {
                        boolean linkerCaseSensitive = linker.isCaseSensitive();
                        localSet.setCaseSensitive(linkerCaseSensitive);
                    }
                    //
                    //   add all the patterns for this libname
                    //
                    for (int j = 0; j < patterns.length; j++) {
                        PatternSet.NameEntry entry = localSet.createInclude();
                        entry.setName(patterns[j]);
                    }
                   int matches = 0;
		           //
		           //  if there was no specified directory then
		           //     run through the libpath backwards
		           //
		           if (localSet.getDir(project) == null) {
		               //
		               //  scan libpath in reverse order
		               //     to give earlier entries priority
		               //
		               for (int j = libpath.length - 1; j >= 0; j--) {
		                   FileSet clone = (FileSet) localSet.clone();
		                   clone.setDir(libpath[j]);
		                   DirectoryScanner scanner = clone.getDirectoryScanner(project);
		                   File basedir = scanner.getBasedir();
		                   String[] files = scanner.getIncludedFiles();
                           matches += files.length;
                           for (int k = 0; k < files.length; k++) {
		                       visitor.visit(basedir, files[k]);
		                   }
		               }
		           } else {
		               DirectoryScanner scanner = localSet.getDirectoryScanner(project);
		               File basedir = scanner.getBasedir();
		               String[] files = scanner.getIncludedFiles();
                       matches += files.length;
		               for (int k = 0; k < files.length; k++) {
		                   visitor.visit(basedir, files[k]);
		               }
		           }
                   //
                   //  TODO: following section works well for Windows
                   //      style linkers but unnecessary fails
                   //     Unix style linkers.  Will need to revisit.
                   //
                   if (matches == 0 && false) {
                      StringBuffer msg = new StringBuffer("No file matching ");
                       if (patterns.length == 1) {
                           msg.append("pattern (");
                           msg.append(patterns[0]);
                           msg.append(")");
                       } else {
                           msg.append("patterns (\"");
                           msg.append(patterns[0]);
                           for (int k = 1; k < patterns.length; k++) {
                               msg.append(", ");
                               msg.append(patterns[k]);
                           }
                           msg.append(")");
                       }
                       msg.append(" for library name \"");
                       msg.append(libnames[i]);
                       msg.append("\" was found.");
                      throw new BuildException(msg.toString());
                   }
                }
            }
        }
    }
}
