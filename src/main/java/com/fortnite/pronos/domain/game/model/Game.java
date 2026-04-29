package com.fortnite.pronos.domain.game.model;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure domain model representing a fantasy league game. Contains all business logic with zero
 * framework dependencies (no JPA, no Spring, no Lombok).
 */
@SuppressWarnings({"java:S107"})
public final class Game {

  private static final int MIN_PARTICIPANTS = 2;
  private static final int MAX_PARTICIPANTS_LIMIT = 50;
  private static final int DEFAULT_MAX_TRADES_PER_TEAM = 5;
  private static final int INVITATION_CODE_LENGTH = 8;
  private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final SecureRandom CODE_RANDOM = new SecureRandom();

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
  private DraftMode draftMode;
  private int teamSize;
  private int trancheSize;
  private boolean tranchesEnabled;
  private LocalDate competitionStart;
  private LocalDate competitionEnd;

  /** Business constructor for creating a new game. */
  public Game(
      String name,
      UUID creatorId,
      int maxParticipants,
      DraftMode draftMode,
      int teamSize,
      int trancheSize,
      boolean tranchesEnabled) {
    validateCreation(
        name, creatorId, maxParticipants, draftMode, teamSize, trancheSize, tranchesEnabled);
    this.id = UUID.randomUUID();
    this.name = name.trim();
    this.creatorId = creatorId;
    this.maxParticipants = maxParticipants;
    this.status = GameStatus.CREATING;
    this.createdAt = LocalDateTime.now();
    this.participants = new ArrayList<>();
    this.regionRules = new ArrayList<>();
    this.tradingEnabled = false;
    this.maxTradesPerTeam = DEFAULT_MAX_TRADES_PER_TEAM;
    this.currentSeason = java.time.Year.now().getValue();
    this.draftMode = draftMode;
    this.teamSize = teamSize;
    this.trancheSize = trancheSize;
    this.tranchesEnabled = tranchesEnabled;
  }

  /**
   * Reconstitution factory for persistence mapping. Backward-compatible overload without draft
   * configuration fields — defaults to SNAKE/5/10/true.
   */
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
    return restore(
        id,
        name,
        description,
        creatorId,
        maxParticipants,
        status,
        createdAt,
        finishedAt,
        deletedAt,
        invitationCode,
        invitationCodeExpiresAt,
        regionRules,
        participants,
        draftId,
        tradingEnabled,
        maxTradesPerTeam,
        tradeDeadline,
        currentSeason,
        DraftMode.SNAKE,
        5,
        10,
        true,
        null,
        null);
  }

  /** Reconstitution factory for persistence mapping with full draft configuration. */
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
      int currentSeason,
      DraftMode draftMode,
      int teamSize,
      int trancheSize,
      boolean tranchesEnabled,
      LocalDate competitionStart,
      LocalDate competitionEnd) {
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
    game.draftMode = draftMode != null ? draftMode : DraftMode.SNAKE;
    game.teamSize = teamSize;
    game.trancheSize = trancheSize;
    game.tranchesEnabled = tranchesEnabled;
    game.competitionStart = competitionStart;
    game.competitionEnd = competitionEnd;
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
    if (isInvalidParticipant(participant)) {
      return false;
    }
    this.participants.add(participant);
    return true;
  }

  public void removeParticipant(GameParticipant participant) {
    int participantIndex = participants.indexOf(participant);
    if (participantIndex >= 0) {
      participants.remove(participantIndex);
    }
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

  public void clearInvitationCode() {
    this.invitationCode = null;
    this.invitationCodeExpiresAt = null;
  }

  // ===============================
  // REGION RULES
  // ===============================

  public void addRegionRule(GameRegionRule rule) {
    Objects.requireNonNull(rule, "Region rule cannot be null");
    regionRules.add(rule);
  }

  public void removeRegionRule(GameRegionRule rule) {
    int ruleIndex = regionRules.indexOf(rule);
    if (ruleIndex >= 0) {
      regionRules.remove(ruleIndex);
    }
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

  public void setCompetitionStart(LocalDate competitionStart) {
    this.competitionStart = competitionStart;
  }

  public void setCompetitionEnd(LocalDate competitionEnd) {
    this.competitionEnd = competitionEnd;
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

  public DraftMode getDraftMode() {
    return draftMode;
  }

  public int getTeamSize() {
    return teamSize;
  }

  public int getTrancheSize() {
    return trancheSize;
  }

  public boolean isTranchesEnabled() {
    return tranchesEnabled;
  }

  public LocalDate getCompetitionStart() {
    return competitionStart;
  }

  public LocalDate getCompetitionEnd() {
    return competitionEnd;
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private void validateCreation(
      String name,
      UUID creatorId,
      int maxParticipants,
      DraftMode draftMode,
      int teamSize,
      int trancheSize,
      boolean tranchesEnabled) {
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
    if (draftMode == null) {
      throw new IllegalArgumentException("Draft mode cannot be null");
    }
    if (teamSize < 1) {
      throw new IllegalArgumentException("Team size must be >= 1");
    }
    if (tranchesEnabled && trancheSize < 1) {
      throw new IllegalArgumentException("Tranche size must be >= 1 when tranches are enabled");
    }
  }

  private boolean isInvalidParticipant(GameParticipant participant) {
    boolean creatorParticipant = participant.getUserId().equals(this.creatorId);
    boolean duplicateParticipant =
        participants.stream().anyMatch(p -> p.getUserId().equals(participant.getUserId()));
    boolean gameCannotAcceptParticipants = isFull() || !canAddParticipants();
    return creatorParticipant || duplicateParticipant || gameCannotAcceptParticipants;
  }

  private String generateRandomCode() {
    StringBuilder code = new StringBuilder(INVITATION_CODE_LENGTH);
    for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
      code.append(CODE_CHARS.charAt(CODE_RANDOM.nextInt(CODE_CHARS.length())));
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
