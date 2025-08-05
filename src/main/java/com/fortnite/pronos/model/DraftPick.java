package com.fortnite.pronos.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "draft_picks")
public class DraftPick {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "draft_id", nullable = false)
  private Draft draft;

  @ManyToOne
  @JoinColumn(name = "participant_id", nullable = false)
  private GameParticipant participant;

  @ManyToOne
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Column(name = "round_number", nullable = false)
  private Integer round;

  @Column(name = "pick_number", nullable = false)
  private Integer pickNumber;

  @Column(name = "selection_time", nullable = false)
  private LocalDateTime selectionTime;

  @Column(name = "time_taken_seconds")
  private Integer timeTakenSeconds;

  @Column(name = "auto_pick", nullable = false)
  private Boolean autoPick = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  // Constructeurs
  public DraftPick() {
    this.createdAt = LocalDateTime.now();
    this.selectionTime = LocalDateTime.now();
    this.autoPick = false;
  }

  public DraftPick(
      Draft draft, GameParticipant participant, Player player, Integer round, Integer pickNumber) {
    this();
    this.draft = draft;
    this.participant = participant;
    this.player = player;
    this.round = round;
    this.pickNumber = pickNumber;
  }

  // Méthodes utilitaires
  public boolean isAutoPick() {
    return this.autoPick;
  }

  public void markAsAutoPick() {
    this.autoPick = true;
  }

  public void setTimeTaken(Integer seconds) {
    this.timeTakenSeconds = seconds;
  }

  // Getters et Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Draft getDraft() {
    return draft;
  }

  public void setDraft(Draft draft) {
    this.draft = draft;
  }

  public GameParticipant getParticipant() {
    return participant;
  }

  public void setParticipant(GameParticipant participant) {
    this.participant = participant;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;
  }

  public Integer getRound() {
    return round;
  }

  public void setRound(Integer round) {
    this.round = round;
  }

  public Integer getPickNumber() {
    return pickNumber;
  }

  public void setPickNumber(Integer pickNumber) {
    this.pickNumber = pickNumber;
  }

  public LocalDateTime getSelectionTime() {
    return selectionTime;
  }

  public void setSelectionTime(LocalDateTime selectionTime) {
    this.selectionTime = selectionTime;
  }

  public Integer getTimeTakenSeconds() {
    return timeTakenSeconds;
  }

  public void setTimeTakenSeconds(Integer timeTakenSeconds) {
    this.timeTakenSeconds = timeTakenSeconds;
  }

  public Boolean getAutoPick() {
    return autoPick;
  }

  public void setAutoPick(Boolean autoPick) {
    this.autoPick = autoPick;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "DraftPick{"
        + "id="
        + id
        + ", draft="
        + (draft != null ? draft.getId() : "null")
        + ", participant="
        + (participant != null ? participant.getUsername() : "null")
        + ", player="
        + (player != null ? player.getNickname() : "null")
        + ", round="
        + round
        + ", pickNumber="
        + pickNumber
        + ", autoPick="
        + autoPick
        + '}';
  }
}
