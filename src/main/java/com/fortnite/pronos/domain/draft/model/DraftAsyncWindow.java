package com.fortnite.pronos.domain.draft.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain aggregate for a simultaneous-draft submission window.
 *
 * <p>One window is opened per slot (round + pick position). All participants submit anonymously
 * within the deadline; the server detects conflicts and runs the coin-flip resolution.
 */
public final class DraftAsyncWindow {

  private final UUID id;
  private final UUID draftId;
  private final String slot;
  private final Instant deadline;
  private final DraftAsyncWindowStatus status;
  private final int totalExpected;

  public DraftAsyncWindow(UUID draftId, String slot, Instant deadline, int totalExpected) {
    this(UUID.randomUUID(), draftId, slot, deadline, DraftAsyncWindowStatus.OPEN, totalExpected);
  }

  private DraftAsyncWindow(
      UUID id,
      UUID draftId,
      String slot,
      Instant deadline,
      DraftAsyncWindowStatus status,
      int totalExpected) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.draftId = Objects.requireNonNull(draftId, "draftId cannot be null");
    this.slot = Objects.requireNonNull(slot, "slot cannot be null");
    this.deadline = Objects.requireNonNull(deadline, "deadline cannot be null");
    this.status = Objects.requireNonNull(status, "status cannot be null");
    if (totalExpected < 1) throw new IllegalArgumentException("totalExpected must be >= 1");
    this.totalExpected = totalExpected;
  }

  /** Reconstitution factory — for persistence mapping only. */
  public static DraftAsyncWindow restore(
      UUID id,
      UUID draftId,
      String slot,
      Instant deadline,
      DraftAsyncWindowStatus status,
      int totalExpected) {
    return new DraftAsyncWindow(id, draftId, slot, deadline, status, totalExpected);
  }

  // ===== BUSINESS METHODS =====

  public DraftAsyncWindow startResolving() {
    if (status != DraftAsyncWindowStatus.OPEN) {
      throw new IllegalStateException("Window must be OPEN to start resolving");
    }
    return new DraftAsyncWindow(
        id, draftId, slot, deadline, DraftAsyncWindowStatus.RESOLVING, totalExpected);
  }

  public DraftAsyncWindow resolve() {
    if (status == DraftAsyncWindowStatus.RESOLVED) {
      throw new IllegalStateException("Window is already RESOLVED");
    }
    return new DraftAsyncWindow(
        id, draftId, slot, deadline, DraftAsyncWindowStatus.RESOLVED, totalExpected);
  }

  // ===== GETTERS =====

  public UUID getId() {
    return id;
  }

  public UUID getDraftId() {
    return draftId;
  }

  public String getSlot() {
    return slot;
  }

  public Instant getDeadline() {
    return deadline;
  }

  public DraftAsyncWindowStatus getStatus() {
    return status;
  }

  public int getTotalExpected() {
    return totalExpected;
  }

  // ===== EQUALS / HASHCODE =====

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DraftAsyncWindow that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
