package com.fortnite.pronos.domain.draft.model;

/** Life-cycle status of a simultaneous draft submission window. */
public enum DraftAsyncWindowStatus {
  /** Submissions are open; not all participants have submitted yet. */
  OPEN,
  /** All submitted; conflict detected — waiting for loser re-selection. */
  RESOLVING,
  /** All conflicts resolved; selections are final. */
  RESOLVED
}
