package com.lit.core.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class Snapshot {
    private final String id;
    private final String parentId;
    private final String message;
    private final Instant timestamp;
    private final String author;
    private final Map<Path, String> fileTree; // Mapa de ruta_relativa -> contenido_archivo

    public Snapshot(String id, String parentId, String message, Instant timestamp, String author, Map<Path, String> fileTree) {
        this.id = id;
        this.parentId = parentId;
        this.message = message;
        this.timestamp = timestamp;
        this.author = author;
        this.fileTree = Map.copyOf(fileTree);
    }

    public String getId() {
        return id;
    }

    public Optional<String> getParentId() {
        return Optional.ofNullable(parentId);
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getAuthor() {
        return author;
    }

    public Map<Path, String> getFileTree() {
        return fileTree;
    }
}
