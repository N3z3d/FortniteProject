package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entity representing a participant in a game. */
@Entity
@Table(
    name = "game_participants",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_game_participant",
            columnNames = {"game_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameParticipant {

  private static final int DRAFT_SELECTION_TIMEOUT_HOURS = 12;

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

  public void addSelectedPlayer(Player player) {
    if (selectedPlayerIndex(player) < 0) {
      selectedPlayers.add(player);
    }
  }

  public void removeSelectedPlayer(Player player) {
    int playerIndex = selectedPlayerIndex(player);
    if (playerIndex >= 0) {
      selectedPlayers.remove(playerIndex);
    }
  }

  public boolean hasSelectedPlayer(Player player) {
    return selectedPlayerIndex(player) >= 0;
  }

  public int getSelectedPlayersCount() {
    return selectedPlayers.size();
  }

  public void updateLastSelectionTime() {
    this.lastSelectionTime = LocalDateTime.now();
  }

  public boolean hasTimedOut() {
    if (lastSelectionTime == null) {
      return false;
    }
    return LocalDateTime.now().isAfter(lastSelectionTime.plusHours(DRAFT_SELECTION_TIMEOUT_HOURS));
  }

  public String getUsername() {
    return user != null ? user.getUsername() : null;
  }

  public UUID getUserId() {
    return user != null ? user.getId() : null;
  }

  private int selectedPlayerIndex(Player player) {
    return selectedPlayers.indexOf(player);
  }
}
