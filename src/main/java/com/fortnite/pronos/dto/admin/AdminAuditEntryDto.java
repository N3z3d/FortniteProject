package com.fortnite.pronos.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAuditEntryDto(
    UUID id,
    String actor,
    String action,
    String entityType,
    String entityId,
    String details,
    LocalDateTime timestamp) {}
