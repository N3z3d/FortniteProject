package com.fortnite.pronos.dto.admin;

import java.util.Map;

/** Result DTO returned by POST /api/admin/scraping/trigger. */
public record IngestionTriggerResultDto(
    String status, int regionsProcessed, Map<String, String> regionFailures, long durationMs) {}
