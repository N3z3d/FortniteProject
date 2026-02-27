package com.fortnite.pronos.dto;

import java.util.UUID;

/**
 * Response DTO for a single conflict resolution (coin flip).
 *
 * <p>Returned by the resolve-conflict endpoint and mirrored in the WebSocket broadcast.
 */
public record ConflictResolutionResponse(
    UUID windowId,
    UUID contestedPlayerId,
    UUID winnerParticipantId,
    UUID loserParticipantId,
    boolean hasMoreConflicts) {}
