package com.fortnite.pronos.service.supabase.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseTeamRowDto(
    UUID id,
    String name,
    @JsonProperty("owner_id") UUID ownerId,
    Integer season,
    @JsonProperty("game_id") UUID gameId) {}
