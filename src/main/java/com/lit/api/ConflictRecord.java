package com.lit.api;

import java.nio.file.Path;
import java.util.List;

public record ConflictRecord(
    Path relativePath,
    List<String> localLines,
    List<String> incomingLines
) {}
