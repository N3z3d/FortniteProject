package com.fortnite.pronos.domain.game.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Domain model representing a participant in a game. Framework-free. */
@SuppressWarnings({"java:S107"})
public final class GameParticipant {

  private static final int TIMEOUT_HOURS = 12;

  private UUID id;
  private final UUID userId;
  private final String username;
  private Integer draftOrder;
  private LocalDateTime joinedAt;
  private LocalDateTime lastSelectionTime;
  private boolean creator;
  private final List<UUID> selectedPlayerIds;

  public GameParticipant(UUID userId, String username, boolean creator) {
    Objects.requireNonNull(userId, "User ID cannot be null");
    this.userId = userId;
    this.username = username;
    this.creator = creator;
    this.joinedAt = LocalDateTime.now();
    this.selectedPlayerIds = new ArrayList<>();
  }

  /** Reconstitution constructor for persistence mapping. */
  public static GameParticipant restore(
      UUID id,
      UUID userId,
      String username,
      Integer draftOrder,
      LocalDateTime joinedAt,
      LocalDateTime lastSelectionTime,
      boolean creator,
      List<UUID> selectedPlayerIds) {
    GameParticipant p = new GameParticipant(userId, username, creator);
    p.id = id;
    p.draftOrder = draftOrder;
    p.joinedAt = joinedAt;
    p.lastSelectionTime = lastSelectionTime;
    p.selectedPlayerIds.clear();
    if (selectedPlayerIds != null) {
      p.selectedPlayerIds.addAll(selectedPlayerIds);
    }
    return p;
  }

  public void addSelectedPlayer(UUID playerId) {
    if (!selectedPlayerIds.contains(playerId)) {
      selectedPlayerIds.add(playerId);
      this.lastSelectionTime = LocalDateTime.now();
    }
  }

  public void removeSelectedPlayer(UUID playerId) {
    selectedPlayerIds.remove(playerId);
  }

  public boolean hasSelectedPlayer(UUID playerId) {
    return selectedPlayerIds.contains(playerId);
  }

  public int getSelectedPlayersCount() {
    return selectedPlayerIds.size();
  }

  public boolean hasTimedOut() {
    if (lastSelectionTime == null) {
      return false;
    }
    return LocalDateTime.now().isAfter(lastSelectionTime.plusHours(TIMEOUT_HOURS));
  }

  // --- Getters ---

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public Integer getDraftOrder() {
    return draftOrder;
  }

  public void setDraftOrder(Integer draftOrder) {
    this.draftOrder = draftOrder;
  }

  public LocalDateTime getJoinedAt() {
    return joinedAt;
  }

  public LocalDateTime getLastSelectionTime() {
    return lastSelectionTime;
  }

  public boolean isCreator() {
    return creator;
  }

  public List<UUID> getSelectedPlayerIds() {
    return Collections.unmodifiableList(selectedPlayerIds);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GameParticipant that)) return false;
    return Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId);
  }
}
