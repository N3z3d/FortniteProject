package com.fortnite.pronos.domain.game.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Pure domain model representing a fantasy league game. Contains all business logic with zero
 * framework dependencies (no JPA, no Spring, no Lombok).
 */
public final class Game {

  private static final int MIN_PARTICIPANTS = 2;
  private static final int MAX_PARTICIPANTS_LIMIT = 50;
  private static final int INVITATION_CODE_LENGTH = 8;
  private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  private UUID id;
  private String name;
  private String description;
  private UUID creatorId;
  private int maxParticipants;
  private GameStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime finishedAt;
  private LocalDateTime deletedAt;
  private String invitationCode;
  private LocalDateTime invitationCodeExpiresAt;
  private final List<GameRegionRule> regionRules;
  private final List<GameParticipant> participants;
  private UUID draftId;
  private boolean tradingEnabled;
  private int maxTradesPerTeam;
  private LocalDateTime tradeDeadline;
  private int currentSeason;

  /** Business constructor for creating a new game. */
  public Game(String name, UUID creatorId, int maxParticipants) {
    validateCreation(name, creatorId, maxParticipants);
    this.id = UUID.randomUUID();
    this.name = name.trim();
    this.creatorId = creatorId;
    this.maxParticipants = maxParticipants;
    this.status = GameStatus.CREATING;
    this.createdAt = LocalDateTime.now();
    this.participants = new ArrayList<>();
    this.regionRules = new ArrayList<>();
    this.tradingEnabled = false;
    this.maxTradesPerTeam = 5;
    this.currentSeason = java.time.Year.now().getValue();
  }

  /** Reconstitution factory for persistence mapping. */
  public static Game restore(
      UUID id,
      String name,
      String description,
      UUID creatorId,
      int maxParticipants,
      GameStatus status,
      LocalDateTime createdAt,
      LocalDateTime finishedAt,
      LocalDateTime deletedAt,
      String invitationCode,
      LocalDateTime invitationCodeExpiresAt,
      List<GameRegionRule> regionRules,
      List<GameParticipant> participants,
      UUID draftId,
      boolean tradingEnabled,
      int maxTradesPerTeam,
      LocalDateTime tradeDeadline,
      int currentSeason) {
    Game game = new Game();
    game.id = id;
    game.name = name;
    game.description = description;
    game.creatorId = creatorId;
    game.maxParticipants = maxParticipants;
    game.status = status;
    game.createdAt = createdAt;
    game.finishedAt = finishedAt;
    game.deletedAt = deletedAt;
    game.invitationCode = invitationCode;
    game.invitationCodeExpiresAt = invitationCodeExpiresAt;
    if (regionRules != null) {
      game.regionRules.addAll(regionRules);
    }
    if (participants != null) {
      game.participants.addAll(participants);
    }
    game.draftId = draftId;
    game.tradingEnabled = tradingEnabled;
    game.maxTradesPerTeam = maxTradesPerTeam;
    game.tradeDeadline = tradeDeadline;
    game.currentSeason = currentSeason;
    return game;
  }

  /** Private no-arg constructor for restore(). */
  private Game() {
    this.participants = new ArrayList<>();
    this.regionRules = new ArrayList<>();
  }

  // ===============================
  // BUSINESS RULES
  // ===============================

  /** Adds a participant with full business rules validation. */
  public boolean addParticipant(GameParticipant participant) {
    Objects.requireNonNull(participant, "Participant cannot be null");

    if (participant.getUserId().equals(this.creatorId)) {
      return false;
    }
    if (participants.stream().anyMatch(p -> p.getUserId().equals(participant.getUserId()))) {
      return false;
    }
    if (isFull()) {
      return false;
    }
    if (!canAddParticipants()) {
      return false;
    }

    this.participants.add(participant);
    return true;
  }

  public void removeParticipant(GameParticipant participant) {
    participants.remove(participant);
  }

  public boolean isParticipant(UUID userId) {
    if (userId.equals(this.creatorId)) {
      return true;
    }
    return participants.stream().anyMatch(p -> p.getUserId().equals(userId));
  }

  public boolean isFull() {
    return getTotalParticipantCount() >= maxParticipants;
  }

  public int getTotalParticipantCount() {
    boolean creatorAlreadyCounted =
        participants.stream()
            .anyMatch(p -> p.getUserId() != null && p.getUserId().equals(creatorId));
    int count = participants.size();
    if (!creatorAlreadyCounted && creatorId != null) {
      count += 1;
    }
    return count;
  }

  public int getAvailableSpots() {
    return maxParticipants - getTotalParticipantCount();
  }

  public boolean canAcceptParticipants() {
    return participants.size() < maxParticipants;
  }

  public boolean canAddParticipants() {
    return status == GameStatus.CREATING && !isFull();
  }

