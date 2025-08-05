package com.fortnite.pronos.model;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
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
@Table(name = "team_players")
@IdClass(TeamPlayer.TeamPlayerId.class)
public class TeamPlayer {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Min(1)
  @Column(nullable = false)
  private Integer position;

  @Column(name = "until")
  private OffsetDateTime until;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TeamPlayerId implements Serializable {
    private UUID team; // Corrigé : UUID au lieu de String
    private UUID player; // Corrigé : UUID au lieu de String
  }

  public void endMembership() {
    this.until = OffsetDateTime.now();
  }

  public OffsetDateTime getUntil() {
    return until;
  }

  public boolean isActive() {
    return until == null;
  }

  @PrePersist
  @PreUpdate
  public void validatePosition() {
    if (position < 1) {
      throw new IllegalArgumentException("La position doit être supérieure à 0");
    }
  }

  public Team getTeam() {
    return team;
  }

  public void setTeam(Team team) {
    this.team = team;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }

  public void setUntil(OffsetDateTime until) {
    this.until = until;
  }
}
