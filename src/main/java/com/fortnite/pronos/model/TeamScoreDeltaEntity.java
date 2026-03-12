package com.fortnite.pronos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity for the team_score_deltas table. */
@Entity
@Table(
    name = "team_score_deltas",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_team_score_delta",
            columnNames = {"game_id", "participant_id", "period_start", "period_end"}))
@Getter
@Setter
@NoArgsConstructor
public class TeamScoreDeltaEntity {

  @Id private UUID id;

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "participant_id", nullable = false)
  private UUID participantId;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "delta_pr", nullable = false)
  private int deltaPr;

  @Column(name = "computed_at", nullable = false)
  private LocalDateTime computedAt;
}
