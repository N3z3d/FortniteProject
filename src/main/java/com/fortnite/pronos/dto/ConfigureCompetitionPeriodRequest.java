package com.fortnite.pronos.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** Request payload for configuring the competition period of a game. */
public record ConfigureCompetitionPeriodRequest(
    @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
