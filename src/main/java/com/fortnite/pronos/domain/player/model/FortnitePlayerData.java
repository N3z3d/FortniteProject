package com.fortnite.pronos.domain.player.model;

/**
 * Snapshot of a Fortnite player's public data fetched from the external Fortnite API. This is a
 * read-only value object — not persisted to the database.
 */
public record FortnitePlayerData(
    String epicAccountId,
    String displayName,
    int battlePassLevel,
    int wins,
    int kills,
    int matches,
    double kd,
    double winRate,
    int minutesPlayed) {}
