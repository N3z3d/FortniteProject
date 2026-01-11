package com.fortnite.pronos.service.supabase.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseUserRow(
    UUID id,
    String username,
    String email,
    String role,
    @JsonProperty("current_season") Integer currentSeason) {}
