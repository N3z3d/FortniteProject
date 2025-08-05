package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entité Trade pour le MVP - Basique pour éviter les erreurs de compilation */
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

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private TradeStatus status;

  public enum TradeStatus {
    PENDING,
    COMPLETED,
    CANCELLED
  }

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (status == null) {
      status = TradeStatus.PENDING;
    }
  }
}
