package com.github.maven_nar.cpptasks;
/**
 * A description of a file built or to be built
 */
public final class TargetHistory {
    private/* final */String config;
    private/* final */String output;
    private/* final */long outputLastModified;
    private/* final */SourceHistory[] sources;
    /**
     * Constructor from build step
     */
    public TargetHistory(String config, String output, long outputLastModified,
            SourceHistory[] sources) {
        if (config == null) {
            throw new NullPointerException("config");
        }
        if (sources == null) {
            throw new NullPointerException("source");
        }
        if (output == null) {
            throw new NullPointerException("output");
        }
        this.config = config;
        this.output = output;
        this.outputLastModified = outputLastModified;
        this.sources = (SourceHistory[]) sources.clone();
    }
    public String getOutput() {
        return output;
    }
    public long getOutputLastModified() {
        return outputLastModified;
    }
    public String getProcessorConfiguration() {
        return config;
    }
    public SourceHistory[] getSources() {
        SourceHistory[] clone = (SourceHistory[]) sources.clone();
        return clone;
    }
}
