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
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar.cpptasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.github.maven_nar.cpptasks.compiler.CompilerConfiguration;

/**
 * @author Curt Arnold
 */
public final class DependencyTable {
  /**
   * This class handles populates the TargetHistory hashtable in response to
   * SAX parse events
   */
  private class DependencyTableHandler extends DefaultHandler {
    private final File baseDir;
    private final DependencyTable dependencyTable;
    private String includePath;
    private final Vector includes;
    private String source;
    private long sourceLastModified;
    private final Vector sysIncludes;

    /**
     * Constructor
     *
     * @param history
     *          hashtable of TargetHistory keyed by output name
     * @param outputFiles
     *          existing files in output directory
     */
    private DependencyTableHandler(final DependencyTable dependencyTable, final File baseDir) {
      this.dependencyTable = dependencyTable;
      this.baseDir = baseDir;
      this.includes = new Vector();
      this.sysIncludes = new Vector();
      this.source = null;
    }

    @Override
    public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
      //
      // if </source> then
      // create Dependency object and add to hashtable
      // if corresponding source file exists and
      // has the same timestamp
      //
      if (qName.equals("source")) {
        if (this.source != null && this.includePath != null) {
          final File existingFile = new File(this.baseDir, this.source);
          //
          // if the file exists and the time stamp is right
          // preserve the dependency info
          if (existingFile.exists()) {
            //
            // would have expected exact matches
            // but was seeing some unexpected difference by
            // a few tens of milliseconds, as long
            // as the times are within a second
            final long existingLastModified = existingFile.lastModified();
            if (!CUtil.isSignificantlyAfter(existingLastModified, this.sourceLastModified)
                && !CUtil.isSignificantlyBefore(existingLastModified, this.sourceLastModified)) {
              final DependencyInfo dependInfo = new DependencyInfo(this.includePath, this.source,
                  this.sourceLastModified, this.includes, this.sysIncludes);
              this.dependencyTable.putDependencyInfo(this.source, dependInfo);
            }
          }
          this.source = null;
          this.includes.setSize(0);
        }
      } else {
        //
        // this causes any <source> elements outside the
        // scope of an <includePath> to be discarded
        //
        if (qName.equals("includePath")) {
          this.includePath = null;
        }
      }
    }

