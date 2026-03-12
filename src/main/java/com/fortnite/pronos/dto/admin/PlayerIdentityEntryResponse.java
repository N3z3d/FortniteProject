package com.fortnite.pronos.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlayerIdentityEntryResponse(
    UUID id,
    UUID playerId,
    String playerUsername,
    String playerRegion,
    String epicId,
    String status,
    int confidenceScore,
    String resolvedBy,
    LocalDateTime resolvedAt,
    LocalDateTime rejectedAt,
    String rejectionReason,
    LocalDateTime createdAt,
    String correctedUsername,
    String correctedRegion,
    String correctedBy,
    LocalDateTime correctedAt) {}
