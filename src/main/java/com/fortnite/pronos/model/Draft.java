package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "drafts")
public class Draft {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @OneToOne
  @JoinColumn(name = "game_id", nullable = false)
  private Game game;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @Column(name = "current_round", nullable = false)
  private Integer currentRound;

  @Column(name = "current_pick", nullable = false)
  private Integer currentPick;

  @Column(name = "total_rounds", nullable = false)
  private Integer totalRounds;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  public enum Status {
    PENDING, // Draft créé mais pas encore démarré
    ACTIVE, // Draft en cours
    IN_PROGRESS, // Draft en cours de progression
    PAUSED, // Draft en pause
    FINISHED, // Draft terminé
    CANCELLED // Draft annulé
  }

  // Constructeurs
  public Draft() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.status = Status.PENDING;
    this.currentRound = 1;
    this.currentPick = 1;
  }

  public Draft(Game game) {
    this();
    this.game = game;
    this.totalRounds = calculateTotalRounds(game);
  }

  // Méthodes utilitaires
  private Integer calculateTotalRounds(Game game) {
    // Calculer le nombre de rounds basé sur le nombre de participants
    // et les règles de sélection
    int participants = game.getParticipantCount();
    int playersPerTeam = 3; // Par défaut, 3 joueurs par équipe
    return (int) Math.ceil((double) (participants * playersPerTeam) / participants);
  }

  public boolean isActive() {
    return Status.ACTIVE.equals(this.status);
  }

  public boolean isFinished() {
    return Status.FINISHED.equals(this.status);
  }

  public boolean isPending() {
    return Status.PENDING.equals(this.status);
  }

  public void start() {
    this.status = Status.ACTIVE;
    this.startedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void finish() {
    this.status = Status.FINISHED;
    this.finishedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void pause() {
    this.status = Status.PAUSED;
    this.updatedAt = LocalDateTime.now();
  }

  public void resume() {
    this.status = Status.ACTIVE;
    this.updatedAt = LocalDateTime.now();
  }

  public void cancel() {
    this.status = Status.CANCELLED;
    this.updatedAt = LocalDateTime.now();
  }

  public void nextPick() {
    this.currentPick++;
    if (this.currentPick > this.game.getParticipantCount()) {
      this.currentRound++;
      this.currentPick = 1;
    }
    this.updatedAt = LocalDateTime.now();
  }

  public boolean isDraftComplete() {
    return this.currentRound > this.totalRounds;
  }

  // Getters et Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Game getGame() {
    return game;
  }

  public void setGame(Game game) {
    this.game = game;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Integer getCurrentRound() {
    return currentRound;
  }

  public void setCurrentRound(Integer currentRound) {
    this.currentRound = currentRound;
  }

  public Integer getCurrentPick() {
    return currentPick;
  }

  public void setCurrentPick(Integer currentPick) {
    this.currentPick = currentPick;
  }

  public Integer getTotalRounds() {
    return totalRounds;
  }

  public void setTotalRounds(Integer totalRounds) {
    this.totalRounds = totalRounds;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public LocalDateTime getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(LocalDateTime finishedAt) {
    this.finishedAt = finishedAt;
  }

  @Override
  public String toString() {
    return "Draft{"
        + "id="
        + id
        + ", game="
        + (game != null ? game.getName() : "null")
        + ", status="
        + status
        + ", currentRound="
        + currentRound
        + ", currentPick="
        + currentPick
        + ", totalRounds="
        + totalRounds
        + '}';
  }
}
