package com.fortnite.pronos.service.supabase.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseGameRow(
    UUID id,
    String name,
    @JsonProperty("creator_id") UUID creatorId,
    @JsonProperty("max_participants") Integer maxParticipants,
    String status,
    @JsonProperty("created_at") OffsetDateTime createdAt,
    String description,
    @JsonProperty("invitation_code") String invitationCode) {}
