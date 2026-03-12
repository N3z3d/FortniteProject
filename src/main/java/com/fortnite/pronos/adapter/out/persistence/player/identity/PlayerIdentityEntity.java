package com.fortnite.pronos.adapter.out.persistence.player.identity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_identity_pipeline")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerIdentityEntity {

  @Id private UUID id;

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "player_username", nullable = false)
  private String playerUsername;

  @Column(name = "player_region", nullable = false)
  private String playerRegion;

  @Column(name = "epic_id")
  private String epicId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IdentityStatus status;

  @Column(name = "confidence_score")
  private int confidenceScore;

  @Column(name = "resolved_by")
  private String resolvedBy;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "rejected_at")
  private LocalDateTime rejectedAt;

  @Column(name = "rejection_reason", length = 512)
  private String rejectionReason;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "corrected_username")
  private String correctedUsername;

  @Column(name = "corrected_region")
  private String correctedRegion;

  @Column(name = "corrected_by")
  private String correctedBy;

  @Column(name = "corrected_at")
  private LocalDateTime correctedAt;
}
