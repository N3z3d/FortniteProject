package com.fortnite.pronos.service.supabase.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseGameParticipantRowDto(
    UUID id,
    @JsonProperty("game_id") UUID gameId,
    @JsonProperty("user_id") UUID userId,
    @JsonProperty("draft_order") Integer draftOrder,
    @JsonProperty("joined_at") OffsetDateTime joinedAt,
    @JsonProperty("last_selection_time") OffsetDateTime lastSelectionTime,
    @JsonProperty("is_creator") Boolean creator) {}
