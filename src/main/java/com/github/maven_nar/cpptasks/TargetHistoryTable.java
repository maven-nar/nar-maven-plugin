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
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;

/**
 * A history of the compiler and linker settings used to build the files in the
 * same directory as the history.
 *
 * @author Curt Arnold
 */
public final class TargetHistoryTable {
  /**
   * This class handles populates the TargetHistory hashtable in response to
   * SAX parse events
   */
  private class TargetHistoryTableHandler extends DefaultHandler {
    private final File baseDir;
    private String config;
    private final Hashtable<String, TargetHistory> history;
    private String output;
    private long outputLastModified;
    private final Vector<SourceHistory> sources = new Vector<>();

    /**
     * Constructor
     *
     * @param history
     *          hashtable of TargetHistory keyed by output name
     */
    private TargetHistoryTableHandler(final Hashtable<String, TargetHistory> history, final File baseDir) {
      this.history = history;
      this.config = null;
      this.output = null;
      this.baseDir = baseDir;
    }

    @Override
    public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
      //
      // if </target> then
      // create TargetHistory object and add to hashtable
      // if corresponding output file exists and
      // has the same timestamp
      //
      if (qName.equals("target")) {
        if (this.config != null && this.output != null) {
          final File existingFile = new File(this.baseDir, this.output);
          //
          // if the corresponding files doesn't exist or has a
          // different
          // modification time, then discard this record
          if (existingFile.exists()) {
            //
            // would have expected exact time stamps
            // but have observed slight differences
            // in return value for multiple evaluations of
            // lastModified(). Check if times are within
            // a second
            final long existingLastModified = existingFile.lastModified();
            if (!CUtil.isSignificantlyBefore(existingLastModified, this.outputLastModified)
                && !CUtil.isSignificantlyAfter(existingLastModified, this.outputLastModified)) {
              final SourceHistory[] sourcesArray = new SourceHistory[this.sources.size()];
              this.sources.copyInto(sourcesArray);
              final TargetHistory targetHistory = new TargetHistory(this.config, this.output, this.outputLastModified,
                  sourcesArray);
              this.history.put(this.output, targetHistory);
            }
          }
        }
        this.output = null;
        this.sources.setSize(0);
      } else {
        //
        // reset config so targets not within a processor element
        // don't pick up a previous processors signature
        //
        if (qName.equals("processor")) {
          this.config = null;
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
      // if sourceElement
      //
      if (qName.equals("source")) {
        final String sourceFile = atts.getValue("file");
        final long sourceLastModified = Long.parseLong(atts.getValue("lastModified"), 16);
        this.sources.addElement(new SourceHistory(sourceFile, sourceLastModified));
      } else {
        //
        // if <target> element,
        // grab file name and lastModified values
        // TargetHistory object will be created in endElement
        //
        if (qName.equals("target")) {
          this.sources.setSize(0);
          this.output = atts.getValue("file");
          this.outputLastModified = Long.parseLong(atts.getValue("lastModified"), 16);
        } else {
          //
          // if <processor> element,
          // grab signature attribute
          //
          if (qName.equals("processor")) {
            this.config = atts.getValue("signature");
          }
        }
      }
    }
  }

  /**
   * Flag indicating whether the cache should be written back to file.
   */
  private boolean dirty;
  /**
   * a hashtable of TargetHistory's keyed by output file name
   */
  private final Hashtable<String, TargetHistory> history = new Hashtable<>();
  /**
   * The file the cache was loaded from.
   */
  private final/* final */File historyFile;
  private final/* final */File outputDir;
  private String outputDirPath;

  /**
   * Creates a target history table from history.xml in the output directory,
   * if it exists. Otherwise, initializes the history table empty.
   *
   * @param task
   *          task used for logging history load errors
   * @param outputDir
   *          output directory for task
   */
  public TargetHistoryTable(final CCTask task, final File outputDir) throws BuildException

