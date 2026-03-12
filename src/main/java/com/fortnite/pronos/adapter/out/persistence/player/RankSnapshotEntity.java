package com.fortnite.pronos.adapter.out.persistence.player;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "rank_snapshots",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_rank_snapshots_player_region_date",
            columnNames = {"player_id", "region", "snapshot_date"}))
@Getter
@Setter
@NoArgsConstructor
public class RankSnapshotEntity {

  @Id private UUID id;

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "region", nullable = false, length = 10)
  private String region;

  @Column(name = "rank", nullable = false)
  private int rank;

  @Column(name = "pr_value", nullable = false)
  private int prValue;

  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;
}
