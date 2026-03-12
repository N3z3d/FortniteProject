package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity for solo swap audit entries (FR-36). */
@Entity
@Table(name = "draft_swap_audits")
@Getter
@Setter
@NoArgsConstructor
public class DraftSwapAuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID draftId;

  @Column(nullable = false)
  private UUID participantId;

  @Column(nullable = false)
  private UUID playerOutId;

  @Column(nullable = false)
  private UUID playerInId;

  @Column(nullable = false)
  private LocalDateTime occurredAt;

  @PrePersist
  protected void onPersist() {
    if (occurredAt == null) {
      occurredAt = LocalDateTime.now();
    }
  }
}