  {
    if (outputDir == null) {
      throw new NullPointerException("outputDir");
    }
    if (!outputDir.isDirectory()) {
      throw new BuildException("Output directory is not a directory");
    }
    if (!outputDir.exists()) {
      throw new BuildException("Output directory does not exist");
    }
    this.outputDir = outputDir;
    try {
      this.outputDirPath = outputDir.getCanonicalPath();
    } catch (final IOException ex) {
      this.outputDirPath = outputDir.toString();
    }
    //
    // load any existing history from file
    // suppressing any records whose corresponding
    // file does not exist, is zero-length or
    // last modified dates differ
    this.historyFile = new File(outputDir, "history.xml");

    if (this.historyFile.exists()) {
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      try {
        final SAXParser parser = factory.newSAXParser();
        parser.parse(this.historyFile, new TargetHistoryTableHandler(this.history, outputDir));
      } catch (final Exception ex) {
        //
        // a failure on loading this history is not critical
        // but should be logged
        task.log("Error reading history.xml: " + ex.toString());
      }
    } else {
      //
      // create empty history file for identifying new files by last
      // modified
      // timestamp comperation (to compare with
      // System.currentTimeMillis() don't work on Unix, because it
      // maesure timestamps only in seconds).
      // try {

      try {
        final File temp = File.createTempFile("history.xml", Long.toString(System.nanoTime()), outputDir);
        try (FileWriter writer = new FileWriter(temp)) {
          writer.write("<history/>");
        }
        if (!temp.renameTo(this.historyFile)) {
          throw new IOException("Could not rename " + temp + " to " + this.historyFile);
        }
      } catch (final IOException ex) {
        throw new BuildException("Can't create history file", ex);
      }
    }
  }

  public void commit() throws IOException {
    //
    // if not dirty, no need to update file
    //
    if (this.dirty) {
      //
      // build (small) hashtable of config id's in history
      //
      final Hashtable<String, String> configs = new Hashtable<>(20);
      Enumeration<TargetHistory> elements = this.history.elements();
      while (elements.hasMoreElements()) {
        final TargetHistory targetHistory = elements.nextElement();
        final String configId = targetHistory.getProcessorConfiguration();
        if (configs.get(configId) == null) {
          configs.put(configId, configId);
        }
      }
      final FileOutputStream outStream = new FileOutputStream(this.historyFile);
      OutputStreamWriter outWriter;
      //
      // early VM's don't support UTF-8 encoding
      // try and fallback to the default encoding
      // otherwise
      String encodingName = "UTF-8";
      try {
        outWriter = new OutputStreamWriter(outStream, "UTF-8");
      } catch (final UnsupportedEncodingException ex) {
        outWriter = new OutputStreamWriter(outStream);
        encodingName = outWriter.getEncoding();
      }
      final BufferedWriter writer = new BufferedWriter(outWriter);
      writer.write("<?xml version='1.0' encoding='");
      writer.write(encodingName);
      writer.write("'?>\n");
      writer.write("<history>\n");
      final StringBuffer buf = new StringBuffer(200);
      final Enumeration<String> configEnum = configs.elements();
      while (configEnum.hasMoreElements()) {
        final String configId = configEnum.nextElement();
        buf.setLength(0);
        buf.append("   <processor signature=\"");
        buf.append(CUtil.xmlAttribEncode(configId));
        buf.append("\">\n");
        writer.write(buf.toString());
        elements = this.history.elements();
        while (elements.hasMoreElements()) {
          final TargetHistory targetHistory = elements.nextElement();
          if (targetHistory.getProcessorConfiguration().equals(configId)) {
            buf.setLength(0);
            buf.append("      <target file=\"");
            buf.append(CUtil.xmlAttribEncode(targetHistory.getOutput()));
            buf.append("\" lastModified=\"");
            buf.append(Long.toHexString(targetHistory.getOutputLastModified()));
            buf.append("\">\n");
            writer.write(buf.toString());
            final SourceHistory[] sourceHistories = targetHistory.getSources();
            for (final SourceHistory sourceHistorie : sourceHistories) {
              buf.setLength(0);
              buf.append("         <source file=\"");
              buf.append(CUtil.xmlAttribEncode(sourceHistorie.getRelativePath()));
              buf.append("\" lastModified=\"");
              buf.append(Long.toHexString(sourceHistorie.getLastModified()));
              buf.append("\"/>\n");
              writer.write(buf.toString());
            }
            writer.write("      </target>\n");
          }
        }
        writer.write("   </processor>\n");
      }
      writer.write("</history>\n");
      writer.close();
      this.dirty = false;
    }
  }

