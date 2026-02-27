package com.fortnite.pronos.adapter.out.persistence.draft;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA entity backing the {@code draft_async_windows} table. */
@Entity
@Table(name = "draft_async_windows")
public class DraftAsyncWindowEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "draft_id", nullable = false)
  private UUID draftId;

  @Column(name = "slot", nullable = false, length = 50)
  private String slot;

  @Column(name = "deadline", nullable = false)
  private Instant deadline;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "total_expected", nullable = false)
  private int totalExpected;

  protected DraftAsyncWindowEntity() {}

  public DraftAsyncWindowEntity(
      UUID id, UUID draftId, String slot, Instant deadline, String status, int totalExpected) {
    this.id = id;
    this.draftId = draftId;
    this.slot = slot;
    this.deadline = deadline;
    this.status = status;
    this.totalExpected = totalExpected;
  }

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

  public String getStatus() {
    return status;
  }

  public int getTotalExpected() {
    return totalExpected;
  }
}
