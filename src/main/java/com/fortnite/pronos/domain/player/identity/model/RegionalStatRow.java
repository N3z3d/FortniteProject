package com.fortnite.pronos.domain.player.identity.model;

/** Raw aggregation row: (region, status, count) returned by repository. */
public record RegionalStatRow(String region, IdentityStatus status, long count) {}
