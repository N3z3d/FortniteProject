package com.fortnite.pronos.dto.admin;

import java.time.LocalDateTime;

/** Per-region pipeline statistics (UNRESOLVED/RESOLVED/REJECTED counts + last ingestion date). */
public record PipelineRegionalStatsDto(
    String region,
    long unresolvedCount,
    long resolvedCount,
    long rejectedCount,
    long totalCount,
    LocalDateTime lastIngestedAt) {}
