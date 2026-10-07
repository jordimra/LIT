package com.lit.api;

import java.util.List;

public record WorkspaceStatus(
    String activeTaskName,
    List<ChangedFile> modifiedFiles,
    List<ConflictRecord> activeConflicts,
    boolean hasChanges
) {}
