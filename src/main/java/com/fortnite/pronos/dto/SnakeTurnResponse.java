package com.fortnite.pronos.dto;

import java.util.UUID;

import com.fortnite.pronos.domain.draft.model.SnakeTurn;

/**
 * DTO representing the current (or next) turn in a snake draft.
 *
 * <p>Returned by all snake draft endpoints and broadcast via WebSocket.
 */
public record SnakeTurnResponse(
    UUID draftId, String region, UUID participantId, int round, int pickNumber, boolean reversed) {

  /** Factory method mapping a domain {@link SnakeTurn} to a response DTO. */
  public static SnakeTurnResponse from(UUID draftId, String region, SnakeTurn turn) {
    return new SnakeTurnResponse(
        draftId, region, turn.participantId(), turn.round(), turn.pickNumber(), turn.isReversed());
  }
}
