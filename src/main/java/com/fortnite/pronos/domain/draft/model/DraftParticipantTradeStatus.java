package com.fortnite.pronos.domain.draft.model;

/** Status lifecycle for a DraftParticipantTrade (FR-35). */
public enum DraftParticipantTradeStatus {
  PENDING,
  ACCEPTED,
  REJECTED,
  CANCELLED;

  public boolean isTerminal() {
    return this == ACCEPTED || this == REJECTED || this == CANCELLED;
  }
}
