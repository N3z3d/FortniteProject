package com.fortnite.pronos.service.admin;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;

/** Immutable record of a single admin write action for the in-memory audit trail. */
@Getter
public final class AdminAuditEntry {

  private final UUID id;
  private final String actor;
  private final String action;
  private final String entityType;
  private final String entityId;
  private final String details;
  private final LocalDateTime timestamp;

  public AdminAuditEntry(
      String actor, String action, String entityType, String entityId, String details) {
    this.id = UUID.randomUUID();
    this.actor = actor;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.details = details;
    this.timestamp = LocalDateTime.now();
  }
}