  /** Starts the draft phase if business rules are met. */
  public boolean startDraft() {
    if (getTotalParticipantCount() < MIN_PARTICIPANTS) {
      return false;
    }
    if (status != GameStatus.CREATING) {
      return false;
    }
    this.status = GameStatus.DRAFTING;
    return true;
  }

  public boolean completeDraft() {
    if (status != GameStatus.DRAFTING) {
      return false;
    }
    this.status = GameStatus.ACTIVE;
    return true;
  }

  public boolean finishGame() {
    if (status != GameStatus.ACTIVE) {
      return false;
    }
    this.status = GameStatus.FINISHED;
    this.finishedAt = LocalDateTime.now();
    return true;
  }

  public boolean isDrafting() {
    return status == GameStatus.DRAFTING;
  }

  public boolean isActive() {
    return status == GameStatus.ACTIVE;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  public int getParticipantCount() {
    return participants.size();
  }

  // ===============================
  // INVITATION CODE
  // ===============================

  public String generateInvitationCode() {
    if (this.invitationCode == null) {
      this.invitationCode = generateRandomCode();
    }
    return this.invitationCode;
  }

  public boolean isInvitationCodeValid() {
    if (invitationCode == null || invitationCode.isEmpty()) {
      return false;
    }
    if (invitationCodeExpiresAt == null) {
      return true;
    }
    return LocalDateTime.now().isBefore(invitationCodeExpiresAt);
  }

  public boolean isInvitationCodeExpired() {
    if (invitationCodeExpiresAt == null) {
      return false;
    }
    return LocalDateTime.now().isAfter(invitationCodeExpiresAt);
  }

  // ===============================
  // REGION RULES
  // ===============================

  public void addRegionRule(GameRegionRule rule) {
    Objects.requireNonNull(rule, "Region rule cannot be null");
    regionRules.add(rule);
  }

  public void removeRegionRule(GameRegionRule rule) {
    regionRules.remove(rule);
  }

  // ===============================
  // SETTERS FOR MUTABLE STATE
  // ===============================

  public void rename(String newName) {
    if (newName == null || newName.trim().isEmpty()) {
      throw new IllegalArgumentException("Game name cannot be null or empty");
    }
    this.name = newName.trim();
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setTradingEnabled(boolean tradingEnabled) {
    this.tradingEnabled = tradingEnabled;
  }

  public void setMaxTradesPerTeam(int maxTradesPerTeam) {
    this.maxTradesPerTeam = maxTradesPerTeam;
  }

  public void setTradeDeadline(LocalDateTime tradeDeadline) {
    this.tradeDeadline = tradeDeadline;
  }

  public void setDraftId(UUID draftId) {
    this.draftId = draftId;
  }

  public void setInvitationCode(String invitationCode) {
    this.invitationCode = invitationCode;
  }

  public void setInvitationCodeExpiresAt(LocalDateTime expiresAt) {
    this.invitationCodeExpiresAt = expiresAt;
  }

  // ===============================
  // GETTERS
  // ===============================

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public UUID getCreatorId() {
    return creatorId;
  }

  public int getMaxParticipants() {
    return maxParticipants;
  }

  public GameStatus getStatus() {
    return status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getFinishedAt() {
    return finishedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public String getInvitationCode() {
    return invitationCode;
  }

  public LocalDateTime getInvitationCodeExpiresAt() {
    return invitationCodeExpiresAt;
  }

  public List<GameRegionRule> getRegionRules() {
    return Collections.unmodifiableList(regionRules);
  }

  public List<GameParticipant> getParticipants() {
    return Collections.unmodifiableList(participants);
  }

  public UUID getDraftId() {
    return draftId;
  }

  public boolean isTradingEnabled() {
    return tradingEnabled;
  }

  public int getMaxTradesPerTeam() {
    return maxTradesPerTeam;
  }

  public LocalDateTime getTradeDeadline() {
    return tradeDeadline;
  }

  public int getCurrentSeason() {
    return currentSeason;
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private void validateCreation(String name, UUID creatorId, int maxParticipants) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Game name cannot be null or empty");
    }
    if (creatorId == null) {
      throw new IllegalArgumentException("Creator ID cannot be null");
    }
    if (maxParticipants < MIN_PARTICIPANTS || maxParticipants > MAX_PARTICIPANTS_LIMIT) {
      throw new IllegalArgumentException(
          "Max participants must be between "
              + MIN_PARTICIPANTS
              + " and "
              + MAX_PARTICIPANTS_LIMIT);
    }
  }

  private String generateRandomCode() {
    Random random = new Random();
    StringBuilder code = new StringBuilder(INVITATION_CODE_LENGTH);
    for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
      code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
    }
    return code.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Game that)) return false;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
