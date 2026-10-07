package com.lit.api;

import java.time.Instant;
import java.util.Optional;

public record SnapshotRecord(
    String id,
    Optional<String> parentId,
    String message,
    Instant timestamp,
    String author
) {}
