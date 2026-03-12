package com.fortnite.pronos.dto.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScrapeLogDto(
    UUID id,
    String source,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    String status,
    Integer totalRowsWritten,
    String errorMessage) {}
