package com.lit.core.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class History {
    private final Map<String, Snapshot> snapshots;
    private final Map<String, Task> tasks;

    public History() {
        this.snapshots = new HashMap<>();
        this.tasks = new HashMap<>();
    }

    public void addSnapshot(Snapshot snapshot) {
        snapshots.put(snapshot.getId(), snapshot);
    }

    public Optional<Snapshot> getSnapshot(String id) {
        return Optional.ofNullable(snapshots.get(id));
    }

    public void addTask(Task task) {
        tasks.put(task.getName(), task);
    }

    public Optional<Task> getTask(String name) {
        return Optional.ofNullable(tasks.get(name));
    }

    public Collection<Task> getTasks() {
        return tasks.values();
    }

    public boolean hasSnapshot(String id) {
        return snapshots.containsKey(id);
    }
}
