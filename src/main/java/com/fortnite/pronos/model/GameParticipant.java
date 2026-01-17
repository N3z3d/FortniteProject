package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entité représentant un participant dans une game */
@Entity
@Table(name = "game_participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", nullable = false)
  private Game game;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "draft_order")
  private Integer draftOrder;

  @Column(name = "joined_at")
  private LocalDateTime joinedAt;

  @Column(name = "last_selection_time")
  private LocalDateTime lastSelectionTime;

  @Column(name = "is_creator")
  private Boolean creator;

  @ManyToMany
  @JoinTable(
      name = "game_participant_players",
      joinColumns = @JoinColumn(name = "game_participant_id"),
      inverseJoinColumns = @JoinColumn(name = "player_id"))
  @Builder.Default
  private List<Player> selectedPlayers = new ArrayList<>();

  /** Ajoute un joueur à la sélection du participant */
  public void addSelectedPlayer(Player player) {
    if (!selectedPlayers.contains(player)) {
      selectedPlayers.add(player);
    }
  }

  /** Supprime un joueur de la sélection du participant */
  public void removeSelectedPlayer(Player player) {
    selectedPlayers.remove(player);
  }

  /** Vérifie si le participant a sélectionné un joueur spécifique */
  public boolean hasSelectedPlayer(Player player) {
    return selectedPlayers.contains(player);
  }

  /** Retourne le nombre de joueurs sélectionnés */
  public int getSelectedPlayersCount() {
    return selectedPlayers.size();
  }

  /** Met à jour le temps de dernière sélection */
  public void updateLastSelectionTime() {
    this.lastSelectionTime = LocalDateTime.now();
  }

  /** Vérifie si le participant a dépassé le timeout (12h) */
  public boolean hasTimedOut() {
    if (lastSelectionTime == null) {
      return false;
    }
    return LocalDateTime.now().isAfter(lastSelectionTime.plusHours(12));
  }

  /** Retourne le nom d'utilisateur du participant */
  public String getUsername() {
    return user != null ? user.getUsername() : null;
  }
}
