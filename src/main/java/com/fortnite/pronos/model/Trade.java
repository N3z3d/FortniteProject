package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entité Trade pour le système de trading complet */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trades")
public class Trade {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_team_id", nullable = false)
  private Team fromTeam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_team_id", nullable = false)
  private Team toTeam;

  @ManyToMany
  @JoinTable(
      name = "trade_offered_players",
      joinColumns = @JoinColumn(name = "trade_id"),
      inverseJoinColumns = @JoinColumn(name = "player_id"))
  @Builder.Default
  private List<Player> offeredPlayers = new ArrayList<>();

  @ManyToMany
  @JoinTable(
      name = "trade_requested_players",
      joinColumns = @JoinColumn(name = "trade_id"),
      inverseJoinColumns = @JoinColumn(name = "player_id"))
  @Builder.Default
  private List<Player> requestedPlayers = new ArrayList<>();

  @Column(name = "proposed_at", nullable = false)
  private LocalDateTime proposedAt;

  @Column(name = "accepted_at")
  private LocalDateTime acceptedAt;

  @Column(name = "rejected_at")
  private LocalDateTime rejectedAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private Status status;

  @Column(name = "original_trade_id")
  private UUID originalTradeId;

  // For backward compatibility
  @ManyToOne
  @JoinColumn(name = "player_out_id")
  private Player playerOut;

  @ManyToOne
  @JoinColumn(name = "player_in_id")
  private Player playerIn;

  @ManyToOne
  @JoinColumn(name = "team_from_id")
  private Team teamFrom;

  @ManyToOne
  @JoinColumn(name = "team_to_id")
  private Team teamTo;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  public enum Status {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    COUNTERED
  }

  // Backward compatibility enum
  public enum TradeStatus {
    PENDING,
    COMPLETED,
    CANCELLED
  }

  @PrePersist
  public void prePersist() {
    if (proposedAt == null) {
      proposedAt = LocalDateTime.now();
    }
    if (status == null) {
      status = Status.PENDING;
    }
    // Backward compatibility
    if (createdAt == null) {
      createdAt = proposedAt != null ? proposedAt : LocalDateTime.now();
    }
  }

  // Helper methods for the tests
  public void setFromTeam(Team team) {
    this.fromTeam = team;
    // Backward compatibility
    this.teamFrom = team;
  }

  public void setToTeam(Team team) {
    this.toTeam = team;
    // Backward compatibility
    this.teamTo = team;
  }
}
