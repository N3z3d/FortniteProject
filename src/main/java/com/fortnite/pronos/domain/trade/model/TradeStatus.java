package com.fortnite.pronos.domain.trade.model;

/** Status values for a trade in the domain model. */
public enum TradeStatus {
  PENDING,
  ACCEPTED,
  REJECTED,
  CANCELLED,
  COUNTERED;

  /** Returns true if this status is terminal (no further transitions allowed). */
  public boolean isTerminal() {
    return this == ACCEPTED || this == REJECTED || this == CANCELLED;
  }
}
