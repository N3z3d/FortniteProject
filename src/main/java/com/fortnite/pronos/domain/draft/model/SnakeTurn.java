package com.fortnite.pronos.domain.draft.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the next expected pick in a snake draft.
 *
 * <p>Returned by the orchestrator to tell callers whose turn it is, in which round, and whether the
 * snake order is currently reversed (even rounds).
 */
public record SnakeTurn(UUID participantId, int round, int pickNumber, boolean reversed) {

  public SnakeTurn {
    Objects.requireNonNull(participantId, "participantId cannot be null");
    if (round < 1) throw new IllegalArgumentException("round must be >= 1");
    if (pickNumber < 1) throw new IllegalArgumentException("pickNumber must be >= 1");
  }

  public UUID getParticipantId() {
    return participantId;
  }

  public int getRound() {
    return round;
  }

  public int getPickNumber() {
    return pickNumber;
  }

  public boolean isReversed() {
    return reversed;
  }
}
