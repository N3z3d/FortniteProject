package com.fortnite.pronos.dto.player;

import java.time.LocalDate;

/** Sparkline data point: one rank value per day. Sorted ascending by date on the response list. */
public record RankSnapshotResponse(LocalDate date, int rank) {}
