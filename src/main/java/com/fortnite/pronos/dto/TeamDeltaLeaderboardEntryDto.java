package com.fortnite.pronos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** DTO representing a single team entry in the game leaderboard, ordered by delta PR descending. */
public record TeamDeltaLeaderboardEntryDto(
    int rank,
    UUID participantId,
    String username,
    int deltaPr,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDateTime computedAt) {}
