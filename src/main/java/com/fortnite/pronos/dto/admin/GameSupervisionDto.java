package com.fortnite.pronos.dto.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GameSupervisionDto(
    UUID gameId,
    String gameName,
    String status,
    String draftMode,
    int participantCount,
    int maxParticipants,
    String creatorUsername,
    OffsetDateTime createdAt) {}
