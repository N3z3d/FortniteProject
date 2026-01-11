package com.fortnite.pronos.service.supabase.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabasePlayerRow(
    UUID id,
    @JsonProperty("fortnite_id") String fortniteId,
    String username,
    String nickname,
    String region,
    String tranche,
    @JsonProperty("current_season") Integer currentSeason) {}
