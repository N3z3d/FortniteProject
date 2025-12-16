package com.fortnite.pronos.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "teams")
public class Team {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(nullable = false)
  private Integer season;

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<TeamPlayer> players = new ArrayList<>();

  @OneToMany(mappedBy = "teamFrom")
  private List<Trade> outgoingTrades = new ArrayList<>();

  @OneToMany(mappedBy = "teamTo")
  private List<Trade> incomingTrades = new ArrayList<>();

  @Column(name = "completed_trades_count")
  private Integer completedTradesCount = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id")
  private Game game;

  public void addPlayer(Player player, int position) {
    TeamPlayer teamPlayer = new TeamPlayer();
    teamPlayer.setTeam(this);
    teamPlayer.setPlayer(player);
    teamPlayer.setPosition(position);
    players.add(teamPlayer);
  }

  public void removePlayer(Player player) {
    players.removeIf(tp -> tp.getPlayer().equals(player));
  }

  public boolean hasPlayer(Player player) {
    return players.stream().anyMatch(tp -> tp.getPlayer().equals(player));
  }

  public int findPlayerPositionInTeam(Player player) {
    return players.stream()
        .filter(tp -> tp.getPlayer().equals(player) && tp.getUntil() == null)
        .findFirst()
        .map(TeamPlayer::getPosition)
        .orElseThrow(() -> new IllegalArgumentException("Le joueur n'est pas dans l'équipe"));
  }

  public int getPlayerCount() {
    return (int) players.stream().count();
  }

  @PrePersist
  @PreUpdate
  public void validateTeam() {
    // Vérification que les positions restent uniques
    if (players.stream().map(TeamPlayer::getPosition).distinct().count() != players.size()) {
      throw new IllegalStateException("Les positions des joueurs doivent être uniques");
    }
  }

  public User getUser() {
    return owner;
  }

  public void setUser(User user) {
    this.owner = user;
  }

  /**
   * Legacy-friendly setter: accepts either TeamPlayer or Player lists and normalizes to TeamPlayer.
   */
  public void setPlayers(List<?> players) {
    this.players = new ArrayList<>();
    if (players == null) {
      return;
    }
    int position = 1;
    for (Object obj : players) {
      if (obj instanceof TeamPlayer tp) {
        this.players.add(tp);
      } else if (obj instanceof Player p) {
        TeamPlayer tp = new TeamPlayer();
        tp.setTeam(this);
        tp.setPlayer(p);
        tp.setPosition(position++);
        this.players.add(tp);
      }
    }
  }

  // Convenience methods for tests that work with simple Player lists
  @Transient // not persisted; test-only helper
  private List<Player> simplePlayersList = new ArrayList<>();

  public List<Player> getLegacyPlayers() {
    return simplePlayersList;
  }

  public void setLegacyPlayers(List<Player> players) {
    this.simplePlayersList = players != null ? players : new ArrayList<>();
  }
}
