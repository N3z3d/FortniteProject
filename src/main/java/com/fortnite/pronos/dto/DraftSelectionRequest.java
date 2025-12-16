package com.fortnite.pronos.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** DTO pour les requêtes de sélection de joueurs dans un draft. */
public class DraftSelectionRequest {

  private static final int MIN_ROUND = 1;
  private static final int MAX_ROUND = 100;
  private static final int MIN_PICK = 1;
  private static final int MAX_PICK = 200;
  private static final long MIN_SELECTION_TIME = 0L;
  private static final String MANUAL_SELECTION_TYPE = "MANUAL";
  private static final String AUTO_PICK_SELECTION_TYPE = "AUTO_PICK";

  private UUID gameId;
  private UUID userId;
  private UUID playerId;
  private Integer round;
  private Integer pick;
  private Boolean isAutoPick;
  private Long selectionTime;

  private final List<String> validationErrors = new ArrayList<>();

  public DraftSelectionRequest() {
    this.isAutoPick = false;
  }

  public DraftSelectionRequest(UUID gameId, UUID userId, UUID playerId) {
    this();
    this.gameId = gameId;
    this.userId = userId;
    this.playerId = playerId;
    this.round = 1;
    this.pick = 1;
  }

  public DraftSelectionRequest(
      UUID gameId,
      UUID userId,
      UUID playerId,
      Integer round,
      Integer pick,
      Boolean isAutoPick,
      Long selectionTime) {
    this.gameId = gameId;
    this.userId = userId;
    this.playerId = playerId;
    this.round = round;
    this.pick = pick;
    this.isAutoPick = isAutoPick;
    this.selectionTime = selectionTime;
  }

  public UUID getGameId() {
    return gameId;
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public void setPlayerId(UUID playerId) {
    this.playerId = playerId;
  }

  public Integer getRound() {
    return round;
  }

  public void setRound(Integer round) {
    this.round = round;
  }

  public Integer getPick() {
    return pick;
  }

  public void setPick(Integer pick) {
    this.pick = pick;
  }

  public Boolean getIsAutoPick() {
    return isAutoPick;
  }

  public void setIsAutoPick(Boolean isAutoPick) {
    this.isAutoPick = isAutoPick;
  }

  public Long getSelectionTime() {
    return selectionTime;
  }

  public void setSelectionTime(Long selectionTime) {
    this.selectionTime = selectionTime;
  }

  public boolean isValid() {
    clearValidationErrors();

    validateGameId();
    validateUserId();
    validatePlayerId();
    validateRound();
    validatePick();
    validateSelectionTime();

    return validationErrors.isEmpty();
  }

  public boolean isAutoPickSelection() {
    return Boolean.TRUE.equals(isAutoPick);
  }

  public boolean hasSelectionTime() {
    return selectionTime != null;
  }

  public int getSelectionTimeInSeconds() {
    if (selectionTime == null) {
      return 0;
    }
    return (int) (selectionTime / 1000);
  }

  public int getSelectionTimeInMinutes() {
    if (selectionTime == null) {
      return 0;
    }
    return (int) (selectionTime / (1000 * 60));
  }

  public int getSelectionTimeSeconds() {
    return getSelectionTimeInSeconds();
  }

  public int getSelectionTimeMinutes() {
    return getSelectionTimeInMinutes();
  }

  public String getFullSelectionNumber() {
    return String.format("R%dP%d", round, pick);
  }

  public String getSelectionType() {
    return isAutoPickSelection() ? AUTO_PICK_SELECTION_TYPE : MANUAL_SELECTION_TYPE;
  }

  public String getSelectionSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append(getFullSelectionNumber());
    summary.append(" - ").append(getSelectionType());

    if (hasSelectionTime()) {
      summary.append(" - ").append(getSelectionTimeInSeconds()).append("s");
    }

    return summary.toString();
  }

  public void addValidationError(String error) {
    validationErrors.add(error);
  }

  public void clearValidationErrors() {
    validationErrors.clear();
  }

  public List<String> getValidationErrors() {
    return new ArrayList<>(validationErrors);
  }

  private void validateGameId() {
    if (gameId == null) {
      addValidationError("L'ID de la game est requis");
    }
  }

  private void validateUserId() {
    if (userId == null) {
      addValidationError("L'ID de l'utilisateur est requis");
    }
  }

  private void validatePlayerId() {
    if (playerId == null) {
      addValidationError("L'ID du joueur est requis");
    }
  }

  private void validateRound() {
    if (round == null) {
      addValidationError("Le numéro de round est requis");
      return;
    }

    if (round < MIN_ROUND) {
      addValidationError("Le numéro de round doit être positif");
    } else if (round > MAX_ROUND) {
      addValidationError("Le numéro de round ne peut pas dépasser " + MAX_ROUND);
    }
  }

  private void validatePick() {
    if (pick == null) {
      addValidationError("Le numéro de pick est requis");
      return;
    }

    if (pick < MIN_PICK) {
      addValidationError("Le numéro de pick doit être positif");
    } else if (pick > MAX_PICK) {
      addValidationError("Le numéro de pick ne peut pas dépasser " + MAX_PICK);
    }
  }

  private void validateSelectionTime() {
    if (selectionTime != null && selectionTime < MIN_SELECTION_TIME) {
      addValidationError("Le temps de sélection ne peut pas être négatif");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DraftSelectionRequest that = (DraftSelectionRequest) o;

    return Objects.equals(gameId, that.gameId)
        && Objects.equals(userId, that.userId)
        && Objects.equals(playerId, that.playerId)
        && Objects.equals(round, that.round)
        && Objects.equals(pick, that.pick)
        && Objects.equals(isAutoPick, that.isAutoPick)
        && Objects.equals(selectionTime, that.selectionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gameId, userId, playerId, round, pick, isAutoPick, selectionTime);
  }

  @Override
  public String toString() {
    return "DraftSelectionRequest{"
        + "gameId="
        + gameId
        + ", userId="
        + userId
        + ", playerId="
        + playerId
        + ", round="
        + round
        + ", pick="
        + pick
        + ", isAutoPick="
        + isAutoPick
        + ", selectionTime="
        + selectionTime
        + '}';
  }
}
