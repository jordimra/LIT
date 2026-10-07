package com.lit.core.domain;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Repository {
    private final Path path;
    private final History history;
    private final Workspace workspace;
    private final Map<String, Remote> remotes;
    private final Map<String, String> config;

    public Repository(Path path) {
        this.path = path;
        this.history = new History();
        this.workspace = new Workspace(path);
        this.remotes = new HashMap<>();
        this.config = new HashMap<>();
    }

    public Path getPath() {
        return path;
    }

    public History getHistory() {
        return history;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Map<String, Remote> getRemotes() {
        return remotes;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void addRemote(Remote remote) {
        remotes.put(remote.getName(), remote);
    }
}
