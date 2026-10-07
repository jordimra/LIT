package com.lit.core.domain;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Task {
    private final String name;
    private String headSnapshotId;
    private final Map<Path, String> cachedChanges; // Almacenamiento temporal para stashing automático

    public Task(String name, String headSnapshotId) {
        this.name = name;
        this.headSnapshotId = headSnapshotId;
        this.cachedChanges = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getHeadSnapshotId() {
        return headSnapshotId;
    }

    public void setHeadSnapshotId(String headSnapshotId) {
        this.headSnapshotId = headSnapshotId;
    }

    public Map<Path, String> getCachedChanges() {
        return cachedChanges;
    }

    public void clearCachedChanges() {
        this.cachedChanges.clear();
    }

    public void putCachedChanges(Map<Path, String> changes) {
        this.cachedChanges.clear();
        this.cachedChanges.putAll(changes);
    }
}
