package com.fortnite.pronos.model;

import java.io.Serializable;
import java.time.LocalDate;
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
@Table(name = "scores")
@IdClass(Score.ScoreId.class)
public class Score implements Serializable {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Id
  @Column(nullable = false)
  private Integer season;

  @Min(0)
  @Column(nullable = false)
  private Integer points;

  @Column(name = "date")
  private LocalDate date;

  @Column(name = "timestamp", nullable = false)
  private OffsetDateTime timestamp;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScoreId implements Serializable {
    private UUID player; // UUID pour correspondre à l'entité Player
    private Integer season;
  }

  @PrePersist
  @PreUpdate
  public void validateScore() {
    if (points < 0) {
      throw new IllegalArgumentException("Les points ne peuvent pas être négatifs");
    }
    if (timestamp == null) {
      timestamp = OffsetDateTime.now();
    }
    if (date == null) {
      date = LocalDate.now();
    }
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;
  }

  public Integer getPoints() {
    return points;
  }

  public void setPoints(Integer points) {
    this.points = points;
  }

  // Méthode utilitaire pour les tests - simule un setId
  public void setId(UUID id) {
    // Pour les tests, on peut créer un nouveau Player avec cet ID
    if (this.player == null) {
      this.player = new Player();
    }
    this.player.setId(id);
  }
}
