package com.fortnite.pronos.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pr_snapshots")
@IdClass(PrSnapshot.PrSnapshotId.class)
public class PrSnapshot implements Serializable {
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Id
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PrRegion region;

  @Id
  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;

  @Min(0)
  @Column(nullable = false)
  private Integer points;

  @Min(1)
  @Column(nullable = false)
  private Integer rank;

  @Column(name = "collected_at", nullable = false)
  private OffsetDateTime collectedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "run_id")
  private IngestionRun run;

  @PrePersist
  @PreUpdate
  void applyDefaults() {
    if (collectedAt == null) {
      collectedAt = OffsetDateTime.now();
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PrSnapshotId implements Serializable {
    private UUID player;
    private PrRegion region;
    private LocalDate snapshotDate;
  }
}