    /**
     * startElement handler
     */
    @Override
    public void startElement(final String namespaceURI, final String localName, final String qName,
        final Attributes atts) throws SAXException {
      //
      // if includes, then add relative file name to vector
      //
      if (qName.equals("include")) {
        this.includes.addElement(atts.getValue("file"));
      } else {
        if (qName.equals("sysinclude")) {
          this.sysIncludes.addElement(atts.getValue("file"));
        } else {
          //
          // if source then
          // capture source file name,
          // modification time and reset includes vector
          //
          if (qName.equals("source")) {
            this.source = atts.getValue("file");
            this.sourceLastModified = Long.parseLong(atts.getValue("lastModified"), 16);
            this.includes.setSize(0);
            this.sysIncludes.setSize(0);
          } else {
            if (qName.equals("includePath")) {
              this.includePath = atts.getValue("signature");
            }
          }
        }
      }
    }
  }

  public abstract class DependencyVisitor {
    /**
     * Previews all the children of this source file.
     *
     * May be called multiple times as DependencyInfo's for children are
     * filled in.
     *
     * @return true to continue towards recursion into included files
     */
    public abstract boolean preview(DependencyInfo parent, DependencyInfo[] children);

    /**
     * Called if the dependency depth exhausted the stack.
     */
    public abstract void stackExhausted();

    /**
     * Visits the dependency info.
     *
     * @return true to continue towards recursion into included files
     */
    public abstract boolean visit(DependencyInfo dependInfo);
  }

  public class TimestampChecker extends DependencyVisitor {
    private boolean noNeedToRebuild;
    private final long outputLastModified;
    private final boolean rebuildOnStackExhaustion;

    public TimestampChecker(final long outputLastModified, final boolean rebuildOnStackExhaustion) {
      this.outputLastModified = outputLastModified;
      this.noNeedToRebuild = true;
      this.rebuildOnStackExhaustion = rebuildOnStackExhaustion;
    }

    public boolean getMustRebuild() {
      return !this.noNeedToRebuild;
    }

    @Override
    public boolean preview(final DependencyInfo parent, final DependencyInfo[] children) {
      // BEGINFREEHEP
      // int withCompositeTimes = 0;
      // long parentCompositeLastModified = parent.getSourceLastModified();
      // ENDFREEHEP
      for (final DependencyInfo element : children) {
        if (element != null) {
          //
          // expedient way to determine if a child forces us to
          // rebuild
          //
          visit(element);
          // BEGINFREEHEP
          // long childCompositeLastModified = children[i]
          // .getCompositeLastModified();
          // if (childCompositeLastModified != Long.MIN_VALUE) {
          // withCompositeTimes++;
          // if (childCompositeLastModified > parentCompositeLastModified) {
          // parentCompositeLastModified = childCompositeLastModified;
          // }
          // }
          // ENDFREEHEP
        }
      }
      // BEGINFREEHEP
      // if (withCompositeTimes == children.length) {
      // parent.setCompositeLastModified(parentCompositeLastModified);
      // }
      // ENDFREEHEP
      //
      // may have been changed by an earlier call to visit()
      //
      return this.noNeedToRebuild;
    }

    @Override
    public void stackExhausted() {
      if (this.rebuildOnStackExhaustion) {
        this.noNeedToRebuild = false;
      }
    }

    @Override
    public boolean visit(final DependencyInfo dependInfo) {
      if (this.noNeedToRebuild && CUtil.isSignificantlyAfter(dependInfo.getSourceLastModified(), this.outputLastModified)) {
          // FREEHEP
          // ||
          // CUtil.isSignificantlyAfter(dependInfo.getCompositeLastModified(),
          // outputLastModified)) {
          this.noNeedToRebuild = false;
      }
      //
      // only need to process the children if
      // it has not yet been determined whether
      // we need to rebuild and the composite modified time
      // has not been determined for this file
      return this.noNeedToRebuild;
      // FREEHEP
      // && dependInfo.getCompositeLastModified() == Long.MIN_VALUE;
    }
  }

  private final/* final */File baseDir;
  private String baseDirPath;
  /**
   * a hashtable of DependencyInfo[] keyed by output file name
   */
  private final Hashtable dependencies = new Hashtable();
  /** The file the cache was loaded from. */
  private final/* final */File dependenciesFile;
  /** Flag indicating whether the cache should be written back to file. */
  private boolean dirty;

  /**
   * Creates a target history table from dependencies.xml in the prject
   * directory, if it exists. Otherwise, initializes the dependencies empty.
   *
   * @param baseDir
   *          output directory for task
   */
  public DependencyTable(final File baseDir) {
    if (baseDir == null) {
      throw new NullPointerException("baseDir");
    }
    this.baseDir = baseDir;
    try {
      this.baseDirPath = baseDir.getCanonicalPath();
    } catch (final IOException ex) {
      this.baseDirPath = baseDir.toString();
    }
    this.dirty = false;
    //
    // load any existing dependencies from file
    this.dependenciesFile = new File(baseDir, "dependencies.xml");
  }

  public void commit(final CCTask task) {
    //
    // if not dirty, no need to update file
    //
    if (this.dirty) {
      //
      // walk through dependencies to get vector of include paths
      // identifiers
      //
      final Vector includePaths = getIncludePaths();
      //
      //
      // write dependency file
      //
      try {
        final FileOutputStream outStream = new FileOutputStream(this.dependenciesFile);
        OutputStreamWriter streamWriter;
        //
        // Early VM's may not have UTF-8 support
        // fallback to default code page which
        // "should" be okay unless there are
        // non ASCII file names
        String encodingName = "UTF-8";
        try {
          streamWriter = new OutputStreamWriter(outStream, "UTF-8");
        } catch (final UnsupportedEncodingException ex) {
          streamWriter = new OutputStreamWriter(outStream);
          encodingName = streamWriter.getEncoding();
        }
        final BufferedWriter writer = new BufferedWriter(streamWriter);
        writer.write("<?xml version='1.0' encoding='");
        writer.write(encodingName);
        writer.write("'?>\n");
        writer.write("<dependencies>\n");
        final StringBuffer buf = new StringBuffer();
        final Enumeration includePathEnum = includePaths.elements();
        while (includePathEnum.hasMoreElements()) {
          writeIncludePathDependencies((String) includePathEnum.nextElement(), writer, buf);
        }
        writer.write("</dependencies>\n");
        writer.close();
        this.dirty = false;
      } catch (final IOException ex) {
        task.log("Error writing " + this.dependenciesFile.toString() + ":" + ex.toString());
      }
    }
  }

  /**
   * Returns an enumerator of DependencyInfo's
   */
  public Enumeration elements() {
    return this.dependencies.elements();
  }

  /**
   * This method returns a DependencyInfo for the specific source file and
   * include path identifier
   *
   */
  public DependencyInfo getDependencyInfo(final String sourceRelativeName, final String includePathIdentifier) {
    DependencyInfo dependInfo = null;
    final DependencyInfo[] dependInfos = (DependencyInfo[]) this.dependencies.get(sourceRelativeName);
    if (dependInfos != null) {
      for (final DependencyInfo dependInfo2 : dependInfos) {
        dependInfo = dependInfo2;
        if (dependInfo.getIncludePathIdentifier().equals(includePathIdentifier)) {
          return dependInfo;
        }
      }
    }
    return null;
  }

  private Vector getIncludePaths() {
    final Vector includePaths = new Vector();
    DependencyInfo[] dependInfos;
    final Enumeration dependenciesEnum = this.dependencies.elements();
    while (dependenciesEnum.hasMoreElements()) {
      dependInfos = (DependencyInfo[]) dependenciesEnum.nextElement();
      for (final DependencyInfo dependInfo : dependInfos) {
        boolean matchesExisting = false;
        final String dependIncludePath = dependInfo.getIncludePathIdentifier();
        final Enumeration includePathEnum = includePaths.elements();
        while (includePathEnum.hasMoreElements()) {
          if (dependIncludePath.equals(includePathEnum.nextElement())) {
            matchesExisting = true;
            break;
          }
        }
        if (!matchesExisting) {
          includePaths.addElement(dependIncludePath);
        }
      }
    }
    return includePaths;
  }

  public void load() throws IOException, ParserConfigurationException, SAXException {
    this.dependencies.clear();
    if (this.dependenciesFile.exists()) {
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      final SAXParser parser = factory.newSAXParser();
      parser.parse(this.dependenciesFile, new DependencyTableHandler(this, this.baseDir));
      this.dirty = false;
    }
  }

  /**
   * Determines if the specified target needs to be rebuilt.
   *
   * This task may result in substantial IO as files are parsed to determine
   * their dependencies
   */
  public boolean needsRebuild(final CCTask task, final TargetInfo target, final int dependencyDepth) {
    // look at any files where the compositeLastModified
    // is not known, but the includes are known
    //
    boolean mustRebuild = false;
    final CompilerConfiguration compiler = (CompilerConfiguration) target.getConfiguration();
    final String includePathIdentifier = compiler.getIncludePathIdentifier();
    final File[] sources = target.getSources();
    final DependencyInfo[] dependInfos = new DependencyInfo[sources.length];
    final long outputLastModified = target.getOutput().lastModified();
    //
    // try to solve problem using existing dependency info
    // (not parsing any new files)
    //
    DependencyInfo[] stack = new DependencyInfo[50];
    boolean rebuildOnStackExhaustion = true;
    if (dependencyDepth >= 0) {
      if (dependencyDepth < 50) {
        stack = new DependencyInfo[dependencyDepth];
      }
      rebuildOnStackExhaustion = false;
    }
    final TimestampChecker checker = new TimestampChecker(outputLastModified, rebuildOnStackExhaustion);
    for (int i = 0; i < sources.length && !mustRebuild; i++) {
      final File source = sources[i];
      final String relative = CUtil.getRelativePath(this.baseDirPath, source);
      DependencyInfo dependInfo = getDependencyInfo(relative, includePathIdentifier);
      if (dependInfo == null) {
        task.log("Parsing " + relative, Project.MSG_VERBOSE);
        dependInfo = parseIncludes(task, compiler, source);
      }
      walkDependencies(task, dependInfo, compiler, stack, checker);
      mustRebuild = checker.getMustRebuild();
    }
    return mustRebuild;
  }

  public DependencyInfo parseIncludes(final CCTask task, final CompilerConfiguration compiler, final File source) {
    final DependencyInfo dependInfo = compiler.parseIncludes(task, this.baseDir, source);
    final String relativeSource = CUtil.getRelativePath(this.baseDirPath, source);
    putDependencyInfo(relativeSource, dependInfo);
    return dependInfo;
  }

  private void putDependencyInfo(final String key, final DependencyInfo dependInfo) {
    //
    // optimistic, add new value
    //
    final DependencyInfo[] old = (DependencyInfo[]) this.dependencies.put(key, new DependencyInfo[] {
      dependInfo
    });
    this.dirty = true;
    //
    // something was already there
    //
    if (old != null) {
      //
      // see if the include path matches a previous entry
      // if so replace it
      final String includePathIdentifier = dependInfo.getIncludePathIdentifier();
      for (int i = 0; i < old.length; i++) {
        final DependencyInfo oldDepend = old[i];
        if (oldDepend.getIncludePathIdentifier().equals(includePathIdentifier)) {
          old[i] = dependInfo;
          this.dependencies.put(key, old);
          return;
        }
      }
      //
      // no match prepend the new entry to the array
      // of dependencies for the file
      final DependencyInfo[] combined = new DependencyInfo[old.length + 1];
      combined[0] = dependInfo;
      System.arraycopy(old, 0, combined, 1, old.length);
      this.dependencies.put(key, combined);
    }
    return;
  }

  public void walkDependencies(final CCTask task, final DependencyInfo dependInfo,
      final CompilerConfiguration compiler, final DependencyInfo[] stack, final DependencyVisitor visitor)
      throws BuildException {
    // BEGINFREEHEP
    if (dependInfo.hasTag(visitor)) {
      return;
    }
    dependInfo.setTag(visitor);
    // ENDFREEHEP
    //
    // visit this node
    // if visit returns true then
    // visit the referenced include and sysInclude dependencies
    //
    if (visitor.visit(dependInfo)) {
      // BEGINFREEHEP
      // //
      // // find first null entry on stack
      // //
      // int stackPosition = -1;
      // for (int i = 0; i < stack.length; i++) {
      // if (stack[i] == null) {
      // stackPosition = i;
      // stack[i] = dependInfo;
      // break;
      // } else {
      // //
      // // if we have appeared early in the calling history
      // // then we didn't exceed the criteria
      // if (stack[i] == dependInfo) {
      // return;
      // }
      // }
      // }
      // if (stackPosition == -1) {
      // visitor.stackExhausted();
      // return;
      // }
      // ENDFREEHEP
      //
      // locate dependency infos
      //
      final String[] includes = dependInfo.getIncludes();
      final String includePathIdentifier = compiler.getIncludePathIdentifier();
      final DependencyInfo[] includeInfos = new DependencyInfo[includes.length];
      for (int i = 0; i < includes.length; i++) {
        final DependencyInfo includeInfo = getDependencyInfo(includes[i], includePathIdentifier);
        includeInfos[i] = includeInfo;
      }
      //
      // preview with only the already available dependency infos
      //
      if (visitor.preview(dependInfo, includeInfos)) {
        //
        // now need to fill in the missing DependencyInfos
        //
        int missingCount = 0;
        for (int i = 0; i < includes.length; i++) {
          if (includeInfos[i] == null) {
            missingCount++;
            task.log("Parsing " + includes[i], Project.MSG_VERBOSE);
            //
            // If the include filepath is relative
            // then anchor it the base directory
            File src = new File(includes[i]);
            if (!src.isAbsolute()) {
              src = new File(this.baseDir, includes[i]);
            }
            final DependencyInfo includeInfo = parseIncludes(task, compiler, src);
            includeInfos[i] = includeInfo;
          }
        }
        //
        // if it passes a review the second time
        // then recurse into all the children
        if (missingCount == 0 || visitor.preview(dependInfo, includeInfos)) {
          //
          // recurse into
          //
          for (final DependencyInfo includeInfo : includeInfos) {
            // Darren Sargent 23Oct2008
            // only recurse for direct includes of current source
            // file
            if (includeInfo.getSource().contains(File.separatorChar + "src" + File.separatorChar + "main")
                || includeInfo.getSource().contains(File.separatorChar + "src" + File.separatorChar + "test")) {
              task.log("Walking dependencies for " + includeInfo.getSource(), Project.MSG_VERBOSE);
              walkDependencies(task, includeInfo, compiler, stack, visitor);
            }
          }
        }
      }
      // FREEHEP
      // stack[stackPosition] = null;
    }
  }

  private void
      writeDependencyInfo(final BufferedWriter writer, final StringBuffer buf, final DependencyInfo dependInfo)
          throws IOException {
    final String[] includes = dependInfo.getIncludes();
    final String[] sysIncludes = dependInfo.getSysIncludes();
    //
    // if the includes have not been evaluted then
    // it is not worth our time saving it
    // and trying to distiguish between files with
    // no dependencies and those with undetermined dependencies
    buf.setLength(0);
    buf.append("      <source file=\"");
    buf.append(CUtil.xmlAttribEncode(dependInfo.getSource()));
    buf.append("\" lastModified=\"");
    buf.append(Long.toHexString(dependInfo.getSourceLastModified()));
    buf.append("\">\n");
    writer.write(buf.toString());
    for (final String include : includes) {
      buf.setLength(0);
      buf.append("         <include file=\"");
      buf.append(CUtil.xmlAttribEncode(include));
      buf.append("\"/>\n");
      writer.write(buf.toString());
    }
    for (final String sysInclude : sysIncludes) {
      buf.setLength(0);
      buf.append("         <sysinclude file=\"");
      buf.append(CUtil.xmlAttribEncode(sysInclude));
      buf.append("\"/>\n");
      writer.write(buf.toString());
    }
    writer.write("      </source>\n");
    return;
  }

  private void writeIncludePathDependencies(final String includePathIdentifier, final BufferedWriter writer,
      final StringBuffer buf) throws IOException {
    //
    // include path element
    //
    buf.setLength(0);
    buf.append("   <includePath signature=\"");
    buf.append(CUtil.xmlAttribEncode(includePathIdentifier));
    buf.append("\">\n");
    writer.write(buf.toString());
    final Enumeration dependenciesEnum = this.dependencies.elements();
    while (dependenciesEnum.hasMoreElements()) {
      final DependencyInfo[] dependInfos = (DependencyInfo[]) dependenciesEnum.nextElement();
      for (final DependencyInfo dependInfo : dependInfos) {
        //
        // if this is for the same include path
        // then output the info
        if (dependInfo.getIncludePathIdentifier().equals(includePathIdentifier)) {
          writeDependencyInfo(writer, buf, dependInfo);
        }
      }
    }
    writer.write("   </includePath>\n");
  }
}
