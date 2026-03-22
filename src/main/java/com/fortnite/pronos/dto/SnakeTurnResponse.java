package com.fortnite.pronos.dto;

import java.time.Instant;
import java.util.UUID;

import com.fortnite.pronos.domain.draft.model.SnakeTurn;

/**
 * DTO representing the current (or next) turn in a snake draft.
 *
 * <p>Returned by all snake draft endpoints and broadcast via WebSocket. The {@code expiresAt} field
 * is populated for WebSocket broadcasts so clients can sync their timer with the server.
 */
public record SnakeTurnResponse(
    UUID draftId,
    String region,
    UUID participantId,
    String participantUsername,
    int round,
    int pickNumber,
    boolean reversed,
    Instant expiresAt) {

  /**
   * Factory method for query and WebSocket endpoints.
   *
   * <p>Pass {@code participantUsername} when available to avoid client-side UUID→username
   * resolution that can desync (BUG-06). Pass {@code expiresAt} for WebSocket broadcasts so clients
   * can sync their timer with the server; pass {@code null} for query endpoints.
   */
  public static SnakeTurnResponse from(
      UUID draftId, String region, SnakeTurn turn, String participantUsername, Instant expiresAt) {
    return new SnakeTurnResponse(
        draftId,
        region,
        turn.participantId(),
        participantUsername,
        turn.round(),
        turn.pickNumber(),
        turn.isReversed(),
        expiresAt);
  }
}
