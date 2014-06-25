package com.github.maven_nar.cpptasks;
import java.io.File;
import java.io.IOException;
/**
 * The history of a source file used to build a target
 * 
 * @author Curt Arnold
 */
public final class SourceHistory {
    private/* final */long lastModified;
    private/* final */String relativePath;
    /**
     * Constructor
     */
    public SourceHistory(String relativePath, long lastModified) {
        if (relativePath == null) {
            throw new NullPointerException("relativePath");
        }
        this.relativePath = relativePath;
        this.lastModified = lastModified;
    }
    public String getAbsolutePath(File baseDir) {
        try {
            return new File(baseDir, relativePath).getCanonicalPath();
        } catch (IOException ex) {
        }
        return relativePath;
    }
    public long getLastModified() {
        return lastModified;
    }
    public String getRelativePath() {
        return relativePath;
    }
}
