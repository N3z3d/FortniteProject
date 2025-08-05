package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entité représentant une game de fantasy league PHASE 1B: Enhanced with EntityGraph for N+1 query
 * prevention
 */
@Entity
@Table(name = "games")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraphs({
  @NamedEntityGraph(
      name = "Game.withBasicDetails",
      attributeNodes = {@NamedAttributeNode("creator"), @NamedAttributeNode("participants")}),
  @NamedEntityGraph(
      name = "Game.withFullDetails",
      attributeNodes = {
        @NamedAttributeNode("creator"),
        @NamedAttributeNode(value = "participants", subgraph = "participants-with-user"),
        @NamedAttributeNode("regionRules"),
        @NamedAttributeNode("draft")
      },
      subgraphs = {
        @NamedSubgraph(
            name = "participants-with-user",
            attributeNodes = @NamedAttributeNode("user"))
      })
})
public class Game {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 500)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id", nullable = false)
  private User creator;

  @Column(name = "max_participants", nullable = false)
  private Integer maxParticipants;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GameStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "invitation_code", length = 10, unique = true)
  private String invitationCode;

  @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<GameRegionRule> regionRules = new ArrayList<>();

  @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<GameParticipant> participants = new ArrayList<>();

  @OneToOne(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
  private Draft draft;

  /** Ajoute une règle régionale à la game */
  public void addRegionRule(GameRegionRule rule) {
    regionRules.add(rule);
    rule.setGame(this);
  }

  /** Supprime une règle régionale de la game */
  public void removeRegionRule(GameRegionRule rule) {
    regionRules.remove(rule);
    rule.setGame(null);
  }

  /** Ajoute un participant à la game */
  public void addParticipant(GameParticipant participant) {
    participants.add(participant);
    participant.setGame(this);
  }

  /** Supprime un participant de la game */
  public void removeParticipant(GameParticipant participant) {
    participants.remove(participant);
    participant.setGame(null);
  }

  /** Vérifie si la game peut accepter de nouveaux participants */
  public boolean canAcceptParticipants() {
    return participants.size() < maxParticipants;
  }

  /** Vérifie si la game est en cours de draft */
  public boolean isDrafting() {
    return status == GameStatus.DRAFTING;
  }

  /** Vérifie si la game est active */
  public boolean isActive() {
    return status == GameStatus.ACTIVE;
  }

  /** Retourne le nombre de participants dans la game */
  public int getParticipantCount() {
    return participants.size();
  }

  // ===============================
  // RICH DOMAIN MODEL METHODS
  // ===============================

  /**
   * Domain constructor for creating a new game with business rules validation
   *
   * @param name Game name (cannot be null or empty)
   * @param creator Game creator (cannot be null)
   * @param maxParticipants Maximum participants (must be between 2 and 50)
   */
  public Game(String name, User creator, int maxParticipants) {
    validateGameCreation(name, creator, maxParticipants);
    this.name = name.trim();
    this.creator = creator;
    this.maxParticipants = maxParticipants;
    this.status = GameStatus.CREATING;
    this.createdAt = LocalDateTime.now();
    this.participants = new ArrayList<>();
    this.regionRules = new ArrayList<>();
  }

  /** Validates game creation business rules */
  private void validateGameCreation(String name, User creator, int maxParticipants) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Game name cannot be null or empty");
    }
    if (creator == null) {
      throw new IllegalArgumentException("Game creator cannot be null");
    }
    if (maxParticipants < 2 || maxParticipants > 50) {
      throw new IllegalArgumentException("Max participants must be between 2 and 50");
    }
  }

  /**
   * Adds a participant to the game with business rules validation
   *
   * @param user User to add as participant
   * @return true if successfully added, false if rejected
   */
  public boolean addParticipant(User user) {
    // Business rule: Creator is automatically a participant
    if (user.equals(this.creator)) {
      return false;
    }

    // Business rule: No duplicate participants
    if (participants.stream().anyMatch(p -> p.getUser().equals(user))) {
      return false;
    }

    // Business rule: Cannot join if game is full
    if (isFull()) {
      return false;
    }

    // Business rule: Cannot join started games
    if (!canAddParticipants()) {
      return false;
    }

    // Create GameParticipant and add
    GameParticipant participant = new GameParticipant();
    participant.setUser(user);
    participant.setGame(this);
    participant.setCreator(false);
    this.participants.add(participant);

    return true;
  }

  /**
   * Checks if a user is a participant in this game
   *
   * @param user User to check
   * @return true if user participates in this game
   */
  public boolean isParticipant(User user) {
    // Creator is always a participant
    if (user.equals(this.creator)) {
      return true;
    }
    return participants.stream().anyMatch(p -> p.getUser().equals(user));
  }

  /**
   * Checks if the game is full (including creator)
   *
   * @return true if game is at maximum capacity
   */
  public boolean isFull() {
    return getTotalParticipantCount() >= maxParticipants;
  }

  /**
   * Gets total participant count including creator
   *
   * @return total number of participants including creator
   */
  public int getTotalParticipantCount() {
    return participants.size() + 1; // +1 for creator
  }

  /**
   * Gets available spots in the game
   *
   * @return number of available spots
   */
  public int getAvailableSpots() {
    return maxParticipants - getTotalParticipantCount();
  }

  /**
   * Checks if game can accept new participants
   *
   * @return true if game accepts new participants
   */
  public boolean canAddParticipants() {
    return status == GameStatus.CREATING && !isFull();
  }

  /**
   * Starts the draft phase if business rules are met
   *
   * @return true if draft started successfully
   */
  public boolean startDraft() {
    // Business rule: Need at least 2 total participants to start
    if (getTotalParticipantCount() < 2) {
      return false;
    }

    // Business rule: Can only start from CREATING status
    if (status != GameStatus.CREATING) {
      return false;
    }

    this.status = GameStatus.DRAFTING;
    return true;
  }

  /**
   * Completes draft and moves to active phase
   *
   * @return true if draft completed successfully
   */
  public boolean completeDraft() {
    if (status != GameStatus.DRAFTING) {
      return false;
    }

    this.status = GameStatus.ACTIVE;
    return true;
  }

  /**
   * Finishes the game and sets completion time
   *
   * @return true if game finished successfully
   */
  public boolean finishGame() {
    if (status != GameStatus.ACTIVE) {
      return false;
    }

    this.status = GameStatus.FINISHED;
    this.finishedAt = LocalDateTime.now();
    return true;
  }

  /**
   * Generates a unique invitation code for the game
   *
   * @return 8-character alphanumeric invitation code
   */
  public String generateInvitationCode() {
    if (this.invitationCode == null) {
      this.invitationCode = generateRandomCode();
    }
    return this.invitationCode;
  }

  /** Generates random 8-character alphanumeric code */
  private String generateRandomCode() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    Random random = new Random();
    StringBuilder code = new StringBuilder(8);
    for (int i = 0; i < 8; i++) {
      code.append(chars.charAt(random.nextInt(chars.length())));
    }
    return code.toString();
  }

  /**
   * Validates state transitions (prevents going backwards)
   *
   * @throws IllegalStateException if invalid transition attempted
   */
  public void resetToCreating() {
    if (status != GameStatus.CREATING) {
      throw new IllegalStateException(
          String.format("Cannot transition from %s to CREATING", status));
    }
  }

  /**
   * Gets the finished timestamp
   *
   * @return LocalDateTime when game finished, null if not finished
   */
  public LocalDateTime getFinishedAt() {
    return finishedAt;
  }
}
