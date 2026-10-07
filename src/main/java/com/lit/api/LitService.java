package com.lit.api;

import java.nio.file.Path;
import java.util.List;

public interface LitService {

    void initialize(Path workspacePath);

    WorkspaceStatus getStatus();

    SnapshotRecord save(String message);

    void switchTask(String taskName);

    List<String> listTasks();

    String getActiveTask();

    void synchronize();

    void undo();
}
