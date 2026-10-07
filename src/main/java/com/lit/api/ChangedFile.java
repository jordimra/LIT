package com.lit.api;

import java.nio.file.Path;

public record ChangedFile(
    Path relativePath,
    ChangeType type
) {}
