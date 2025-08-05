package com.fortnite.pronos.model;

import java.util.UUID;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entité représentant les règles régionales d'une game */
@Entity
@Table(name = "game_region_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRegionRule {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", nullable = false)
  private Game game;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Player.Region region;

  @Column(name = "max_players", nullable = false)
  private Integer maxPlayers;

  /** Vérifie si la règle est valide */
  public boolean isValid() {
    return maxPlayers != null && maxPlayers >= 1 && maxPlayers <= 10;
  }

  /** Retourne une description de la règle */
  public String getDescription() {
    return String.format("%s: %d joueurs max", region.name(), maxPlayers);
  }
}