  public TargetHistory get(final String configId, final String outputName) {
    TargetHistory targetHistory = this.history.get(outputName);
    if (targetHistory != null && !targetHistory.getProcessorConfiguration().equals(configId)) {
        targetHistory = null;
    }
    return targetHistory;
  }

  public File getHistoryFile() {
    return this.historyFile;
  }

  public void markForRebuild(final Map<String, TargetInfo> targetInfos) {
    for (final TargetInfo targetInfo : targetInfos.values()) {
      markForRebuild(targetInfo);
    }
  }

  // FREEHEP added synchronized
  public synchronized void markForRebuild(final TargetInfo targetInfo) {
    //
    // if it must already be rebuilt, no need to check further
    //
    if (!targetInfo.getRebuild()) {
      final TargetHistory history = get(targetInfo.getConfiguration().toString(), targetInfo.getOutput().getName());
      if (history == null) {
        targetInfo.mustRebuild();
      } else {
        final SourceHistory[] sourceHistories = history.getSources();
        final File[] sources = targetInfo.getSources();
        if (sourceHistories.length != sources.length) {
          targetInfo.mustRebuild();
        } else {
          final Hashtable<String, File> sourceMap = new Hashtable<>(sources.length);
          for (final File source : sources) {
            try {
              sourceMap.put(source.getCanonicalPath(), source);
            } catch (final IOException ex) {
              sourceMap.put(source.getAbsolutePath(), source);
            }
          }
          for (final SourceHistory sourceHistorie : sourceHistories) {
            //
            // relative file name, must absolutize it on output
            // directory
            //
            final String absPath = sourceHistorie.getAbsolutePath(this.outputDir);
            File match = sourceMap.get(absPath);
            if (match != null) {
              try {
                match = sourceMap.get(new File(absPath).getCanonicalPath());
              } catch (final IOException ex) {
                targetInfo.mustRebuild();
                break;
              }
            }
            if (match == null || match.lastModified() != sourceHistorie.getLastModified()) {
              targetInfo.mustRebuild();
              break;
            }
          }
        }
      }
    }
  }

  public void update(final ProcessorConfiguration config, final String[] sources, final VersionInfo versionInfo) {
    final String configId = config.getIdentifier();
    final String[] onesource = new String[1];
    String[] outputNames;
    for (final String source : sources) {
      onesource[0] = source;
      outputNames = config.getOutputFileNames(source, versionInfo);
      for (final String outputName : outputNames) {
        update(configId, outputName, onesource);
      }
    }
  }

  // FREEHEP added synchronized
  private synchronized void update(final String configId, final String outputName, final String[] sources) {
    final File outputFile = new File(this.outputDir, outputName);
    //
    // if output file doesn't exist or predates the start of the
    // compile step (most likely a compilation error) then
    // do not write add a history entry
    //
    if (outputFile.exists() && !CUtil.isSignificantlyBefore(outputFile.lastModified(), this.historyFile.lastModified())) {
      this.dirty = true;
      this.history.remove(outputName);
      final SourceHistory[] sourceHistories = new SourceHistory[sources.length];
      for (int i = 0; i < sources.length; i++) {
        final File sourceFile = new File(sources[i]);
        final long lastModified = sourceFile.lastModified();
        final String relativePath = CUtil.getRelativePath(this.outputDirPath, sourceFile);
        sourceHistories[i] = new SourceHistory(relativePath, lastModified);
      }
      final TargetHistory newHistory = new TargetHistory(configId, outputName, outputFile.lastModified(),
          sourceHistories);
      this.history.put(outputName, newHistory);
    }
  }

  // FREEHEP added synchronized
  public synchronized void update(final TargetInfo linkTarget) {
    final File outputFile = linkTarget.getOutput();
    final String outputName = outputFile.getName();
    //
    // if output file doesn't exist or predates the start of the
    // compile or link step (most likely a compilation error) then
    // do not write add a history entry
    //
    if (outputFile.exists() && !CUtil.isSignificantlyBefore(outputFile.lastModified(), this.historyFile.lastModified())) {
      this.dirty = true;
      this.history.remove(outputName);
      final SourceHistory[] sourceHistories = linkTarget.getSourceHistories(this.outputDirPath);
      final TargetHistory newHistory = new TargetHistory(linkTarget.getConfiguration().getIdentifier(), outputName,
          outputFile.lastModified(), sourceHistories);
      this.history.put(outputName, newHistory);
    }
  }
}
