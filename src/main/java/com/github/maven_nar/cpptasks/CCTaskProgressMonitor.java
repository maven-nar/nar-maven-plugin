package com.github.maven_nar.cpptasks;
import java.io.IOException;

import com.github.maven_nar.cpptasks.compiler.ProcessorConfiguration;
import com.github.maven_nar.cpptasks.compiler.ProgressMonitor;

public class CCTaskProgressMonitor implements ProgressMonitor {
    private ProcessorConfiguration config;
    private TargetHistoryTable history;
    private VersionInfo versionInfo;
    private long lastCommit = -1;
    public CCTaskProgressMonitor(TargetHistoryTable history, VersionInfo versionInfo) {
        this.history = history;
        this.versionInfo = versionInfo;
    }
    public void finish(ProcessorConfiguration config, boolean normal) {
        long current = System.currentTimeMillis();
        if ((current - lastCommit) > 120000) {
            try {
                history.commit();
                lastCommit = System.currentTimeMillis();
            } catch (IOException ex) {
            }
        }
    }
    public void progress(String[] sources) {
        history.update(config, sources, versionInfo);
        long current = System.currentTimeMillis();
        if ((current - lastCommit) > 120000) {
            try {
                history.commit();
                lastCommit = current;
            } catch (IOException ex) {
            }
        }
    }
    public void start(ProcessorConfiguration config) {
        if (lastCommit < 0) {
            lastCommit = System.currentTimeMillis();
        }
        this.config = config;
    }
}
