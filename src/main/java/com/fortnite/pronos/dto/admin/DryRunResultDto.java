package com.fortnite.pronos.dto.admin;

import java.util.List;

/** Result of a manual dry-run of the FortniteTracker scraping adapter. */
public record DryRunResultDto(
    String region, int rowCount, boolean valid, List<String> sampleRows, List<String> errors) {}
