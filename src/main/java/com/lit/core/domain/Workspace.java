package com.lit.core.domain;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Workspace {
    private final Path path;
    private final Map<Path, String> virtualDisk; // RutaRelativa -> Contenido
    private final Map<Path, Conflict> conflicts;

    public Workspace(Path path) {
        this.path = path;
        this.virtualDisk = new HashMap<>();
        this.conflicts = new HashMap<>();
    }

    public Path getPath() {
        return path;
    }

    public Map<Path, String> getVirtualDisk() {
        return virtualDisk;
    }

    public Map<Path, Conflict> getConflicts() {
        return conflicts;
    }

    public void addFile(Path relativePath, String content) {
        virtualDisk.put(relativePath, content);
    }

    public void modifyFile(Path relativePath, String content) {
        virtualDisk.put(relativePath, content);
    }

    public void deleteFile(Path relativePath) {
        virtualDisk.remove(relativePath);
    }

    public void addConflict(Conflict conflict) {
        conflicts.put(conflict.getRelativePath(), conflict);
    }

    public void clearConflicts() {
        conflicts.clear();
    }

    public void resolveConflict(Path relativePath) {
        Conflict c = conflicts.get(relativePath);
        if (c != null) {
            c.setResolved(true);
        }
    }
}
