package com.lit.core.domain;

import java.nio.file.Path;
import java.util.List;

public class Conflict {
    private final Path relativePath;
    private final List<String> localLines;
    private final List<String> incomingLines;
    private boolean resolved;

    public Conflict(Path relativePath, List<String> localLines, List<String> incomingLines) {
        this.relativePath = relativePath;
        this.localLines = List.copyOf(localLines);
        this.incomingLines = List.copyOf(incomingLines);
        this.resolved = false;
    }

    public Path getRelativePath() {
        return relativePath;
    }

    public List<String> getLocalLines() {
        return localLines;
    }

    public List<String> getIncomingLines() {
        return incomingLines;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
