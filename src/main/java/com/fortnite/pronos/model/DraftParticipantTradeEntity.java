package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity for draft participant trade proposals (Story 5.2). */
@Entity
@Table(name = "draft_participant_trades")
@Getter
@Setter
@NoArgsConstructor
public class DraftParticipantTradeEntity {

  // Domain aggregate pre-assigns the UUID before persistence.
  @Id private UUID id;

  @Column(nullable = false)
  private UUID draftId;

  @Column(nullable = false)
  private UUID proposerParticipantId;

  @Column(nullable = false)
  private UUID targetParticipantId;

  @Column(nullable = false)
  private UUID playerFromProposerId;

  @Column(nullable = false)
  private UUID playerFromTargetId;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private LocalDateTime proposedAt;

  @Column private LocalDateTime resolvedAt;

  @PrePersist
  protected void onPersist() {
    if (proposedAt == null) {
      proposedAt = LocalDateTime.now();
    }
    if (status == null) {
      status = "PENDING";
    }
  }
}
