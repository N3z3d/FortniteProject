package com.fortnite.pronos.service.supabase.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseScoreRow(
    @JsonProperty("player_id") UUID playerId,
    Integer season,
    Integer points,
    LocalDate date,
    @JsonProperty("timestamp") OffsetDateTime timestamp) {}
